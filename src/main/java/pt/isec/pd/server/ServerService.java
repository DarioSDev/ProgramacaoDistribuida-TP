package pt.isec.pd.server;

import pt.isec.pd.common.*;
import pt.isec.pd.db.DatabaseManager;
import pt.isec.pd.db.QueryPerformer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ServerService {
    private static final int DIRECTORY_TIMEOUT_MS = 26000;

    private final String directoryHost;
    private final int directoryPort;
    private final String multicastGroupIp;
    private String dbDirectoryPath;

    private DatabaseManager dbManager;
    private QueryPerformer queryPerformer;

    private int tcpClientPort = 0;
    private int tcpDbPort = 0;
    private int udpPort = 0;

    private volatile int primaryTcpClientPort = -1;
    private volatile int primaryDbPort = -1;
    private volatile InetAddress primaryIp = null;

    private volatile boolean isPrimary = false;
    private volatile boolean running = true;

    private DatagramSocket udpSocket;
    private ServerSocket clientServerSocket;
    private ServerSocket dbServerSocket;

    private HeartbeatManager heartbeatManager;
    private ExecutorService clientPool;
    private Thread directoryListenerThread;

    public ServerService(String directoryHost, int directoryPort, String multicastGroupIp, String dbDirectoryPath) {
        this.directoryHost = directoryHost;
        this.directoryPort = directoryPort;
        this.multicastGroupIp = multicastGroupIp;
        this.dbDirectoryPath = dbDirectoryPath;
    }

    public void start() {
        try {
            udpSocket = new DatagramSocket();
            udpPort = udpSocket.getLocalPort();

            clientServerSocket = new ServerSocket(0);
            dbServerSocket = new ServerSocket(0);
            tcpClientPort = clientServerSocket.getLocalPort();
            tcpDbPort = dbServerSocket.getLocalPort();

            System.out.printf("[Server] Portos: UDP=%d, Cliente=%d, BD=%d%n", udpPort, tcpClientPort, tcpDbPort);

            if (!registerAndGetPrimary()) {
                System.err.println("[Server] Falha no registo com o Directory. A terminar.");
                return;
            }

            initializeDatabaseLogic();

            heartbeatManager = new HeartbeatManager(udpSocket, directoryHost, directoryPort,
                    tcpClientPort, tcpDbPort, dbManager, 5000, this);
            heartbeatManager.start();
            if (dbManager != null) {
                dbManager.setHeartbeatManager(heartbeatManager);
            }

            directoryListenerThread = startDirectoryListener();
            startClientListener();
            startDbSyncListener();

            System.out.println("[Server] Servidor iniciado com sucesso.");
            if (isPrimary) {
                System.out.println("[Server] Este servidor é o PRINCIPAL.");
                queryPerformer = new QueryPerformer(dbManager);
            } else {
                System.out.printf("[Server] Modo Backup. Primary: %s:%d%n",
                        primaryIp.getHostAddress(), primaryTcpClientPort);
            }

            new Scanner(System.in).nextLine();

        } catch (IOException e) {
            System.err.println("[Server] Erro fatal: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    public int getPrimaryClientTcpPort() {
        return primaryTcpClientPort;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    // Chamado pelo HeartbeatManager quando recebe atualização do Directory
    public synchronized void updatePrimary(String ip, int clientPort, int dbPort) {
        try {
            InetAddress newIp = InetAddress.getByName(ip);
            boolean wasPrimary = isPrimary;

            primaryIp = newIp;
            primaryTcpClientPort = clientPort;
            primaryDbPort = dbPort;
            isPrimary = (clientPort == tcpClientPort);

            if (isPrimary && !wasPrimary) {
                System.out.println("[Server] PROMOVIDO A PRINCIPAL!");
                if (queryPerformer == null && dbManager != null) {
                    queryPerformer = new QueryPerformer(dbManager);
                    System.out.println("[Server] QueryPerformer inicializado após promoção.");
                }
            }
        } catch (Exception e) {
            System.err.println("[Server] Erro ao atualizar primary: " + e.getMessage());
        }
    }

    private void initializeDatabaseLogic() {
        dbDirectoryPath = dbDirectoryPath + "/" + tcpClientPort;
        File dir = new File(dbDirectoryPath);
        if (!dir.exists()) {
            System.out.println("[DB-INIT] Criando diretório: " + dir.getAbsolutePath());
            dir.mkdirs();
        }

        if (isPrimary) {
            File selectedFile = chooseOrCreateDatabaseFile(dir);
            dbManager = new DatabaseManager(dbDirectoryPath, selectedFile.getName());
            System.out.println("[DB-INIT][PRIMARY] Criando schema...");
            dbManager.createSchema();
            System.out.println("[DB-INIT][PRIMARY] Schema criado. Versão: " + dbManager.getDbVersion());
        } else {
            System.out.println("[DB-INIT][BACKUP] Sincronizando BD com o Primary...");
            if (!downloadDatabaseFromPrimary()) {
                System.err.println("[DB-INIT][BACKUP] Falha crítica na sincronização.");
                shutdown();
                System.exit(1);
            }
            dbManager = new DatabaseManager(dbDirectoryPath, "data.db");
            System.out.println("[DB-INIT][BACKUP] BD sincronizada. Versão: " + dbManager.getDbVersion());
        }
    }

    private File chooseOrCreateDatabaseFile(File dir) {
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".db"));
        if (files == null || files.length == 0) {
            System.out.println("[DB-INIT][PRIMARY] Criando nova base de dados 'data.db'");
            return new File(dir, "data.db");
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        File selected = files[0];
        System.out.println("[DB-INIT][PRIMARY] Usando BD mais recente: " + selected.getName());
        return selected;
    }

    private boolean registerAndGetPrimary() throws IOException {
        udpSocket.setSoTimeout(DIRECTORY_TIMEOUT_MS);
        String msg = String.format("REGISTER %d %d %d", tcpClientPort, tcpDbPort, udpPort);
        DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(),
                InetAddress.getByName(directoryHost), directoryPort);
        udpSocket.send(packet);
        System.out.println("[Server] Registo enviado: " + msg);

        byte[] buffer = new byte[256];
        DatagramPacket recv = new DatagramPacket(buffer, buffer.length);
        try {
            udpSocket.receive(recv);
            String response = new String(recv.getData(), 0, recv.getLength()).trim();
            System.out.println("[Server] Confirmação do Directory: " + response);

            String[] parts = response.split("\\s+");
            if (parts.length >= 4 && parts[0].equals("PRIMARY")) {
                updatePrimary(parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                udpSocket.setSoTimeout(0);
                return true;
            }
        } catch (SocketTimeoutException e) {
            System.err.println("[Server] Timeout no registo.");
        }
        return false;
    }

    private boolean downloadDatabaseFromPrimary() {
        File target = new File(dbDirectoryPath, "data.db");
        System.out.printf("[SYNC][BACKUP] Recebendo BD de %s:%d%n",
                primaryIp.getHostAddress(), primaryDbPort);

        try (Socket socket = new Socket(primaryIp, primaryDbPort);
             InputStream in = socket.getInputStream();
             FileOutputStream fos = new FileOutputStream(target)) {

            socket.setSoTimeout(10000);
            byte[] buf = new byte[8192];
            int read;
            long total = 0;
            while ((read = in.read(buf)) != -1) {
                fos.write(buf, 0, read);
                total += read;
            }
            System.out.println("[SYNC][BACKUP] Download concluído (" + total + " bytes)");
            return true;
        } catch (IOException e) {
            System.err.println("[SYNC][BACKUP] Falha: " + e.getMessage());
            if (target.exists()) target.delete();
            return false;
        }
    }

    private void sendDatabase(Socket peer) {
        if (dbManager == null || !dbManager.isSchemaReady()) {
            System.err.println("[SYNC][PRIMARY] BD não pronta para envio.");
            return;
        }
        File dbFile = dbManager.getDbFile();
        System.out.println("[SYNC][PRIMARY] Enviando BD (" + dbFile.length() + " bytes)...");

        dbManager.getReadLock().lock();
        try (FileInputStream fis = new FileInputStream(dbFile);
             OutputStream out = peer.getOutputStream()) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = fis.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
            out.flush();
        } catch (IOException e) {
            System.err.println("[SYNC][PRIMARY] Erro no envio: " + e.getMessage());
        } finally {
            dbManager.getReadLock().unlock();
        }
    }

    private Thread startDirectoryListener() {
        Thread t = new Thread(() -> {
            byte[] buf = new byte[256];
            while (running) {
                try {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    udpSocket.setSoTimeout(8000);
                    udpSocket.receive(p);
                    String msg = new String(p.getData(), 0, p.getLength()).trim();

                    if (msg.startsWith("PRIMARY")) {
                        String[] parts = msg.split("\\s+");
                        if (parts.length >= 4) {
                            updatePrimary(parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                        }
                    }
                } catch (SocketTimeoutException ignored) {
                } catch (Exception e) {
                    if (running) System.err.println("[DirectoryListener] Erro: " + e.getMessage());
                }
            }
        }, "Directory-Listener");
        t.start();
        return t;
    }

    private void startClientListener() {
        clientPool = Executors.newFixedThreadPool(10);
        new Thread(() -> {
            System.out.println("[ClientListener] Aguardando conexões no porto " + tcpClientPort);
            while (running) {
                try {
                    Socket client = clientServerSocket.accept();
                    if (isPrimary) {
                        clientPool.submit(() -> handleClient(client));
                    } else {
                        rejectClient(client);
                    }
                } catch (IOException e) {
                    if (running) System.err.println("[ClientListener] Erro: " + e.getMessage());
                }
            }
        }, "ClientListener").start();
    }

    private void rejectClient(Socket client) {
        try (ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream())) {
            oos.writeObject(new Message(Command.CONNECTION, "ERRO: Este servidor está em modo Backup. Conecte-se ao Primary."));
            oos.flush();
        } catch (IOException ignored) {}
        try { client.close(); } catch (IOException ignored) {}
    }

    private void handleClient(Socket client) {
        ObjectInputStream in = null;
        ObjectOutputStream out = null;

        try {
            out = new ObjectOutputStream(client.getOutputStream());
            out.flush();
            in = new ObjectInputStream(client.getInputStream());

            System.out.println("[Server] Cliente conectado: " + client.getRemoteSocketAddress());

            // Mensagem de boas-vindas
            out.writeObject(new Message(Command.CONNECTION, "BEM-VINDO AO SERVIDOR PD"));
            out.flush();

            Object obj;
            while ((obj = in.readObject()) != null) {
                if (!(obj instanceof Message msg)) {
                    continue;
                }

                System.out.println("[Server] Recebido: " + msg.getCommand());

                Message response = null;

                try {
                    switch (msg.getCommand()) {
                        case LOGIN -> {
                            if (msg.getData() instanceof User loginUser) {
                                RoleType role = queryPerformer.authenticate(loginUser.getEmail(), loginUser.getPassword());
                                if (role != RoleType.NONE) {
                                    User u = queryPerformer.getUser(loginUser.getEmail());
                                    if (u == null) u = new User(null, loginUser.getEmail(), null);
                                    u.setRole(role.name().toLowerCase());
                                    response = new Message(Command.LOGIN, u);
                                } else {
                                    response = new Message(Command.LOGIN, null);
                                }
                            }
                        }

                        case REGISTER_STUDENT, REGISTER_TEACHER -> {
                            if (msg.getData() instanceof User newUser) {
                                boolean success = queryPerformer.registerUser(newUser);
                                response = new Message(msg.getCommand(), success);
                            } else {
                                response = new Message(msg.getCommand(), false);
                            }
                        }

                        case CREATE_QUESTION -> {
                            if (msg.getData() instanceof Question q) {
                                boolean ok = queryPerformer.saveQuestion(q);
                                response = new Message(Command.CREATE_QUESTION, ok);
                            } else {
                                response = new Message(Command.CREATE_QUESTION, false);
                            }
                        }

                        case VALIDATE_QUESTION_CODE -> {
                            if (msg.getData() instanceof String code) {
                                String result = queryPerformer.validateQuestionCode(code);
                                out.writeObject(new Message(Command.VALIDATE_QUESTION_CODE, result));
                                out.flush();
                                continue; // já enviada
                            }
                        }

                        case GET_QUESTION -> {
                            if (msg.getData() instanceof String code) {
                                Question q = queryPerformer.getQuestionByCode(code);
                                out.writeObject(new Message(Command.GET_QUESTION, q));
                                out.flush();
                                continue;
                            }
                        }

                        case SUBMIT_ANSWER -> {
                            if (msg.getData() instanceof Object[] arr && arr.length == 3) {
                                String email = (String) arr[0];
                                String code = (String) arr[1];
                                int index = (Integer) arr[2];
                                boolean ok = queryPerformer.submitAnswer(email, code, index);
                                out.writeObject(new Message(Command.SUBMIT_ANSWER, ok));
                                out.flush();
                                continue;
                            }
                        }

                        case GET_TEACHER_QUESTIONS -> {
                            if (msg.getData() instanceof String email) {
                                List<Question> list = queryPerformer.getTeacherQuestions(email);
                                out.writeObject(new Message(Command.GET_TEACHER_QUESTIONS, new ArrayList<>(list)));
                                out.flush();
                                continue;
                            }
                        }

                        case LOGOUT -> {
                            response = new Message(Command.LOGOUT, "BYE");
                        }

                        default -> {
                            response = new Message(Command.UNKNOWN, "Comando desconhecido");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    response = new Message(Command.ERROR, "Erro no servidor: " + e.getMessage());
                }

                // Envia resposta (exceto nos casos já enviados acima)
                if (response != null) {
                    out.writeObject(response);
                    out.flush();
                }

                if (msg.getCommand() == Command.LOGOUT) break;
            }

        } catch (EOFException e) {
            System.out.println("[Server] Cliente desconectado: " + client.getRemoteSocketAddress());
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Server] Erro com cliente: " + e.getMessage());
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
            try { client.close(); } catch (Exception ignored) {}
        }
    }

    private void startDbSyncListener() {
        new Thread(() -> {
            while (running) {
                try {
                    Socket peer = dbServerSocket.accept();
                    sendDatabase(peer);
                    peer.close();
                } catch (IOException e) {
                    if (running) System.err.println("[DbSync] Erro: " + e.getMessage());
                }
            }
        }, "DbSyncListener").start();
    }

    private void sendUnregister() {
        try {
            String msg = "UNREGISTER " + tcpClientPort;
            byte[] data = msg.getBytes();
            DatagramPacket p = new DatagramPacket(data, data.length,
                    InetAddress.getByName(directoryHost), directoryPort);
            udpSocket.send(p);
            System.out.println("[Server] UNREGISTER enviado.");
        } catch (Exception ignored) {}
    }

    public void shutdown() {
        if (!running) return;
        running = false;
        System.out.println("\n[Server] A encerrar...");

        sendUnregister();
        if (heartbeatManager != null) heartbeatManager.shutdown();
        if (clientPool != null) clientPool.shutdownNow();
        if (directoryListenerThread != null) directoryListenerThread.interrupt();

        try { if (udpSocket != null) udpSocket.close(); } catch (Exception ignored) {}
        try { if (clientServerSocket != null) clientServerSocket.close(); } catch (Exception ignored) {}
        try { if (dbServerSocket != null) dbServerSocket.close(); } catch (Exception ignored) {}

        System.out.println("[Server] Encerrado com sucesso.");
    }
}