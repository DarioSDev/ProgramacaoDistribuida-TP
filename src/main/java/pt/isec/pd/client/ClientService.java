package pt.isec.pd.client;

import javafx.application.Platform;
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
    private static final long RECONN_DELAY_MS = 20000; // 20 segundos
    private static final int CONNECTION_TIMEOUT_MS = 5000;

    private final String directoryHost;
    private final int directoryPort;

    private String currentServerIp = null;
    private int currentServerPort = -1;

    private Socket activeSocket = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    private volatile boolean running = true;
    private volatile boolean isAuthenticated = false;

    private final Object lock = new Object();
    private Object syncResponse = null;
    private boolean expectingResponse = false;

    public ClientService(String directoryHost, int directoryPort) {
        this.directoryHost = directoryHost;
        this.directoryPort = directoryPort;
    }

    public void start() {
        System.out.println("[Client] Iniciando cliente REAL (Object Streams)...");

        if (!tryConnectAndAuthenticate(false)) {
            System.err.println("[Client] Falha na ligação inicial. A terminar.");
            closeResources();
            return;
        }

        while (running) {
            // Se a ligação caiu, tenta reconectar com lógica de failover (isReconnect = true)
            if (activeSocket == null || activeSocket.isClosed()) {
                if (!tryConnectAndAuthenticate(true)) {
                    System.err.println("[Client] Falha crítica na reconexão. A terminar cliente.");
                    break;
                }
            }

            listenAndMaintainSession();
        }

        closeResources();
        System.out.println("[Client] Cliente REAL encerrado.");
    }

    private boolean tryConnectAndAuthenticate(boolean isReconnect) {
        String oldIp = currentServerIp;
        int oldPort = currentServerPort;
        int attempts = isReconnect ? 2 : 1; // 1 tentativa para arranque, 2 para reconexão ao mesmo Primary

        for (int attempt = 1; attempt <= attempts && running; attempt++) {

            String[] info = requestActiveServer(); // Pede ao Directory o Primary atual

            if (info == null) {
                // Requisito: Se falhar na localização do Primary no arranque/reconexão, termina (a menos que seja o 20s delay)
                if (!isReconnect) {
                    return false;
                } else if (attempt == attempts && oldPort == currentServerPort) {
                    return false; // Última tentativa ao mesmo Primary
                }
                // No modo reconexão, se o Directory falhar temporariamente, damos 20s de tolerância.
                System.out.println("[Client] Diretoria indisponível. Aguardando 20s...");
                sleep(RECONN_DELAY_MS);
                continue;
            }

            String newIp = info[0];
            int newPort = Integer.parseInt(info[1]);

            boolean isSamePrimary = (oldPort == newPort && oldIp != null && oldIp.equals(newIp));

            // Cenário 1: Primary diferente (Reconexão Imediata - B)
            if (isReconnect && !isSamePrimary) {
                System.out.printf("[Client] Primary mudou para %s:%d. A tentar reconexão imediata...%n", newIp, newPort);
                if (attemptTcpConnection(newIp, newPort)) {
                    currentServerIp = newIp;
                    currentServerPort = newPort;
                    return true;
                } else {
                    // Requisito: Se falhar na ligação/autenticação ao novo Primary, termina.
                    running = false;
                    return false;
                }
            }

            // Cenário 2: Primary igual ou Arranque Inicial
            System.out.printf("[Client] Tentativa %d/%d de ligação ao Primary %s:%d%n", attempt, attempts, newIp, newPort);

            // [R11]
            if (attemptTcpConnection(newIp, newPort)) {
                currentServerIp = newIp;
                currentServerPort = newPort;
                return true;
            } else {
                System.err.println("[Client] Ligação TCP ao Primary falhou ❌");

                // Requisito C/D: Se falhar e for o mesmo Primary, deve tentar apenas mais uma vez após 20s.
                if (isReconnect && isSamePrimary && attempt == 1) {
                    System.out.println("[Client] Primary inativo! Aguardando 20s para tentar novamente...");
                    sleep(RECONN_DELAY_MS); // Espera 20s antes da tentativa 2 (a última)
                } else if (isReconnect && isSamePrimary && attempt == 2) {
                    // Última falha ao mesmo Primary.
                    running = false;
                    return false;
                } else if (!isReconnect) {
                    // Falha na tentativa de arranque inicial (apenas 1 tentativa)
                    running = false;
                    return false;
                }
            }
        }
        return false; // Nunca deve chegar aqui se o loop estiver bem gerido
    }

    private boolean attemptTcpConnection(String ip, int port) {
        closeResources();
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), CONNECTION_TIMEOUT_MS);

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            try {
                Object welcome = in.readObject();
                System.out.println("[Client] Conectado: " + welcome);
            } catch (EOFException ignore) {}

            activeSocket = socket;
            return true;

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Client] Erro ao ligar TCP: " + e.getMessage());
            return false;
        }
    }

    // [R7]
    private void listenAndMaintainSession() {
        boolean lastConnectionFailed = false;
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

                // [R7]
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
            if (!this.isAuthenticated) {
                System.err.println("[Client] Ligação encerrada antes da autenticação. A terminar aplicação.");
                running = false;
                Platform.runLater(() -> {
                    Platform.exit();
                    System.exit(0);
                });
            } else {
                lastConnectionFailed = true;
                this.isAuthenticated = false;
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Client] Conexão perdida: " + e.getMessage());
            if (!this.isAuthenticated) {
                System.err.println("[Client] Ligação perdida antes da autenticação. A terminar aplicação.");
                running = false;
                Platform.runLater(() -> {
                    Platform.exit();
                    System.exit(0);
                });
            } else {
                lastConnectionFailed = true;
                this.isAuthenticated = false;
            }

            synchronized (lock) {
                syncResponse = null;
                expectingResponse = false;
                lock.notifyAll();
            }

        } finally {
            closeResources();
        }
    }

    // [R9]
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

    // [R15]
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
                    this.isAuthenticated = true;
                    String role = u.getRole() != null ? u.getRole() : "student";
                    String name = u.getName() != null ? u.getName() : email;
                    String extra = u.getExtra() != null ? u.getExtra() : "";
                    return "OK;" + role + ";" + name + ";" + extra;
                }

                this.isAuthenticated = false;
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

    // [R18]
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
    public StudentHistory getStudentHistory(User user, LocalDate start, LocalDate end, String filter) throws IOException {
        if (out == null) return null;

        synchronized (lock) {
            try {
                expectingResponse = true;
                syncResponse = null;

                out.writeObject(new Message(Command.GET_STUDENT_HISTORY, user.getEmail()));
                out.flush();

                lock.wait(5000);

                if (syncResponse instanceof Message m && m.getData() instanceof StudentHistory history) {
                    return history;
                }
                return null;
            } catch (InterruptedException e) {
                return null;
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
    public boolean editQuestion(Question q) throws IOException {
        if (out == null) throw new IOException("Sem ligação TCP.");

        synchronized (lock) {
            try {
                expectingResponse = true;
                syncResponse = null;

                out.writeObject(new Message(Command.EDIT_QUESTION, q));
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
    public boolean deleteQuestion(String questionId) {
        if (out == null) return false;

        synchronized (lock) {
            try {
                expectingResponse = true;
                syncResponse = null;

                out.writeObject(new Message(Command.DELETE_QUESTION, questionId));
                out.flush();

                lock.wait(5000);

                if (syncResponse instanceof Boolean b) return b;
                if (syncResponse instanceof Message m && m.getData() instanceof Boolean b) return b;

                return false;

            } catch (InterruptedException | IOException e) {
                return false;
            }
        }
    }

    @Override
    public AnswerResultData getAnswerResult(User user, String code) {
        return null;
    }

    public void stop() {
        running = false;
        closeResources();
    }
}
