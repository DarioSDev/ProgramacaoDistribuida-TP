package pt.isec.pd.server;

import pt.isec.pd.common.MessageType; // Presumindo que esta classe existe
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerService {
    // Constantes
    private static final String MULTICAST_IP = "230.30.30.30";
    private static final int MULTICAST_PORT = 3030;
    private static final int HEARTBEAT_INTERVAL_MS = 5000;
    private static final int DIRECTORY_TIMEOUT_MS = 26000;

    // Configuração
    private final String directoryHost;
    private final int directoryPort;
    private final String multicastGroupIp;

    // Portos Dinâmicos
    private int tcpClientPort = 0;
    private int primaryTcpClientPort = -1;
    private int tcpDbPort = 0;
    private int udpPort = 0;

    // Sockets
    private DatagramSocket udpSocket;
    private ServerSocket clientServerSocket;
    private ServerSocket dbServerSocket;
    private MulticastSocket multicastReceiverSocket;
    private MulticastSocket multicastSenderSocket;

    // Estado
    private InetAddress primaryIp = null;
    private int primaryDbPort = -1;
    private boolean isPrimary = false;
    private volatile boolean running = true;

    // Serviços e Monitorização
    private HeartbeatSender heartbeatSender;
    private ExecutorService clientPool;
    private Thread directoryHeartbeatThread;

    public ServerService(String directoryHost, int directoryPort, String multicastGroupIp) {
        this.directoryHost = directoryHost;
        this.directoryPort = directoryPort;
        this.multicastGroupIp = multicastGroupIp;
    }

    public void start() {
        try {
            // 1. Inicializar Sockets Principais
            udpSocket = new DatagramSocket();
            udpPort = udpSocket.getLocalPort();

            clientServerSocket = new ServerSocket(0);
            dbServerSocket = new ServerSocket(0);
            tcpClientPort = clientServerSocket.getLocalPort();
            tcpDbPort = dbServerSocket.getLocalPort();

            System.out.printf("[Server] Portos: UDP=%d, Cliente=%d, BD=%d%n", udpPort, tcpClientPort, tcpDbPort);

            // 2. Registo
            if (!registerAndGetPrimary()) {
                System.err.println("[Server] Falha no registo com o Directory. A terminar.");
                return;
            }

            // 3. Inicializar serviços de rede
            // NOTA: A classe HeartbeatSender não foi fornecida, mas é assumida a sua existência e método start().
            heartbeatSender = new HeartbeatSender(udpSocket, directoryHost, directoryPort, tcpClientPort, HEARTBEAT_INTERVAL_MS);
            heartbeatSender.start();

            directoryHeartbeatThread = startDirectoryHeartbeatListener();
            startClientListener();
            startDbSyncListener();

            // 4. Inicializar Multicast (sockets dedicados)
            multicastReceiverSocket = startMulticastReceiver();
            multicastSenderSocket = startMulticastHeartbeat();

            System.out.println("[Server] Servidor iniciado.");
            if (isPrimary) {
                System.out.println("[Server] Este é o servidor PRINCIPAL.");
            } else {
                System.out.printf("[Server] Servidor backup. Primary: %s:%d (clientes) | BD: %d%n",
                        primaryIp.getHostAddress(), primaryTcpClientPort, primaryDbPort);
                downloadDatabaseFromPrimary();
            }

            // Bloco de espera
            new Scanner(System.in).nextLine();

        } catch (IOException e) {
            System.err.println("[Server] Erro fatal: " + e.getMessage());
        } finally {
            shutdown();
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

        String response = null;

        try {
            udpSocket.receive(recv);
            response = new String(recv.getData(), 0, recv.getLength()).trim();
            System.out.println("[Server] Confirmação do Directory: " + response);

            String[] parts = response.split("\\s+");

            // 1. Tratamento de Sucesso (PRIMARY)
            if (parts.length >= 4 && parts[0].equals("PRIMARY")) {
                primaryIp = InetAddress.getByName(parts[1]);
                primaryTcpClientPort = Integer.parseInt(parts[2]);
                primaryDbPort = Integer.parseInt(parts[3]);

                udpSocket.setSoTimeout(0); // Sucesso: Reset do timeout

                isPrimary = (primaryTcpClientPort == tcpClientPort);
                if (isPrimary) {
                    System.out.println("[Server] EU SOU O PRIMARY! Porto cliente: " + tcpClientPort);
                } else {
                    System.out.println("[Server] Servidor backup. Primary: " + primaryIp.getHostAddress() + ":" + primaryTcpClientPort);
                }
                return true; // Sucesso no registo

                // 2. Tratamento de Respostas de Erro (Adicionar esta lógica se o Directory enviar erros explícitos)
            } else if (parts[0].equals("ERRO") || parts[0].equals("FAIL")) {
                System.err.println("[Server] Registo Rejeitado pelo Directory: " + response);
                return false; // Falha de protocolo esperada

            } else {
                // Resposta desconhecida ou mal formatada
                throw new IllegalArgumentException("Resposta do Directory inesperada ou mal formatada.");
            }

        } catch (SocketTimeoutException e) {
            System.err.println("[Server] Timeout: Directory não respondeu.");
        } catch (IllegalArgumentException e) {
            // Captura o erro da nova exceção ou NumberFormatException/AddressException
            System.err.println("[Server] Erro no protocolo de registo: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[Server] Erro genérico ao processar confirmação: " + e.getMessage());
        }

        // Qualquer falha nos blocos try/catch leva ao retorno 'false'.
        return false;
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

                    // ⚠️ ALTERADO: Processar a resposta do Directory
                    if (msg.startsWith("PRIMARY")) {
                        processPrimaryUpdate(msg);
                    }
                    // Se o Directory enviar o seu próprio heartbeat genérico, mantém-se a lógica de timeout
                    else if (msg.equals("DIRECTORY_HEARTBEAT")) {
                        // lastDirectoryHeartbeat = System.currentTimeMillis();
                    }
                } catch (SocketTimeoutException e) {
                    System.err.println("[Server] Directory inativo por mais de " + (DIRECTORY_TIMEOUT_MS / 1000) + "s! Iniciando shutdown...");
                    shutdown();
                } catch (IOException e) {
                    if (running) System.err.println("[Server] Erro UDP: " + e.getMessage());
                }
            }
        }, "Dir-Heartbeat-Listener");

        listenerThread.start();
        return listenerThread;
    }

    // NOVO: Método auxiliar para processar a atualização do Primary
    private void processPrimaryUpdate(String response) {
        // Sincronização essencial para garantir que a atualização das variáveis de estado
        // (como isPrimary, primaryIp) é atómica, evitando race conditions com outras threads.
        synchronized (this) {
            try {
                String[] parts = response.split("\\s+");

                if (parts.length >= 4 && parts[0].equals("PRIMARY")) {
                    InetAddress newPrimaryIp = InetAddress.getByName(parts[1]);
                    int newPrimaryTcpPort = Integer.parseInt(parts[2]);
                    int newPrimaryDbPort = Integer.parseInt(parts[3]);

                    boolean wasPrimary = this.isPrimary;

                    // 1. Verificar se o próprio servidor foi promovido
                    if (newPrimaryTcpPort == tcpClientPort) {
                        this.isPrimary = true;
                    } else {
                        this.isPrimary = false;
                    }

                    // 2. Atualizar dados do Primary (mesmo que seja ele próprio)
                    this.primaryIp = newPrimaryIp;
                    this.primaryTcpClientPort = newPrimaryTcpPort;
                    this.primaryDbPort = newPrimaryDbPort;

                    // 3. Log de promoção (se a flag mudou)
                    if (this.isPrimary && !wasPrimary) {
                        System.out.println("🌟🌟 [PROMOÇÃO] ESTE SERVIDOR É O NOVO PRINCIPAL! 🌟🌟");
                        // A partir deste momento, a thread ClientListener aceitará novos clientes.
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
            while (running) {
                try {
                    Socket client = clientServerSocket.accept();
                    if (isPrimary) {
                        clientPool.submit(() -> handleClient(client));
                    } else {
                        rejectClient(client);
                    }
                } catch (SocketException e) {
                    if (running) System.err.println("[Server] Erro ao aceitar cliente: " + e.getMessage());
                } catch (IOException e) {
                    if (running) System.err.println("[Server] Erro ao aceitar cliente: " + e.getMessage());
                }
            }
        }, "ClientListener").start();
    }

    private void rejectClient(Socket client) {
        try (PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {
            out.println("ERRO: Este servidor não é o principal. Contacte o Directory novamente.");
            System.out.printf("[Server] Cliente rejeitado (não sou primary): %s%n", client.getRemoteSocketAddress());
        } catch (IOException ignored) {}
        finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    private void handleClient(Socket client) {
        BufferedReader in = null;
        PrintWriter out = null;

        try {
            // 1. Inicializar os streams (fora do try-with-resources)
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(client.getOutputStream(), true);

            System.out.println("[Server] Cliente conectado: " + client.getRemoteSocketAddress());

            // --- 1. Autenticação/Boas-Vindas ---
            out.println("Bem-vindo! (BD ainda não implementada)");

            String authMsg = in.readLine(); // Recebe o "CLIENT_AUTH_REQUEST"
            if (authMsg != null && authMsg.equals("CLIENT_AUTH_REQUEST")) {
                System.out.println("[Server] Cliente autenticado.");
                out.println("Mensagem recebida com sucesso! (Ligação Ativa)");
            } else {
                out.println("ERRO: Falha na autenticação.");
                return; // Sai e o finally fecha o socket
            }

            // --- 2. Loop de Sessão Persistente (A LIGAÇÃO MANTÉM-SE AQUI) ---
            String clientMsg;
            // O Servidor agora espera por dados do Cliente, mantendo a ligação aberta.
            while ((clientMsg = in.readLine()) != null) {
                if (clientMsg.trim().isEmpty()) continue;

                System.out.println("[Server] Recebido do cliente (Sessão Ativa): " + clientMsg);

                // Lógica de processamento e resposta
                out.println("Comando processado: " + clientMsg);
            }

            // Se o loop termina (in.readLine() == null), o cliente fechou a ligação
            System.out.println("[Server] Cliente fechou ligação: " + client.getRemoteSocketAddress());

        } catch (IOException e) {
            System.err.println("[Server] Erro na sessão com cliente: " + client.getRemoteSocketAddress() + " | " + e.getMessage());
        } finally {
            // ⚠️ Fecha os streams E o socket de forma segura.
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
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
                    if (running) System.err.println("[Server] Erro sync BD: " + e.getMessage());
                }
            }
        }, "DbSyncListener").start();
    }

    private void sendDatabase(Socket peer) {
        File dbFile = new File("events.db");
        if (!dbFile.exists()) {
            System.out.println("[Server] BD não existe. Criando vazia...");
            try (var conn = java.sql.DriverManager.getConnection("jdbc:sqlite:events.db")) {
                var stmt = conn.createStatement();
                stmt.execute("CREATE TABLE IF NOT EXISTS events (id INTEGER PRIMARY KEY, name TEXT)");
            } catch (Exception e) {
                System.err.println("Erro ao criar BD: " + e.getMessage());
                return;
            }
        }

        try (FileInputStream fis = new FileInputStream(dbFile);
             OutputStream out = peer.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            System.err.println("[Server] Erro ao enviar BD: " + e.getMessage());
        }
    }

    private void downloadDatabaseFromPrimary() {
        System.out.println("[Server] Sincronizando BD com principal...");
        try (Socket socket = new Socket(primaryIp, primaryDbPort);
             InputStream in = socket.getInputStream();
             FileOutputStream fos = new FileOutputStream("events.db")) {

            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            System.out.println("[Server] BD sincronizada.");
        } catch (IOException e) {
            System.err.println("[Server] Falha na sincronização: " + e.getMessage());
        }
    }

    // 🌟 MÉTODO CORRIGIDO/COMPLETO
    private MulticastSocket startMulticastReceiver() throws IOException {
        MulticastSocket receiver = new MulticastSocket(8888); // Porta 8888 para receção
        InetAddress group = InetAddress.getByName(multicastGroupIp);
        receiver.joinGroup(group);

        new Thread(() -> {
            byte[] buf = new byte[256];
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    receiver.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("[Multicast Receiver] " + msg.trim());
                } catch (IOException e) {
                    if (running) System.err.println("[Multicast Receiver] Erro: " + e.getMessage());
                }
            }
        }, "Multicast-Receiver").start();

        return receiver;
    }

    // 🌟 MÉTODO CORRIGIDO/COMPLETO
    private MulticastSocket startMulticastHeartbeat() throws IOException {
        MulticastSocket sender = new MulticastSocket(); // Usa porta dinâmica para envio
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
                    if (running) System.err.println("[Multicast Sender] Erro: " + e.getMessage());
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
            System.out.println("[Server] UNREGISTER enviado ao Directory.");
        } catch (IOException e) {
            System.err.println("[Server] Falha ao enviar UNREGISTER: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (!running) return;
        running = false;

        System.out.println("\n[Server] Iniciando encerramento gracioso...");

        // 1. Enviar UNREGISTER e interromper Heartbeat UDP
        sendUnregister();
        if (heartbeatSender != null && heartbeatSender.isAlive()) {
            heartbeatSender.interrupt();
        }

        // 2. Encerrar ExecutorService de Clientes (Gracioso)
        if (clientPool != null) {
            clientPool.shutdown();
            try {
                if (!clientPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    clientPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                clientPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 3. Interromper Threads de Listeners (UDP Dir)
        if (directoryHeartbeatThread != null) {
            directoryHeartbeatThread.interrupt();
        }

        // 4. Fechar Sockets
        try { if (udpSocket != null) udpSocket.close(); } catch (Exception ignored) {}
        try { if (clientServerSocket != null) clientServerSocket.close(); } catch (Exception ignored) {}
        try { if (dbServerSocket != null) dbServerSocket.close(); } catch (Exception ignored) {}

        // Fechar sockets Multicast
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