package pt.isec.pd.server;
import pt.isec.pd.client.ClientAPI;
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
    private static final int NO_AUTH_TIMEOUT_MS = 60000;

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

    private final Map < Socket, ObjectOutputStream > clientOutputStreams = new ConcurrentHashMap < > ();

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
                    primaryIp,
                    primaryTcpClientPort,
                    primaryDbPort,
                    this);

            heartbeatManager.start();
            this.dbManager.setHeartbeatManager(this.heartbeatManager);
            this.dbManager.setConnectedClients(new ArrayList < > (clientOutputStreams.values()));
            directoryHeartbeatThread = startDirectoryHeartbeatListener();
            startClientListener();
            startDbSyncListener();
            multicastReceiverSocket = startMulticastReceiver();

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

    public int getPrimaryClientTcpPort() {
        return primaryTcpClientPort;
    }

    public void initiateShutdown() {
        this.shutdown();
    }

    private void initializeDatabaseLogic() {
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
            if (dbFiles == null || dbFiles.length == 0) { // [R33] create
                String newFileName = "data_" + this.tcpClientPort + "_" + System.currentTimeMillis() + ".db"; // [R29]
                System.out.println("[DB-INIT][PRIMARY] Nenhuma BD encontrada. Criando nova: " + newFileName);
                selectedFile = new File(dir, newFileName);
            } else {
                // [R33] reuse newer bd
                System.out.println("[DB-INIT][PRIMARY] BDs encontradas:");
                for (File f: dbFiles)
                    System.out.println(" → " + f.getName() + " (size=" + f.length() + " bytes)");
                // [REQUISITO] Usa a BD com data de alteração mais recente.
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
            if (!downloadDatabaseFromPrimary()) { // [R30]
                // [REQUISITO] Se falhar, termina informando o diretório.
                System.err.println("[DB-INIT][BACKUP] ERRO: Falha ao receber base de dados. A iniciar shutdown.");
                shutdown(); // [R30]
                System.exit(1);
            }
        }
    }
    // [R27]
    private boolean registerAndGetPrimary() throws IOException {
        udpSocket.setSoTimeout(DIRECTORY_TIMEOUT_MS); // [R28]
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
            // [R27]
            if (parts.length >= 4 && parts[0].equals("PRIMARY")) {
                primaryIp = InetAddress.getByName(parts[1]);
                primaryTcpClientPort = Integer.parseInt(parts[2]);
                primaryDbPort = Integer.parseInt(parts[3]);
                udpSocket.setSoTimeout(0);
                // [R27]
                isPrimary = (primaryTcpClientPort == tcpClientPort);
                if (isPrimary) {
                    System.out.println("[Server] EU SOU O PRIMARY!");
                } else {
                    System.out.println("[Server] Servidor backup definido.");
                }
                return true;
            }
            return false;
            // [R28]
        } catch (SocketTimeoutException e) {
            System.err.println("[Server] Timeout: Directory não respondeu. A terminar.");
            return false;
        }
    }

    private boolean downloadDatabaseFromPrimary() {
        String newFileName = "data_" + this.tcpClientPort + "_" + System.currentTimeMillis() + ".db";
        File syncedFile = new File(dbDirectoryPath, newFileName);
        System.out.println("[SYNC][BACKUP] A tentar ligação ao primary para download:");
        System.out.println(" IP = " + primaryIp);
        System.out.println(" PORT = " + primaryDbPort);
        System.out.println(" File destino = " + syncedFile.getAbsolutePath());
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
            System.out.println(" version = " + dbManager.getDbVersion());
            return true;
        } catch (IOException e) {
            System.err.println("[SYNC][BACKUP] ERRO: " + e.getMessage());
            if (syncedFile.exists()) {
                System.err.println("[SYNC][BACKUP] Apagando ficheiro incompleto: " + syncedFile.getAbsolutePath());
                syncedFile.delete();
            }
            // [R30]
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
        System.out.println(" Ficheiro = " + dbFile.getAbsolutePath());
        System.out.println(" Tamanho = " + dbFile.length() + " bytes");
        if (!dbManager.isSchemaReady()) {
            System.err.println("[SYNC][PRIMARY] Schema ainda não está pronto! NÃO envio BD.");
            return;
        }
        // [R1]
        dbManager.getWriteLock().lock();
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
            dbManager.getWriteLock().unlock();
            // R1
        }
    }

    private Thread startDirectoryHeartbeatListener() {
        // [R6]
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
        synchronized(this) {
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
                    // AÇÃO CRÍTICA: Propagar a informação atualizada ao HeartbeatManager
                    if (this.heartbeatManager != null) {
                        this.heartbeatManager.updatePrimary(newPrimaryIp, newPrimaryTcpPort, newPrimaryDbPort);
                    }
                    if (this.isPrimary && !wasPrimary) {
                        System.out.println("[Server] PROMOVIDO A PRINCIPAL!");
                        if (this.dbManager != null) {
                            this.queryPerformer = new QueryPerformer(dbManager);
                            System.out.println("[Server] QueryPerformer inicializado para modo PRINCIPAL.");
                        } else {
                            System.err.println("[Server] ERRO FATAL: dbManager é nulo após promoção.");
                            initiateShutdown();
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[Server] Erro ao processar atualização do Primary: " + e.getMessage());
            }
        }
    }

    // [R3]
    private void startClientListener() {
        clientPool = Executors.newFixedThreadPool(10);
        new Thread(() -> {
            // [REQUISITO] Todos os servidores aguardam conexões de clientes
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
        } catch (IOException ignored) {} finally {
            try {
                if (out != null) out.close();
            } catch (IOException ignored) {}
            try {
                client.close();
            } catch (IOException ignored) {}
        }
    }

    // [R4]
    private void handleClient(Socket client) {
        ObjectInputStream in = null;
        ObjectOutputStream out = null;
        try {
            // [R10]
            client.setSoTimeout(NO_AUTH_TIMEOUT_MS);
            out = new ObjectOutputStream(client.getOutputStream());
            out.flush();
            in = new ObjectInputStream(client.getInputStream());
            System.out.println("[Server] Cliente conectado: " + client.getRemoteSocketAddress());
            out.writeObject(new Message(Command.CONNECTION, "BEM-VINDO AO SERVIDOR PD"));
            out.flush();
            // 2. Tenta ler a primeira mensagem (LOGIN ou REGISTER).
            // [R10] ENDS
            Object obj = in.readObject();
            // 3. Se a primeira leitura foi bem-sucedida, remove o timeout para o resto da sessão
            client.setSoTimeout(0);
            // Variável para a mensagem lida
            Message msg;
            // 4. Inicia o loop de processamento (processa a primeira mensagem e depois continua a ler)
            if (!(obj instanceof Message)) {
                System.out.println("[Server] Recebido objeto não-Message como primeira mensagem: " + obj);
                return;
            }
            msg = (Message) obj;
            boolean isFirstMessage = true;
            do {
                // Se não for a primeira mensagem, tenta ler a próxima
                if (!isFirstMessage) {
                    obj = in.readObject();
                    if (obj == null) break;
                    if (!(obj instanceof Message)) {
                        System.out.println("[Server] Recebido objeto não-Message: " + obj);
                        continue;
                    }
                    msg = (Message) obj;
                } else {
                    // Se for a primeira mensagem, já está em 'msg'
                    isFirstMessage = false;
                }
                System.out.println("[Server] Recebido: " + msg);
                Command cmd = msg.getCommand();
                Object data = msg.getData();
                Message responseMsg = null;
                try {
                    switch (cmd) {
                        // [R15]
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
                        // [R13]
                        // [R14]
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

                        // [R18]
                        case CREATE_QUESTION -> {
                            if (data instanceof Question q) {
                                boolean ok = queryPerformer.saveQuestion(q);
                                responseMsg = new Message(Command.CREATE_QUESTION, ok);
                            } else {
                                responseMsg = new Message(Command.CREATE_QUESTION, false);
                            }
                        }
                        // [R17]
                        case LOGOUT -> responseMsg = new Message(Command.LOGOUT, "BYE");
                        case VALIDATE_QUESTION_CODE -> {
                            if (msg.getData() instanceof String code) {
                                String result = queryPerformer.validateQuestionCode(code);
                                System.out.println("[Server] Pedido de validação do código: " + code + " → " + result);
                                out.writeObject(new Message(Command.VALIDATE_QUESTION_CODE, result));
                                out.flush();
                            }
                        }
                        // [R24] TODO APENAS PODE REQUISITAR A PERGUNTA DENTRO DO PRAZO
                        // [R25] TODO APENAS PODE REQUISITAR A PERGUNTA DENTRO DO PRAZO
                        case GET_QUESTION -> {
                            if (msg.getData() instanceof String code) {
                                System.out.println("[Server] Pedido de dados da pergunta: " + code);
                                Question q = queryPerformer.getQuestionByCode(code);
                                out.writeObject(new Message(Command.GET_QUESTION, q));
                                out.flush();
                            }
                        }
                        case SUBMIT_ANSWER -> {
                            if (msg.getData() instanceof Object[] arr && arr.length == 3) {
                                String email = (String) arr[0];
                                String code = (String) arr[1];
                                int index = (int) arr[2];
                                boolean success = queryPerformer.submitAnswer(email, code, index);
                                out.writeObject(new Message(Command.SUBMIT_ANSWER, success));
                                out.flush();
                            } else {
                                out.writeObject(new Message(Command.SUBMIT_ANSWER, false));
                                out.flush();
                            }
                        }
                        // [R19] TODO APENAS EDITAR SEM RESPOSTAS ASSOCIADAS
                        case EDIT_QUESTION -> {
                            if (msg.getData() instanceof Question q) {
                                String questionCode = q.getId();

                                // 1. Verificar se há respostas (Regra de Negócio)
                                if (queryPerformer.hasSubmittedAnswers(questionCode)) {
                                    System.out.println("[Server] Edição bloqueada: Respostas encontradas para " + questionCode);
                                    // Retorna uma mensagem de erro ou false com um código de erro
                                    responseMsg = new Message(Command.EDIT_QUESTION, "ANSWERS_EXIST");
                                } else {
                                    System.out.println("[Server] A editar pergunta: " + questionCode);
                                    // 2. Executa a edição (o QueryPerformer trata da transação/replicação)
                                    boolean success = queryPerformer.editQuestion(q);
                                    responseMsg = new Message(Command.EDIT_QUESTION, success);
                                }
                            } else {
                                responseMsg = new Message(Command.EDIT_QUESTION, false);
                            }
                        }
                        // [R20] TODO APENAS DELETE SEM RESPOSTAS ASSOCIADAS
                        case DELETE_QUESTION -> {
                            if (msg.getData() instanceof String code) {

                                // 1. Verificar se há respostas (Regra de Negócio)
                                if (queryPerformer.hasSubmittedAnswers(code)) {
                                    System.out.println("[Server] Eliminação bloqueada: Respostas encontradas para " + code);
                                    // Retorna uma mensagem de erro ou false com um código de erro
                                    responseMsg = new Message(Command.DELETE_QUESTION, "ANSWERS_EXIST");
                                } else {
                                    System.out.println("[Server] A apagar pergunta: " + code);
                                    // 2. Executa a eliminação
                                    boolean success = queryPerformer.deleteQuestion(code);
                                    responseMsg = new Message(Command.DELETE_QUESTION, success);
                                }
                            } else {
                                responseMsg = new Message(Command.DELETE_QUESTION, false);
                            }
                        }
                        // [R21]
                        case GET_TEACHER_QUESTIONS -> {
                            if (msg.getData() instanceof String email) {
                                List < Question > list = queryPerformer.getTeacherQuestions(email);
                                out.writeObject(new Message(Command.GET_TEACHER_QUESTIONS, new ArrayList < > (list)));
                                out.flush();
                            }
                        }
                        // [R26] TODO APENAS DEVE CONSEGUIR CONSULTAR AS PERGUNTAS EXPIRADAS
                        case GET_STUDENT_HISTORY -> {
                            if (msg.getData() instanceof String email) {
                                StudentHistory history = queryPerformer.getStudentHistory(email);
                                out.writeObject(new Message(Command.GET_STUDENT_HISTORY, history));
                                out.flush();
                            }
                        }
                        // [R22]
                        case GET_QUESTION_RESULTS -> {
                            if (msg.getData() instanceof String code) {
                                TeacherResultsData results = queryPerformer.getQuestionResults(code);
                                out.writeObject(new Message(Command.GET_QUESTION_RESULTS, results));
                                out.flush();
                            }
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
            } while (true); // Loop infinito, interrompido por break/return
        } catch (SocketTimeoutException e) {
            System.err.println("[Server] Cliente excedeu " + NO_AUTH_TIMEOUT_MS + "ms para enviar credenciais. Fechando ligação.");
        } catch (EOFException e) {
            System.out.println("[Server] Cliente fechou a ligação: " + client.getRemoteSocketAddress());
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Server] Erro na conexão com cliente: " + e.getMessage());
        } finally {
            // Fechar recursos
            try {
                if (in != null) in.close();
            } catch (IOException ignored) {}
            try {
                if (out != null) out.close();
            } catch (IOException ignored) {}
            try {
                client.close();
            } catch (IOException ignored) {}
        }
    }

    private void startDbSyncListener() {
        // [R5]
        new Thread(() -> {
            while (running) {
                try {
                    Socket peer = dbServerSocket.accept();
                    // [R5]
                    new Thread(() -> {
                        try {
                            sendDatabase(peer);
                        } finally {
                            try { peer.close(); } catch (IOException ignored) {}
                        }
                    }, "DbTransfer-Thread").start();
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
        try {
            if (udpSocket != null) udpSocket.close();
        } catch (Exception ignored) {}
        try {
            if (clientServerSocket != null) clientServerSocket.close();
        } catch (Exception ignored) {}
        try {
            if (dbServerSocket != null) dbServerSocket.close();
        } catch (Exception ignored) {}
        try {
            if (multicastReceiverSocket != null) {
                InetAddress group = InetAddress.getByName(multicastGroupIp);
                multicastReceiverSocket.leaveGroup(group);
                multicastReceiverSocket.close();
            }
        } catch (Exception ignored) {}
        try {
            if (multicastSenderSocket != null) multicastSenderSocket.close();
        } catch (Exception ignored) {}
        System.out.println("[Server] Encerrado.");
    }
}