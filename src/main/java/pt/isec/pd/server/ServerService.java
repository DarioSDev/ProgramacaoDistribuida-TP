// pt.isec.pd.server.ServerService
package pt.isec.pd.server;

import pt.isec.pd.common.MessageType;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerService {
    private static final String MULTICAST_IP = "230.30.30.30";
    private static final int MULTICAST_PORT = 3030;
    private static final int HEARTBEAT_INTERVAL_MS = 5000;
    private static final int DIRECTORY_TIMEOUT_MS = 26000;

    private final String directoryHost;
    private final int directoryPort;
    private final String multicastGroupIp;

    private int tcpClientPort = 0;
    private int primaryTcpClientPort = -1;  // ← NOVO: porto TCP do primary
    private int tcpDbPort = 0;
    private int udpPort = 0;

    private DatagramSocket udpSocket;
    private ServerSocket clientServerSocket;
    private ServerSocket dbServerSocket;
    private MulticastSocket multicastSocket;

    private InetAddress primaryIp = null;
    private int primaryDbPort = -1;
    private boolean isPrimary = false;
    private boolean running = true;

    private HeartbeatSender heartbeatSender;
    private long lastDirectoryHeartbeat = System.currentTimeMillis();

    public ServerService(String directoryHost, int directoryPort, String multicastGroupIp) {
        this.directoryHost = directoryHost;
        this.directoryPort = directoryPort;
        this.multicastGroupIp = multicastGroupIp;
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

            heartbeatSender = new HeartbeatSender(udpSocket, directoryHost, directoryPort, tcpClientPort, HEARTBEAT_INTERVAL_MS);
            heartbeatSender.start();
            startDirectoryHeartbeatListener();
            startClientListener();       // ← Agora REJEITA se não for primary
            startDbSyncListener();
            startMulticastReceiver();
            startMulticastHeartbeat();

            System.out.println("[Server] Servidor iniciado.");
            if (isPrimary) {
                System.out.println("[Server] Este é o servidor PRINCIPAL.");
            } else {
                System.out.printf("[Server] Servidor backup. Primary: %s:%d (clientes) | BD: %d%n",
                        primaryIp.getHostAddress(), primaryTcpClientPort, primaryDbPort);
                downloadDatabaseFromPrimary();
            }

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
        try {
            udpSocket.receive(recv);
            String response = new String(recv.getData(), 0, recv.getLength()).trim();
            System.out.println("[Server] Confirmação do Directory: " + response);

            String[] parts = response.split("\\s+");
            if (parts.length >= 4 && parts[0].equals("PRIMARY")) {
                primaryIp = InetAddress.getByName(parts[1]);
                primaryTcpClientPort = Integer.parseInt(parts[2]);
                primaryDbPort = Integer.parseInt(parts[3]);

                isPrimary = (primaryTcpClientPort == tcpClientPort);
                if (isPrimary) {
                    System.out.println("[Server] EU SOU O PRIMARY! Porto cliente: " + tcpClientPort);
                    return true;
                } else {
                    System.out.println("[Server] Servidor backup. Primary: " + primaryIp.getHostAddress() + ":" + primaryTcpClientPort);
                    return false;
                }
            }
        } catch (SocketTimeoutException e) {
            System.err.println("[Server] Timeout: Directory não respondeu.");
        } catch (Exception e) {
            System.err.println("[Server] Erro ao processar confirmação: " + e.getMessage());
        }
        return false;
    }

    private void startDirectoryHeartbeatListener() {
        new Thread(() -> {
            byte[] buf = new byte[256];
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    udpSocket.setSoTimeout(DIRECTORY_TIMEOUT_MS);
                    udpSocket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength()).trim();

                    if ("DIRECTORY_HEARTBEAT".equals(msg)) {
                        lastDirectoryHeartbeat = System.currentTimeMillis();
                    }
                } catch (SocketTimeoutException e) {
                    System.err.println("[Server] Directory inativo por mais de 26s! Encerrando...");
                    shutdown();
                    System.exit(0);
                } catch (IOException e) {
                    if (running) System.err.println("[Server] Erro UDP: " + e.getMessage());
                }
            }
        }, "Dir-Heartbeat-Listener").start();
    }

    private void startClientListener() {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        new Thread(() -> {
            while (running) {
                try {
                    Socket client = clientServerSocket.accept();
                    if (isPrimary) {
                        pool.submit(() -> handleClient(client));
                    } else {
                        rejectClient(client);
                    }
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
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {

            System.out.println("[Server] Cliente conectado: " + client.getRemoteSocketAddress());

            out.println("Bem-vindo! (BD ainda não implementada)");

            String clientMsg = in.readLine();
            if (clientMsg != null && !clientMsg.isEmpty()) {
                System.out.println("[Server] Recebido do cliente: " + clientMsg);
                out.println("Mensagem recebida com sucesso!");
            } else {
                System.out.println("[Server] Cliente não enviou mensagem ou fechou ligação.");
                out.println("ERRO: Mensagem vazia.");
            }

        } catch (IOException e) {
            System.err.println("[Server] Erro ao lidar com cliente: " + e.getMessage());
        } finally {
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

    private void startMulticastReceiver() {
        try {
            multicastSocket = new MulticastSocket(8888);
            InetAddress group = InetAddress.getByName(multicastGroupIp);
            multicastSocket.joinGroup(group);

            new Thread(() -> {
                byte[] buf = new byte[256];
                while (running) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        multicastSocket.receive(packet);
                        String msg = new String(packet.getData(), 0, packet.getLength());
                        System.out.println("[Multicast] " + msg);
                    } catch (IOException e) {
                        if (running) System.err.println("[Multicast] Erro: " + e.getMessage());
                    }
                }
            }, "Multicast").start();
        } catch (IOException e) {
            System.err.println("[Server] Erro multicast: " + e.getMessage());
        }
    }

    private void startMulticastHeartbeat() {
        new Thread(() -> {
            try {
                multicastSocket = new MulticastSocket();
                InetAddress group = InetAddress.getByName(MULTICAST_IP);

                String msg = "HEARTBEAT " + tcpClientPort;
                byte[] buf = msg.getBytes();

                while (running) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, group, MULTICAST_PORT);
                    multicastSocket.send(packet);
                    Thread.sleep(HEARTBEAT_INTERVAL_MS);
                }
            } catch (Exception e) {
                if (running) System.err.println("[Server] Erro no multicast: " + e.getMessage());
            }
        }, "Multicast-Heartbeat").start();
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
        running = false;
        sendUnregister();
        if (heartbeatSender != null && heartbeatSender.isAlive()) {
            heartbeatSender.interrupt(); // para sair do sleep
        }
        try { if (udpSocket != null) udpSocket.close(); } catch (Exception ignored) {}
        try { if (clientServerSocket != null) clientServerSocket.close(); } catch (Exception ignored) {}
        try { if (dbServerSocket != null) dbServerSocket.close(); } catch (Exception ignored) {}
        try { if (multicastSocket != null) multicastSocket.close(); } catch (Exception ignored) {}
        System.out.println("[Server] Encerrado.");
    }
}