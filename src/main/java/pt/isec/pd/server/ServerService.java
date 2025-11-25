package pt.isec.pd.server;

import pt.isec.pd.common.Question;
import pt.isec.pd.common.Student;
import pt.isec.pd.common.Teacher;
import pt.isec.pd.common.User;
import pt.isec.pd.db.DatabaseManager;
import pt.isec.pd.db.QueryPerformer;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
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
    private final String dbDirectoryPath;

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

    private HeartbeatSender heartbeatSender;
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

            heartbeatSender = new HeartbeatSender(udpSocket, directoryHost, directoryPort, tcpClientPort, HEARTBEAT_INTERVAL_MS);
            heartbeatSender.start();

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
        File dir = new File(dbDirectoryPath);
        if (!dir.exists()) dir.mkdirs();

        if (isPrimary) {
            File[] dbFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".db"));
            File selectedFile;

            if (dbFiles == null || dbFiles.length == 0) {
                System.out.println("[Server] Nenhuma BD encontrada. Criando nova versão 0...");
                selectedFile = new File(dir, "data_v0.db");
            } else {
                Arrays.sort(dbFiles, Comparator.comparingLong(File::lastModified));
                selectedFile = dbFiles[dbFiles.length - 1];
                System.out.println("[Server] Usando BD mais recente: " + selectedFile.getName());
            }

            this.dbManager = new DatabaseManager(dbDirectoryPath, selectedFile.getName());
            this.dbManager.createSchema();
            this.queryPerformer = new QueryPerformer(dbManager);

        } else {
            System.out.println("[Server] Backup mode. A aguardar sincronização...");

            if (!downloadDatabaseFromPrimary()) {
                System.err.println("[Server] Falha crítica na sincronização. A encerrar.");
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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String newFileName = "data_backup_" + sdf.format(new Date()) + ".db";
        File syncedFile = new File(dbDirectoryPath, newFileName);

        try (Socket socket = new Socket(primaryIp, primaryDbPort);
             InputStream in = socket.getInputStream();
             FileOutputStream fos = new FileOutputStream(syncedFile)) {

            socket.setSoTimeout(5000);

            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }

            System.out.println("[Server] Download concluído.");
            this.dbManager = new DatabaseManager(dbDirectoryPath, newFileName);
            System.out.println("[Server] DatabaseManager inicializado com versão: " + dbManager.getDbVersion());
            return true;

        } catch (IOException e) {
            System.err.println("[Server] Erro sync: " + e.getMessage());
            if (syncedFile.exists()) syncedFile.delete();
            return false;
        }
    }

    private void sendDatabase(Socket peer) {
        if (dbManager == null || !dbManager.getDbFile().exists()) return;

        dbManager.getReadLock().lock();
        try {
            System.out.println("[Server] A enviar BD...");
            try (FileInputStream fis = new FileInputStream(dbManager.getDbFile());
                 OutputStream out = peer.getOutputStream()) {

                byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            System.out.println("[Server] Envio concluído.");
        } catch (IOException e) {
            System.err.println("[Server] Erro ao enviar: " + e.getMessage());
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
        try (PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {
            out.println("ERRO: Server is Backup mode. Connect to Primary.");
        } catch (IOException ignored) {}
        finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    private void handleClient(Socket client) {
        BufferedReader in = null;
        PrintWriter out = null;

        try {
            // Configurar streams de texto
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(client.getOutputStream(), true);

            System.out.println("[Server] Cliente conectado: " + client.getRemoteSocketAddress());

            // 1. Handshake Inicial (se necessário, ou mensagem de boas-vindas)
            // Se o cliente espera ler algo logo ao conectar:
            out.println("BEM-VINDO AO SERVIDOR PD");

            // 2. Loop de Processamento
            String msg;
            while ((msg = in.readLine()) != null) {
                msg = msg.trim();
                if (msg.isEmpty()) continue;

                System.out.println("[Server] Recebido: " + msg);

                // --- PROCESSAMENTO DE COMANDOS ---
                String[] parts = msg.split(";"); // Usamos ';' como separador para permitir espaços nos nomes
                String command = parts[0].toUpperCase();

                String response = "ERRO: Comando desconhecido ou invalido";

                try {
                    switch (command) {
                        case "LOGIN":
                            // Formato: LOGIN;email;password
                            if (parts.length == 3) {
                                String email = parts[1];
                                String pass = parts[2];

                                if (queryPerformer.authenticate(email, pass)) {
                                    response = "OK";
                                } else {
                                    response = "NOK";
                                }
                            }
                            break;

                        case "REGISTER_STUDENT":
                            // Formato: REGISTER_STUDENT;nome;email;pass;numero_estudante
                            if (parts.length == 5) {
                                String name = parts[1];
                                String email = parts[2];
                                String pass = parts[3];
                                String idNumber = parts[4];

                                Student s = new Student(name, email, pass, idNumber);
                                if (queryPerformer.registerUser(s)) {
                                    response = "REGISTER_SUCCESS";
                                } else {
                                    response = "REGISTER_FAILED";
                                }
                            }
                            break;

                        case "REGISTER_TEACHER":
                            // Formato: REGISTER_TEACHER;nome;email;pass
                            if (parts.length == 4) {
                                String name = parts[1];
                                String email = parts[2];
                                String pass = parts[3];

                                Teacher t = new Teacher(name, email, pass);
                                if (queryPerformer.registerUser(t)) {
                                    response = "REGISTER_SUCCESS";
                                } else {
                                    response = "REGISTER_FAILED";
                                }
                            }
                            break;

                        case "GET_USER_INFO":
                            // Formato: GET_USER_INFO;email
                            if (parts.length == 2) {
                                String email = parts[1];
                                User u = queryPerformer.getUser(email);
                                if (u != null) {
                                    // Retorna representação simples. Ex: "USER_INFO;Nome;Email"
                                    response = "USER_INFO;" + u.getName() + ";" + u.getEmail();
                                } else {
                                    response = "USER_NOT_FOUND";
                                }
                            }
                            break;

                        case "CREATE_QUESTION":
                            // Exemplo mais complexo.
                            // Formato: CREATE_QUESTION;pergunta;opcaoCorrecta;op1,op2,op3...
                            if (parts.length >= 4) {
                                String text = parts[1];
                                String correct = parts[2];
                                // Assumindo que as opções vêm separadas por vírgula no 4º campo
                                String[] options = parts[3].split(",");

                                // Precisamos do ID do professor que criou (poderia vir no comando)
                                // Aqui uso um ID dummy ou tens de passar o email do docente logado
                                Question q = new Question(text, correct, options, null, null, "DOCENTE_ID");

                                if (queryPerformer.saveQuestion(q)) {
                                    response = "QUESTION_SAVED";
                                } else {
                                    response = "QUESTION_ERROR";
                                }
                            }
                            break;

                        case "LOGOUT":
                            response = "BYE";
                            break;

                        default:
                            response = "UNKNOWN_COMMAND";
                    }
                } catch (Exception e) {
                    response = "SERVER_ERROR: " + e.getMessage();
                    e.printStackTrace();
                }

                // Envia resposta ao cliente
                out.println(response);

                // Se for LOGOUT, podemos fechar o ciclo
                if ("BYE".equals(response)) {
                    break;
                }
            }

        } catch (IOException e) {
            System.err.println("[Server] Erro na conexão com cliente: " + e.getMessage());
        } finally {
            // Fechar recursos
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    private void processClientUpdate(String sqlCommand) {
        if (dbManager != null) {
            dbManager.executeUpdate(sqlCommand);
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

        if (heartbeatSender != null && heartbeatSender.isAlive()) {
            heartbeatSender.shutdown();
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