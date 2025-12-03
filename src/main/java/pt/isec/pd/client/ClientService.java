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
    private static final int MAX_SAME_SERVER_RETRY = 3;
    private static final long SAME_SERVER_RETRY_DELAY_MS = 20000;
    private static final int CONNECTION_TIMEOUT_MS = 5000;

    private final String directoryHost;
    private final int directoryPort;

    private String currentServerIp = null;
    private int currentServerPort = -1;
    private int sameServerFailureCount = 0;

    private Socket activeSocket = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    private volatile boolean running = true;

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

            boolean sameServer = newIp.equals(currentServerIp) && newPort == currentServerPort;

            if (sameServer && sameServerFailureCount >= 1) {
                sameServerFailureCount++;
                if (sameServerFailureCount > MAX_SAME_SERVER_RETRY) {
                    System.err.println("[Client] Esgotadas tentativas para o mesmo servidor.");
                    return false;
                }
                System.out.printf("[Client] Mesmo servidor. Esperando %ds...\n", SAME_SERVER_RETRY_DELAY_MS / 1000);
                sleep(SAME_SERVER_RETRY_DELAY_MS);
                continue;
            }

            if (attemptTcpConnection(newIp, newPort)) {
                currentServerIp = newIp;
                currentServerPort = newPort;
                sameServerFailureCount = 0;
                return true;
            }

            System.err.println("[Client] Falhou a ligação TCP.");
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

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            Object welcome = in.readObject();
            System.out.println("[Client] Conectado: " + welcome);

            activeSocket = socket;
            return true;

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Client] Erro ao ligar TCP: " + e.getMessage());
            return false;
        }
    }

    private void listenAndMaintainSession() {
        boolean hardFailure = false;
        try {
            if (activeSocket != null) activeSocket.setSoTimeout(0);

            while (running && activeSocket != null && !activeSocket.isClosed()) {
                try {
                    Object received = in.readObject();

                    synchronized (lock) {
                        if (expectingResponse) {
                            syncResponse = received;
                            expectingResponse = false;
                            lock.notifyAll();
                        } else {
                            System.out.println("[Server Async Push] " + received);
                        }
                    }
                } catch (SocketTimeoutException e) {
                    // Ignorar timeout
                }
            }

        } catch (EOFException e) {
            System.out.println("[Client] Servidor fechou a ligação.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Client] Conexão perdida: " + e.getMessage());
            hardFailure = true;

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

                Object[] payload = new Object[]{ user.getEmail(), code, index };

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
        in = null; out = null; activeSocket = null;
    }

    private void sleep(long ms) {
        try { TimeUnit.MILLISECONDS.sleep(ms); } catch (InterruptedException ignored) {}
    }

    @Override public AnswerResultData getAnswerResult(User user, String code) { return null; }
}
