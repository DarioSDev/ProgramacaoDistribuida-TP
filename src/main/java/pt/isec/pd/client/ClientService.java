package pt.isec.pd.client;

import pt.isec.pd.common.*;

import java.io.*;
import java.net.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ClientService implements ClientAPI {
    private static final long RETRY_DELAY_MS = 20000; // 20 segundos
    private static final int CONNECTION_TIMEOUT_MS = 5000;

    private final String directoryHost;
    private final int directoryPort;

    private String currentServerIp = null;
    private int currentServerPort = -1;

    private Socket activeSocket = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    private volatile boolean running = true;

    // controla se já houve alguma ligação com sucesso (para saber se estamos em reconexão)
    private boolean hasEverConnected = false;
    private boolean lastConnectionFailed = false;

    private final Object lock = new Object();
    private Object syncResponse = null;
    private boolean expectingResponse = false;

    public ClientService(String directoryHost, int directoryPort) {
        this.directoryHost = directoryHost;
        this.directoryPort = directoryPort;
    }

    public void start() {
        System.out.println("[Client] Iniciando cliente REAL (Object Streams)...");

        while (running) {
            if (activeSocket == null || activeSocket.isClosed()) {

                // Se já estivemos ligados e a última ligação caiu, espera 20s antes de tentar reconectar
                if (hasEverConnected && lastConnectionFailed) {
                    System.out.println("[Client] Conexão ao servidor perdida. Aguardando 20s antes de tentar reconectar...");
                    sleep(RETRY_DELAY_MS);
                    lastConnectionFailed = false; // para não ficar sempre a dormir
                }

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

    /**
     * Tenta ligar ao servidor principal seguindo o flow:
     * - Pergunta à diretoria qual é o primary
     * - Tenta ligação TCP
     * - Se falhar / não houver servidor → espera 20s e repete
     * - Máximo 3 tentativas (1 imediata + 2 com espera de 20s). Se falhar, devolve false.
     */
    private boolean tryConnectAndAuthenticate() {
        for (int attempt = 1; attempt <= 3 && running; attempt++) {

            String[] info = requestActiveServer();

            if (info == null) {
                System.err.printf("[Client] Diretoria não devolveu servidor (tentativa %d/3).%n", attempt);
            } else {
                String ip = info[0];
                int port = Integer.parseInt(info[1]);
                System.out.printf("[Client] Tentativa %d/3 de ligação ao PRIMARY %s:%d%n", attempt, ip, port);

                if (attemptTcpConnection(ip, port)) {
                    System.out.println("[Client] Ligação ao PRIMARY estabelecida ✅");
                    currentServerIp = ip;
                    currentServerPort = port;
                    hasEverConnected = true;
                    lastConnectionFailed = false;
                    return true;
                } else {
                    System.err.println("[Client] Ligação TCP ao PRIMARY falhou ❌");
                }
            }

            if (attempt < 3) {
                System.out.println("[Client] Aguardando 20s antes da próxima tentativa...");
                sleep(RETRY_DELAY_MS);
            }
        }

        System.err.println("[Client] Todas as tentativas de ligação ao servidor falharam ❌. Cliente vai morrer.");
        running = false;
        return false;
    }

    private boolean attemptTcpConnection(String ip, int port) {
        closeResources();
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), CONNECTION_TIMEOUT_MS);

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // se o servidor enviar logo uma mensagem de boas-vindas, podes lê-la aqui
            try {
                Object welcome = in.readObject();
                System.out.println("[Client] Conectado: " + welcome);
            } catch (EOFException ignore) {
                // servidor não enviou nada, seguimos à mesma
            }

            activeSocket = socket;
            return true;

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Client] Erro ao ligar TCP: " + e.getMessage());
            return false;
        }
    }

    private void listenAndMaintainSession() {
        try {
            if (activeSocket == null || activeSocket.isClosed()) {
                return;
            }

            activeSocket.setSoTimeout(0);

            while (running && activeSocket != null && !activeSocket.isClosed()) {
                Object received;

                try {
                    received = in.readObject();
                } catch (SocketTimeoutException e) {
                    // sem timeout configurado, não deve acontecer; se acontecer, ignora
                    continue;
                }

                synchronized (lock) {
                    if (expectingResponse) {
                        syncResponse = received;
                        expectingResponse = false;
                        lock.notifyAll();
                    } else {
                        System.out.println("[Server Async Push] " + received);
                    }
                }
            }

        } catch (EOFException e) {
            System.out.println("[Client] Servidor fechou a ligação.");
            lastConnectionFailed = true;

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Client] Conexão perdida: " + e.getMessage());
            lastConnectionFailed = true;

            synchronized (lock) {
                syncResponse = null;
                expectingResponse = false;
                lock.notifyAll();
            }

        } finally {
            closeResources();
        }
    }

    private String[] requestActiveServer() {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] buf = "REQUEST_SERVER".getBytes();
            socket.send(new DatagramPacket(buf, buf.length, InetAddress.getByName(directoryHost), directoryPort));
            socket.setSoTimeout(5000);
            byte[] recv = new byte[256];
            DatagramPacket packet = new DatagramPacket(recv, recv.length);
            socket.receive(packet);
            String response = new String(packet.getData(), 0, packet.getLength()).trim();
            if (response.equals("NO_SERVER_AVAILABLE")) return null;
            return response.split(" ");
        } catch (Exception e) {
            return null;
        }
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
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    // ==========================
    // Métodos da ClientAPI (os teus, intactos)
    // ==========================

    @Override
    public String sendLogin(String email, String pwd) throws IOException {
        if (out == null) throw new IOException("Sem ligação TCP.");

        synchronized (lock) {
            try {
                expectingResponse = true;
                syncResponse = null;

                User loginUser = new User(null, email, pwd);
                Message msg = new Message(Command.LOGIN, loginUser);
                out.writeObject(msg);
                out.flush();

                lock.wait(5000);

                if (syncResponse instanceof Message responseMsg && responseMsg.getData() instanceof User u) {
                    String role = u.getRole() != null ? u.getRole() : "student";
                    String name = u.getName() != null ? u.getName() : email;
                    String extra = u.getExtra() != null ? u.getExtra() : "";
                    return "OK;" + role + ";" + name + ";" + extra;
                }

                return "LOGIN_FAILED";

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "ERROR";
            }
        }
    }

    @Override
    public boolean register(String role, String name, String id, String email, String password) throws IOException {
        if (out == null) throw new IOException("Sem ligação TCP.");

        synchronized (lock) {
            try {
                expectingResponse = true;
                syncResponse = null;

                User newUser;
                if ("Teacher".equalsIgnoreCase(role)) {
                    newUser = new Teacher(name, email, password, id);
                } else {
                    newUser = new Student(name, email, password, id);
                }
                newUser.setRole(role.toLowerCase());

                Command cmd = "Teacher".equalsIgnoreCase(role) ? Command.REGISTER_TEACHER : Command.REGISTER_STUDENT;

                out.writeObject(new Message(cmd, newUser));
                out.flush();

                lock.wait(5000);

                if (syncResponse instanceof Boolean b) return b;
                if (syncResponse instanceof Message m && m.getData() instanceof Boolean b) return b;

                return false;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    @Override
    public QuestionResult createQuestion(User user, String text, List<String> options, String correctOption,
                                         LocalDate sd, LocalTime st, LocalDate ed, LocalTime et) throws IOException {
        if (out == null) throw new IOException("Sem ligação TCP.");

        synchronized (lock) {
            try {
                expectingResponse = true;
                syncResponse = null;

                LocalDateTime start = LocalDateTime.of(sd, st);
                LocalDateTime end = LocalDateTime.of(ed, et);

                Question question = new Question(
                        text,
                        correctOption,
                        options.toArray(new String[0]),
                        start,
                        end,
                        user.getEmail()
                );
                System.out.println("A criar pergunta com ID local: " + question.getId());

                out.writeObject(new Message(Command.CREATE_QUESTION, question));
                out.flush();

                lock.wait(5000);

                if (syncResponse == null) {
                    expectingResponse = false;
                    return new QuestionResult(false, null);
                }

                if (syncResponse instanceof Boolean success) {
                    return new QuestionResult(success, success ? question.getId() : null);
                }

                if (syncResponse instanceof Message m && m.getData() instanceof Boolean success) {
                    return new QuestionResult(success, success ? question.getId() : null);
                }

                return new QuestionResult(false, null);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new QuestionResult(false, null);
            }
        }
    }

    @Override
    public String validateQuestionCode(String code) throws IOException {
        if (out == null) throw new IOException("Sem ligação TCP.");

        synchronized (lock) {
            try {
                expectingResponse = true;
                syncResponse = null;

                out.writeObject(new Message(Command.VALIDATE_QUESTION_CODE, code));
                out.flush();

                lock.wait(5000);

                if (syncResponse instanceof Message m && m.getData() instanceof String res) {
                    return res; // "VALID", "INVALID", "EXPIRED", etc.
                }
                return "TIMEOUT";
            } catch (InterruptedException e) {
                return "ERROR";
            }
        }
    }

    @Override
    public QuestionData getQuestionByCode(String code) {
        if (out == null) return null; // Devia lançar exceção, mas a interface retorna objeto

        synchronized (lock) {
            try {
                expectingResponse = true;
                syncResponse = null;

                out.writeObject(new Message(Command.GET_QUESTION, code));
                out.flush();

                lock.wait(5000);

                if (syncResponse instanceof Message m && m.getData() instanceof Question q) {
                    // Converter objeto Question (comum) para QuestionData (record do cliente)
                    return new QuestionData(q.getQuestion(), List.of(q.getOptions()));
                }
                return null;

            } catch (Exception e) {
                return null;
            }
        }
    }

    @Override
    public boolean submitAnswer(User user, String code, int index) throws IOException {
        if (out == null) return false;

        synchronized (lock) {
            try {
                expectingResponse = true;
                syncResponse = null;

                Object[] payload = new Object[]{user.getEmail(), code, index};

                out.writeObject(new Message(Command.SUBMIT_ANSWER, payload));
                out.flush();

                lock.wait(5000);

                if (syncResponse instanceof Message m && m.getData() instanceof Boolean b) {
                    return b;
                }
                return false;

            } catch (InterruptedException e) {
                return false;
            }
        }
    }

    @Override
    public List<Question> getTeacherQuestions(User user, String filter) throws IOException {
        if (out == null) return new ArrayList<>();

        synchronized (lock) {
            try {
                expectingResponse = true;
                syncResponse = null;

                // Envia o email do professor
                out.writeObject(new Message(Command.GET_TEACHER_QUESTIONS, user.getEmail()));
                out.flush();

                lock.wait(5000);

                if (syncResponse instanceof Message m && m.getData() instanceof List<?> list) {
                    return (List<Question>) list;
                }
                return new ArrayList<>();

            } catch (InterruptedException e) {
                return new ArrayList<>();
            }
        }
    }

    @Override
    public List<HistoryItem> getStudentHistory(User user, LocalDate start, LocalDate end, String filter) {
        if (out == null) return new ArrayList<>();

        synchronized (lock) {
            try {
                expectingResponse = true;
                syncResponse = null;

                out.writeObject(new Message(Command.GET_STUDENT_HISTORY, user.getEmail()));
                out.flush();

                lock.wait(5000);

                if (syncResponse instanceof Message m && m.getData() instanceof List<?> list) {
                    return (List<HistoryItem>) list;
                }
                return new ArrayList<>();
            } catch (InterruptedException e) {
                return new ArrayList<>();
            } catch (IOException e) {
                return new ArrayList<>();
            }
        }
    }

    @Override
    public TeacherResultsData getQuestionResults(User user, String questionCode) {
        if (out == null) return null;

        synchronized (lock) {
            try {
                expectingResponse = true;
                syncResponse = null;

                out.writeObject(new Message(Command.GET_QUESTION_RESULTS, questionCode));
                out.flush();

                lock.wait(5000);

                if (syncResponse instanceof Message m && m.getData() instanceof TeacherResultsData data) {
                    System.out.println("[Client] Recebidos resultados para a questão " + questionCode);
                    System.out.println("[Client] Recebidos resultados para a questão " + data);
                    return data;
                }
                return null;
            } catch (InterruptedException | IOException e) {
                return null;
            }
        }
    }

    @Override
    public AnswerResultData getAnswerResult(User user, String code) {
        // ainda não implementado no teu código original
        return null;
    }

    public void stop() {
        running = false;
        closeResources();
    }
}
