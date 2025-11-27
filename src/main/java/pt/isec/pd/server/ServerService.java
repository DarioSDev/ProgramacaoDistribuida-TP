package pt.isec.pd.server;

import pt.isec.pd.common.*;
import pt.isec.pd.db.DatabaseManager;
import pt.isec.pd.db.QueryPerformer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ServerService {
    private static final String MULTICAST_IP = "230.30.30.30";
    private static final int MULTICAST_PORT = 3030;
    private static final int HEARTBEAT_INTERVAL_MS = 5000;
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

    private int primaryTcpClientPort = -1;
    private int primaryDbPort = -1;

    private InetAddress primaryIp = null;

    private volatile boolean isPrimary = false;
    private volatile boolean running = true;

    private DatagramSocket udpSocket;
    private ServerSocket clientServerSocket;
    private ServerSocket dbServerSocket;
    private MulticastSocket multicastReceiverSocket;
    private MulticastSocket multicastSenderSocket;

    private HeartbeatManager heartbeatManager;
    private ExecutorService clientPool;
    private Thread directoryHeartbeatThread;

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

            heartbeatManager = new HeartbeatManager(udpSocket,
                    directoryHost,
                    directoryPort,
                    tcpClientPort,
                    tcpDbPort,
                    dbManager,
                    HEARTBEAT_INTERVAL_MS,
                    primaryIp);
            heartbeatManager.start();
            this.dbManager.setHeartbeatManager(this.heartbeatManager);

            directoryHeartbeatThread = startDirectoryHeartbeatListener();
            startClientListener();
            startDbSyncListener();

            multicastReceiverSocket = startMulticastReceiver();
            multicastSenderSocket = startMulticastHeartbeat();

            System.out.println("[Server] Servidor iniciado.");
            if (isPrimary) {
                System.out.println("[Server] Este é o servidor PRINCIPAL.");
            } else {
                System.out.printf("[Server] Servidor backup. Primary: %s:%d%n", primaryIp.getHostAddress(), primaryTcpClientPort);
            }

            new Scanner(System.in).nextLine();

        } catch (IOException e) {
            System.err.println("[Server] Erro fatal: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void initializeDatabaseLogic() {
        dbDirectoryPath = dbDirectoryPath + "/" + this.tcpClientPort;
        File dir = new File(dbDirectoryPath);
        if (!dir.exists()) {
            System.out.println("[DB-INIT] Diretório não existe. A criar: " + dir.getAbsolutePath());
            dir.mkdirs();
        } else {
            System.out.println("[DB-INIT] Diretório existe: " + dir.getAbsolutePath());
        }

        System.out.println("[DB-INIT] Diretório de BD: " + dir.getAbsolutePath());
        System.out.println("[DB-INIT] isPrimary = " + isPrimary);

        if (isPrimary) {
            File[] dbFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".db"));
            File selectedFile;

            if (dbFiles == null || dbFiles.length == 0) {
                System.out.println("[DB-INIT][PRIMARY] Nenhuma BD encontrada. Criando nova...");
                selectedFile = new File(dir, "data.db");
            } else {
                System.out.println("[DB-INIT][PRIMARY] BDs encontradas:");
                for (File f : dbFiles)
                    System.out.println("   → " + f.getName() + " (size=" + f.length() + " bytes)");

                Arrays.sort(dbFiles, Comparator.comparingLong(File::lastModified));
                selectedFile = dbFiles[dbFiles.length - 1];
                System.out.println("[DB-INIT][PRIMARY] Selecionada: " + selectedFile.getAbsolutePath());
            }

            this.dbManager = new DatabaseManager(dbDirectoryPath, selectedFile.getName());
            System.out.println("[DB-INIT][PRIMARY] A criar schema...");
            this.dbManager.createSchema();
            System.out.println("[DB-INIT][PRIMARY] Schema criado com sucesso!");
            System.out.println("[DB-INIT][PRIMARY] Versão BD: " + dbManager.getDbVersion());

            this.queryPerformer = new QueryPerformer(dbManager);

        } else {
            System.out.println("[DB-INIT][BACKUP] Modo backup. A aguardar sincronização...");
            if (!downloadDatabaseFromPrimary()) {
                System.err.println("[DB-INIT][BACKUP] ERRO: Falha ao receber base de dados.");
                shutdown();
                System.exit(1);
            }
        }
    }

    private boolean registerAndGetPrimary() throws IOException {
        udpSocket.setSoTimeout(DIRECTORY_TIMEOUT_MS);

        String msg = String.format("REGISTER %d %d %d", tcpClientPort, tcpDbPort, udpPort);
        byte[] buf = msg.getBytes();
        InetAddress dirAddr = InetAddress.getByName(directoryHost);

        DatagramPacket packet = new DatagramPacket(buf, buf.length, dirAddr, directoryPort);
        udpSocket.send(packet);
        System.out.println("[Server] Registo enviado: " + msg);

        byte[] recvBuf = new byte[256];
        DatagramPacket recv = new DatagramPacket(recvBuf, recvBuf.length);
        String response;

        try {
            udpSocket.receive(recv);
            response = new String(recv.getData(), 0, recv.getLength()).trim();
            System.out.println("[Server] Confirmação do Directory: " + response);

            String[] parts = response.split("\\s+");

            if (parts.length >= 4 && parts[0].equals("PRIMARY")) {
                primaryIp = InetAddress.getByName(parts[1]);
                primaryTcpClientPort = Integer.parseInt(parts[2]);
                primaryDbPort = Integer.parseInt(parts[3]);

                udpSocket.setSoTimeout(0);

                isPrimary = (primaryTcpClientPort == tcpClientPort);
                if (isPrimary) {
                    System.out.println("[Server] EU SOU O PRIMARY!");
                } else {
                    System.out.println("[Server] Servidor backup definido.");
                }
                return true;
            }
            return false;

        } catch (SocketTimeoutException e) {
            System.err.println("[Server] Timeout: Directory não respondeu.");
            return false;
        }
    }

    private boolean downloadDatabaseFromPrimary() {
        String newFileName = "data.db";
        File syncedFile = new File(dbDirectoryPath, newFileName);

        System.out.println("[SYNC][BACKUP] A tentar ligação ao primary para download:");
        System.out.println("   IP   = " + primaryIp);
        System.out.println("   PORT = " + primaryDbPort);
        System.out.println("   File destino = " + syncedFile.getAbsolutePath());

        try (Socket socket = new Socket(primaryIp, primaryDbPort);
             InputStream in = socket.getInputStream();
             FileOutputStream fos = new FileOutputStream(syncedFile)) {

            socket.setSoTimeout(5000);

            byte[] buffer = new byte[8192];
            int read;
            long totalBytes = 0;

            while ((read = in.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
                totalBytes += read;
            }

            System.out.println("[SYNC][BACKUP] Download concluído. Bytes recebidos = " + totalBytes);

            this.dbManager = new DatabaseManager(dbDirectoryPath, newFileName);

            System.out.println("[SYNC][BACKUP] BD recebida contém tabelas:");
            System.out.println("   version = " + dbManager.getDbVersion());

            return true;

        } catch (IOException e) {
            System.err.println("[SYNC][BACKUP] ERRO: " + e.getMessage());
            if (syncedFile.exists()) {
                System.err.println("[SYNC][BACKUP] Apagando ficheiro incompleto: " + syncedFile.getAbsolutePath());
                syncedFile.delete();
            }
            return false;
        }
    }


    private void sendDatabase(Socket peer) {
        if (dbManager == null) {
            System.err.println("[SYNC][PRIMARY] ERRO: dbManager == null");
            return;
        }

        File dbFile = dbManager.getDbFile();
        if (!dbFile.exists()) {
            System.err.println("[SYNC][PRIMARY] ERRO: ficheiro de BD não existe! " + dbFile.getAbsolutePath());
            return;
        }

        System.out.println("[SYNC][PRIMARY] A enviar BD ao backup:");
        System.out.println("   Ficheiro = " + dbFile.getAbsolutePath());
        System.out.println("   Tamanho  = " + dbFile.length() + " bytes");

        if (!dbManager.isSchemaReady()) {
            System.err.println("[SYNC][PRIMARY] Schema ainda não está pronto! NÃO envio BD.");
            return;
        }

        dbManager.getReadLock().lock();
        try {
            try (FileInputStream fis = new FileInputStream(dbFile);
                 OutputStream out = peer.getOutputStream()) {

                byte[] buffer = new byte[8192];
                int read;
                long totalSent = 0;

                while ((read = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    totalSent += read;
                }
                out.flush();
                peer.shutdownOutput();

                System.out.println("[SYNC][PRIMARY] Envio concluído. Total enviado = " + totalSent + " bytes");
            }
        } catch (IOException e) {
            System.err.println("[SYNC][PRIMARY] ERRO ao enviar: " + e.getMessage());
        } finally {
            dbManager.getReadLock().unlock();
        }
    }


    private Thread startDirectoryHeartbeatListener() {
        Thread listenerThread = new Thread(() -> {
            byte[] buf = new byte[256];
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    udpSocket.setSoTimeout(DIRECTORY_TIMEOUT_MS);
                    udpSocket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength()).trim();

                    if (msg.startsWith("PRIMARY")) {
                        processPrimaryUpdate(msg);
                    }
                } catch (SocketTimeoutException e) {
                    System.err.println("[Server] Directory inativo! Iniciando shutdown...");
                    shutdown();
                    System.exit(1);
                } catch (IOException e) {
                    if (running) System.err.println("[Server] Erro UDP: " + e.getMessage());
                }
            }
        }, "Dir-Heartbeat-Listener");

        listenerThread.start();
        return listenerThread;
    }

    private void processPrimaryUpdate(String response) {
        synchronized (this) {
            try {
                String[] parts = response.split("\\s+");

                if (parts.length >= 4 && parts[0].equals("PRIMARY")) {
                    InetAddress newPrimaryIp = InetAddress.getByName(parts[1]);
                    int newPrimaryTcpPort = Integer.parseInt(parts[2]);
                    int newPrimaryDbPort = Integer.parseInt(parts[3]);

                    boolean wasPrimary = this.isPrimary;

                    if (newPrimaryTcpPort == tcpClientPort) {
                        this.isPrimary = true;
                    } else {
                        this.isPrimary = false;
                    }

                    this.primaryIp = newPrimaryIp;
                    this.primaryTcpClientPort = newPrimaryTcpPort;
                    this.primaryDbPort = newPrimaryDbPort;

                    if (this.isPrimary && !wasPrimary) {
                        System.out.println("[Server] PROMOVIDO A PRINCIPAL!");
                    }
                }
            } catch (Exception e) {
                System.err.println("[Server] Erro ao processar atualização do Primary: " + e.getMessage());
            }
        }
    }

    private void startClientListener() {
        clientPool = Executors.newFixedThreadPool(10);
        new Thread(() -> {
            System.out.println("[ClientListener] A aguardar conexões no porto " + tcpClientPort);
            while (running) {
                try {
                    Socket client = clientServerSocket.accept();
                    if (isPrimary) {
                        clientPool.submit(() -> handleClient(client));
                    } else {
                        rejectClient(client);
                    }
                } catch (IOException e) {
                    if (running) System.err.println("[Server] Erro ClientListener: " + e.getMessage());
                }
            }
        }, "ClientListener").start();
    }

    private void rejectClient(Socket client) {
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(client.getOutputStream());
            out.flush();
            out.writeObject(new Message(Command.CONNECTION, "ERRO: Server is Backup mode. Connect to Primary."));
            out.flush();
        } catch (IOException ignored) {}
        finally {
            try { if (out != null) out.close(); } catch (IOException ignored) {}
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    private void handleClient(Socket client) {
        ObjectInputStream in = null;
        ObjectOutputStream out = null;

        try {
            out = new ObjectOutputStream(client.getOutputStream());
            out.flush();
            in = new ObjectInputStream(client.getInputStream());

            System.out.println("[Server] Cliente conectado: " + client.getRemoteSocketAddress());

            out.writeObject(new Message(Command.CONNECTION, "BEM-VINDO AO SERVIDOR PD"));
            out.flush();

            Object obj;
            while ((obj = in.readObject()) != null) {
                if (!(obj instanceof Message)) {
                    System.out.println("[Server] Recebido object não-Message: " + obj);
                    continue;
                }

                Message msg = (Message) obj;
                System.out.println("[Server] Recebido: " + msg);

                Command cmd = msg.getCommand();
                Object data = msg.getData();

                Message responseMsg = null;

                try {
                    switch (cmd) {
                        case LOGIN -> {
                            if (data instanceof User loginUser) {
                                RoleType role = queryPerformer.authenticate(loginUser.getEmail(), loginUser.getPassword());
                                if (role != RoleType.NONE) {
                                    User u = queryPerformer.getUser(loginUser.getEmail());
                                    if (u == null) u = new User(null, loginUser.getEmail(), null);
                                    u.setRole(role.name().toLowerCase());
                                    responseMsg = new Message(Command.LOGIN, u);
                                } else {
                                    responseMsg = new Message(Command.LOGIN, null);
                                }
                            } else {
                                responseMsg = new Message(Command.LOGIN, null);
                            }
                        }

                        case REGISTER_STUDENT, REGISTER_TEACHER -> {
                            if (data instanceof User newUser) {
                                boolean ok = queryPerformer.registerUser(newUser);
                                responseMsg = new Message(cmd, ok);
                            } else {
                                responseMsg = new Message(cmd, false);
                            }
                        }

                        case GET_USER_INFO -> {
                            if (data instanceof String email) {
                                User u = queryPerformer.getUser(email);
                                responseMsg = new Message(Command.GET_USER_INFO, u);
                            } else {
                                responseMsg = new Message(Command.GET_USER_INFO, null);
                            }
                        }

                        case CREATE_QUESTION -> {
                            if (data instanceof Question q) {
                                boolean ok = queryPerformer.saveQuestion(q);
                                responseMsg = new Message(Command.CREATE_QUESTION, ok);
                            } else {
                                responseMsg = new Message(Command.CREATE_QUESTION, false);
                            }
                        }

                        case LOGOUT -> {
                            responseMsg = new Message(Command.LOGOUT, "BYE");
                        }

                        default -> responseMsg = new Message(cmd, "UNKNOWN_COMMAND");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    responseMsg = new Message(cmd, "SERVER_ERROR: " + e.getMessage());
                }

                // Enviar resposta
                try {
                    out.writeObject(responseMsg);
                    out.flush();
                } catch (IOException e) {
                    System.err.println("[Server] Erro ao enviar resposta ao cliente: " + e.getMessage());
                    break;
                }

                if (cmd == Command.LOGOUT) break;
            }

        } catch (EOFException e) {
            System.out.println("[Server] Cliente fechou a ligação: " + client.getRemoteSocketAddress());
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Server] Erro na conexão com cliente: " + e.getMessage());
        } finally {
            // Fechar recursos
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            try { if (out != null) out.close(); } catch (IOException ignored) {}
            try { client.close(); } catch (IOException ignored) {}
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
                    if (running) System.err.println("[Server] Erro DbSync: " + e.getMessage());
                }
            }
        }, "DbSyncListener").start();
    }

    private MulticastSocket startMulticastReceiver() throws IOException {
        MulticastSocket receiver = new MulticastSocket(8888);
        InetAddress group = InetAddress.getByName(multicastGroupIp);
        receiver.joinGroup(group);

        new Thread(() -> {
            byte[] buf = new byte[256];
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    receiver.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("[Multicast] " + msg.trim());
                } catch (IOException e) {
                    if (running) System.err.println("[Multicast] Erro: " + e.getMessage());
                }
            }
        }, "Multicast-Receiver").start();

        return receiver;
    }

    private MulticastSocket startMulticastHeartbeat() throws IOException {
        MulticastSocket sender = new MulticastSocket();
        InetAddress group = InetAddress.getByName(MULTICAST_IP);

        String msg = "HEARTBEAT " + tcpClientPort;
        byte[] buf = msg.getBytes();

        new Thread(() -> {
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, group, MULTICAST_PORT);
                    sender.send(packet);
                    Thread.sleep(HEARTBEAT_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (running) System.err.println("[MulticastSender] Erro: " + e.getMessage());
                }
            }
        }, "Multicast-Sender").start();

        return sender;
    }

    private void sendUnregister() {
        try {
            String msg = "UNREGISTER " + tcpClientPort;
            byte[] buf = msg.getBytes();
            InetAddress dirAddr = InetAddress.getByName(directoryHost);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, dirAddr, directoryPort);
            udpSocket.send(packet);
            System.out.println("[Server] UNREGISTER enviado.");
        } catch (IOException e) {
            System.err.println("[Server] Falha ao enviar UNREGISTER: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (!running) return;
        running = false;

        System.out.println("\n[Server] A encerrar...");

        sendUnregister();

        if (heartbeatManager != null && heartbeatManager.isAlive()) {
            heartbeatManager.shutdown();
        }

        if (clientPool != null) {
            clientPool.shutdownNow();
        }

        if (directoryHeartbeatThread != null) {
            directoryHeartbeatThread.interrupt();
        }

        try { if (udpSocket != null) udpSocket.close(); } catch (Exception ignored) {}
        try { if (clientServerSocket != null) clientServerSocket.close(); } catch (Exception ignored) {}
        try { if (dbServerSocket != null) dbServerSocket.close(); } catch (Exception ignored) {}

        try {
            if (multicastReceiverSocket != null) {
                InetAddress group = InetAddress.getByName(multicastGroupIp);
                multicastReceiverSocket.leaveGroup(group);
                multicastReceiverSocket.close();
            }
        } catch (Exception ignored) {}
        try { if (multicastSenderSocket != null) multicastSenderSocket.close(); } catch (Exception ignored) {}

        System.out.println("[Server] Encerrado.");
    }
}