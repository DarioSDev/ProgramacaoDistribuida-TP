package pt.isec.pd.server;

import pt.isec.pd.db.DatabaseManager;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class HeartbeatManager extends Thread {

    private static final String MULTICAST_IP = "230.30.30.30";
    private static final int MULTICAST_PORT = 3030;

    private final DatagramSocket udpSocket;
    private final String directoryHost;
    private final int directoryPort;

    private final int selfClientTcpPort;
    private final int selfDbTcpPort;

    private final DatabaseManager dbManager;
    private final int heartbeatInterval;

    private final ServerService serverService;

    private volatile boolean running = true;
    private MulticastSocket multicastReceiver;

    public HeartbeatManager(DatagramSocket udpSocket,
                            String directoryHost,
                            int directoryPort,
                            int selfClientTcpPort,
                            int selfDbTcpPort,
                            DatabaseManager dbManager,
                            int heartbeatInterval,
                            ServerService serverService) {

        this.udpSocket = udpSocket;
        this.directoryHost = directoryHost;
        this.directoryPort = directoryPort;
        this.selfClientTcpPort = selfClientTcpPort;
        this.selfDbTcpPort = selfDbTcpPort;
        this.dbManager = dbManager;
        this.heartbeatInterval = heartbeatInterval;
        this.serverService = serverService;

        setName("HeartbeatManager-Main");
        setDaemon(true);
    }

    @Override
    public void start() {
        super.start();                    // Inicia o sender (run())
        startMulticastReceiver();         // Inicia o receiver em thread separada
    }

    // ====================== SENDER ======================
    @Override
    public void run() {
        while (running) {
            try {
                sendHeartbeat();  // heartbeat normal (sem query)
                Thread.sleep(heartbeatInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                if (running)
                    System.err.println("[HB Sender] Erro: " + e.getMessage());
            }
        }
    }

    // Envia heartbeat normal (sem query SQL)
    public void sendHeartbeat() {
        int version = (dbManager != null) ? dbManager.getDbVersion() : 0;
        String msg = String.format("HEARTBEAT %d %d %d",
                selfClientTcpPort, selfDbTcpPort, version);

        sendMulticast(msg);
        sendToDirectory(msg);
    }

    // Envia heartbeat com query SQL (chamado pelo QueryPerformer após alterações)
    public void sendHeartbeatWithQuery(String sqlQuery) {
        if (!serverService.isPrimary()) return; // só o primary envia queries

        int version = (dbManager != null) ? dbManager.getDbVersion() : 0;
        String msg = String.format("HEARTBEAT %d %d %d %s",
                selfClientTcpPort, selfDbTcpPort, version, sqlQuery);

        sendMulticast(msg);
        // Não envia para o directory com query (não é necessário)
    }

    private void sendMulticast(String message) {
        try {
            InetAddress group = InetAddress.getByName(MULTICAST_IP);
            byte[] data = message.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, group, MULTICAST_PORT);
            udpSocket.send(packet);
        } catch (IOException e) {
            System.out.println("[HB] Falha ao enviar multicast: " + e.getMessage());
        }
    }

    private void sendToDirectory(String message) {
        try {
            InetAddress dirAddr = InetAddress.getByName(directoryHost);
            byte[] data = message.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, dirAddr, directoryPort);
            udpSocket.send(packet);
        } catch (IOException ignored) {}
    }

    // ====================== RECEIVER ======================
    private void startMulticastReceiver() {
        new Thread(() -> {
            try {
                multicastReceiver = new MulticastSocket(MULTICAST_PORT);
                multicastReceiver.setReuseAddress(true);
                InetAddress group = InetAddress.getByName(MULTICAST_IP);
                multicastReceiver.joinGroup(group);

                System.out.println("[HB Receiver] A ouvir heartbeats em " + MULTICAST_IP + ":" + MULTICAST_PORT);

                byte[] buffer = new byte[4096];
                while (running) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    multicastReceiver.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
                    processHeartbeat(message, packet.getAddress());
                }
            } catch (Exception e) {
                if (running)
                    System.err.println("[HB Receiver] Erro: " + e.getMessage());
            }
        }, "Heartbeat-Receiver").start();
    }

    private void processHeartbeat(String message, InetAddress senderIp) {
        String[] parts = message.split("\\s+", 5); // max 5 partes

        if (parts.length < 4 || !parts[0].equals("HEARTBEAT")) {
            return; // não é heartbeat válido
        }

        try {
            int senderClientPort = Integer.parseInt(parts[1]);
            int senderDbPort = Integer.parseInt(parts[2]);
            // int version = Integer.parseInt(parts[3]);

            // Ignora o próprio heartbeat
            if (senderClientPort == selfClientTcpPort) {
                return;
            }

            // Se for do Primary atual → aplica query (se existir)
            if (senderClientPort == serverService.getPrimaryClientTcpPort()) {
                if (parts.length == 5 && dbManager != null) {
                    String sql = parts[4].trim();
                    if (!sql.isEmpty()) {
                        System.out.println("[HB] Aplicando query do Primary: " + sql);
                        dbManager.executeUpdate(sql);
                    }
                }
            }
            // Caso contrário: é outro backup → ignora
        } catch (Exception e) {
            System.err.println("[HB] Erro ao processar heartbeat: " + e.getMessage());
        }
    }

    // ====================== SHUTDOWN ======================
    public void shutdown() {
        running = false;
        this.interrupt();

        if (multicastReceiver != null && !multicastReceiver.isClosed()) {
            try {
                multicastReceiver.leaveGroup(InetAddress.getByName(MULTICAST_IP));
                multicastReceiver.close();
            } catch (Exception ignored) {}
        }

        System.out.println("[HeartbeatManager] Parado com sucesso.");
    }
}