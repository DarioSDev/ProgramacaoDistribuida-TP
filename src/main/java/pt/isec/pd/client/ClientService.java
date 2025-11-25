package pt.isec.pd.client;

import pt.isec.pd.common.User;
import pt.isec.pd.common.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ClientService implements ClientAPI {
    private static final int MAX_SAME_SERVER_RETRY = 2;
    private static final long SAME_SERVER_RETRY_DELAY_MS = 20000;
    private static final int CONNECTION_TIMEOUT_MS = 5000;

    private final String directoryHost;
    private final int directoryPort;

    private String currentServerIp = null;
    private int currentServerPort = -1;

    private int sameServerFailureCount = 0;

    private Socket activeSocket = null;
    private PrintWriter out = null;
    private BufferedReader in = null;

    private volatile boolean running = true;

    // 🔒 Sincronização
    private final Object lock = new Object();
    private String syncResponse = null;
    private boolean expectingResponse = false;


    public ClientService(String directoryHost, int directoryPort) {
        this.directoryHost = directoryHost;
        this.directoryPort = directoryPort;
    }

    public void start() {
        System.out.println("[Client] Iniciando cliente REAL...");

        while (running) {
            if (activeSocket == null || activeSocket.isClosed()) {
                if (!tryConnectAndAuthenticate()) {
                    System.err.println("[Client] Falha crítica. A terminar cliente.");
                    break;
                }
            }
            listenAndMaintainSession();
        }

        closeResources();
        System.out.println("[Client] Cliente REAL encerrado.");
    }

    private boolean tryConnectAndAuthenticate() {

        while (running) {
            String[] serverInfo = requestActiveServer();

            if (serverInfo == null) {
                System.err.println("[Client] Nenhum servidor disponível. Tentando de novo...");
                sameServerFailureCount = 0;
                sleep(5000);
                continue;
            }

            String newIp = serverInfo[0];
            int newPort = Integer.parseInt(serverInfo[1]);

            boolean sameServer =
                    newIp.equals(currentServerIp) && newPort == currentServerPort;

            if (sameServer && sameServerFailureCount >= 1) {
                sameServerFailureCount++;

                if (sameServerFailureCount > MAX_SAME_SERVER_RETRY) {
                    System.err.println("[Client] Esgotadas tentativas para o mesmo servidor.");
                    return false;
                }

                System.out.printf("[Client] Mesmo servidor. Esperando %ds...\n",
                        SAME_SERVER_RETRY_DELAY_MS / 1000);
                sleep(SAME_SERVER_RETRY_DELAY_MS);
                continue;
            }

            if (attemptTcpConnection(newIp, newPort)) {
                currentServerIp = newIp;
                currentServerPort = newPort;
                sameServerFailureCount = 0;
                return true;
            }

            System.err.println("[Client] Falhou a ligação TCP. Marcando servidor como falhado.");
            sameServerFailureCount = 1;
            sleep(2000);
        }
        return false;
    }

    private boolean attemptTcpConnection(String ip, int port) {

        closeResources();

        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), CONNECTION_TIMEOUT_MS);

            out  = new PrintWriter(socket.getOutputStream(), true);
            in   = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String welcome = in.readLine();
            if (welcome == null) return false;

            if (welcome.startsWith("ERRO:")) {
                socket.close();
                return false;
            }

            System.out.println("[Client] Servidor: " + welcome);

            // 3. Autenticação (ou primeira mensagem)
            out.println(Command.CONNECTION);

            System.out.println("[Client] Enviado: " + Command.CONNECTION);

            // 4. Receber confirmação
            String confirmation = in.readLine();
            if (confirmation == null) return false;

            activeSocket = socket;
            return true;

        } catch (IOException e) {
            System.err.println("[Client] Erro ao ligar TCP: " + e.getMessage());
            return false;
        }
    }

    private void listenAndMaintainSession() {
        boolean hardFailure = false;

        try {
            if (activeSocket != null) activeSocket.setSoTimeout(0);

            while (running && activeSocket != null && !activeSocket.isClosed()) {
                String serverMsg = in.readLine();

                if (serverMsg == null) {
                    System.out.println("[Client] Servidor fechou ligação.");
                    break;
                }

                // 🔒 Lógica de Entrega da Mensagem
                synchronized (lock) {
                    if (expectingResponse) {
                        // Se o método de Login/Registo está à espera, entregamos a ele
                        syncResponse = serverMsg;
                        expectingResponse = false;
                        lock.notifyAll(); // Acorda a thread que está no wait()
                    } else {
                        // Caso contrário, é uma mensagem assíncrona (notificação)
                        System.out.println("[Server Async] " + serverMsg);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("[Client] Conexão perdida: " + e.getMessage());
            hardFailure = true;

            // Desbloquear quem estiver à espera em caso de erro
            synchronized (lock) {
                syncResponse = null;
                expectingResponse = false;
                lock.notifyAll();
            }

        } finally {
            closeResources();
            sameServerFailureCount = hardFailure ? 1 : 0;
        }
    }

    private String[] requestActiveServer() {
        try (DatagramSocket socket = new DatagramSocket()) {

            byte[] buf = "REQUEST_SERVER".getBytes();
            socket.send(new DatagramPacket(buf, buf.length,
                    InetAddress.getByName(directoryHost), directoryPort));

            socket.setSoTimeout(5000);

            byte[] recv = new byte[256];
            DatagramPacket packet = new DatagramPacket(recv, recv.length);

            socket.receive(packet);

            String response = new String(packet.getData(), 0, packet.getLength()).trim();

            if (response.equals("NO_SERVER_AVAILABLE"))
                return null;

            String[] parts = response.split(" ");
            if (parts.length == 2) return parts;

        } catch (Exception e) {
            System.err.println("[Client] Erro ao contactar Directory: " + e.getMessage());
        }

        return null;
    }

    private void closeResources() {
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (activeSocket != null) activeSocket.close(); } catch (Exception ignored) {}

        in = null;
        out = null;
        activeSocket = null;
    }

    private void sleep(long ms) {
        try { TimeUnit.MILLISECONDS.sleep(ms); } catch (InterruptedException ignored) {}
    }


    @Override
    public String sendLogin(String email, String pwd) throws IOException {
        if (out == null) throw new IOException("Ligação TCP não está ativa.");

        synchronized (lock) {
            try {
                expectingResponse = true;
                syncResponse = null;

                StringBuilder sb = new StringBuilder();
                sb.append(Command.LOGIN).append(";").append(email).append(";").append(pwd);
                out.println(sb.toString());

                // Esperar pela resposta (timeout de 5s para segurança)
                lock.wait(5000);

                if (syncResponse == null) {
                    expectingResponse = false; // Cancelar espera se estourou o tempo
                    return "TIMEOUT";
                }

                System.out.println("RESPONSE LOGIN: " + syncResponse);
                return syncResponse;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "ERROR";
            }
        }
    }

    @Override
    public boolean register(String role, String name, String id, String email, String password) throws IOException {
        if (out == null) throw new IOException("Ligação TCP não está ativa.");

        synchronized (lock) {
            try {
                expectingResponse = true;
                syncResponse = null;

                StringBuilder sb = new StringBuilder();
                // Ajusta o comando conforme o Enum (REGISTER_STUDENT ou TEACHER)
                if ("student".equalsIgnoreCase(role)) {
                    sb.append("REGISTER_STUDENT");
                } else {
                    sb.append("REGISTER_TEACHER");
                }

                sb.append(";").append(name)
                        .append(";").append(email)
                        .append(";").append(password)
                        .append(";").append(id); // ID é o último campo

                out.println(sb.toString());

                lock.wait(5000); // Espera 5s no máximo

                if (syncResponse == null) {
                    expectingResponse = false;
                    return false;
                }

                System.out.println("RESPONSE REGISTER: " + syncResponse);
                // Verifica se a resposta contém sucesso (ajusta conforme o teu servidor responde)
                return syncResponse.toUpperCase().contains("SUCCESS") || syncResponse.equalsIgnoreCase("true");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    // ===============================================================
    // A PARTIR DAQUI: MÉTODOS A IMPLEMENTAR MAIS TARDE
    // (por agora lançam UnsupportedOperationException para o TP REAL)
    // ===============================================================

    @Override
    public QuestionData getQuestionByCode(String code) {
        throw new UnsupportedOperationException("getQuestionByCode ainda não implementado no ClientServiceReal.");
    }

    @Override
    public boolean submitAnswer(User user, String code, int index) {
        throw new UnsupportedOperationException("submitAnswer ainda não implementado no ClientServiceReal.");
    }

    @Override
    public String validateQuestionCode(String code) {
        throw new UnsupportedOperationException("validateQuestionCode ainda não implementado no ClientServiceReal.");
    }

    @Override
    public AnswerResultData getAnswerResult(User user, String code) {
        throw new UnsupportedOperationException("getAnswerResult ainda não implementado no ClientServiceReal.");
    }

    @Override
    public List<HistoryItem> getStudentHistory(User user, LocalDate start, LocalDate end, String filter) {
        throw new UnsupportedOperationException("getStudentHistory ainda não implementado no ClientServiceReal.");
    }

    @Override
    public List<TeacherQuestionItem> getTeacherQuestions(User user, String filter) {
        throw new UnsupportedOperationException("getTeacherQuestions ainda não implementado no ClientServiceReal.");
    }

    @Override
    public boolean createQuestion(User user, String text, List<String> options,
                                  int correctIndex, LocalDate sd, LocalTime st,
                                  LocalDate ed, LocalTime et) {
        throw new UnsupportedOperationException("createQuestion ainda não implementado no ClientServiceReal.");
    }

    @Override
    public TeacherResultsData getQuestionResults(User user, String questionCode) {
        throw new UnsupportedOperationException("getQuestionResults ainda não implementado no ClientServiceReal.");
    }
}
