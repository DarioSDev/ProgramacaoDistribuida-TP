package pt.isec.pd.server;

import pt.isec.pd.db.DatabaseManager;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class HeartbeatManager extends Thread {
    private final DatagramSocket socket;
    private final String directoryHost;
    private final int directoryPort;
    private final int selfClientTcpPort;
    private final int selfDBTcpPort;
    private final DatabaseManager dbManager;
    private final int heartbeatInterval;

    // ⚠️ NOVO: Flag para controle explícito de execução (coerência com ServerService)
    private volatile boolean running = true;

    public HeartbeatManager(DatagramSocket socket, String directoryHost, int directoryPort, int selfClientTcpPort, int selfDBTcpPort, DatabaseManager dbManager , int heartbeatInterval) {
        this.socket = socket;
        this.directoryHost = directoryHost;
        this.directoryPort = directoryPort;
        this.selfClientTcpPort = selfClientTcpPort;
        this.selfDBTcpPort = selfDBTcpPort;
        this.dbManager = dbManager;
        this.heartbeatInterval = heartbeatInterval;
        setDaemon(true);
        setName("Heartbeat-Sender-Dir");
    }

    @Override
    public void run() {
        try {
            InetAddress dirAddr = InetAddress.getByName(directoryHost);

            while (running) {
                sendHeartbeat();
                Thread.sleep(heartbeatInterval);
            }
        } catch (InterruptedException e) {
            // Interrupção esperada pelo shutdown() do ServerService
            Thread.currentThread().interrupt();
            System.out.println("[HB Dir] Interrompido.");
        } catch (Exception e) {
            System.err.println("[HB Dir] Erro fatal: " + e.getMessage());
        } finally {
            this.running = false; // Garantir que a flag é limpa
        }
    }

    public void sendHeartbeat() {
        try {
            InetAddress dirAddr = InetAddress.getByName(directoryHost);
            String msg = String.format("HEARTBEAT %d %d %d", selfClientTcpPort, selfDBTcpPort, dbManager.getDbVersion());
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, dirAddr, directoryPort);

            socket.send(packet);

            System.out.printf("[HB Dir] Enviado ao Directory (%s:%d).%n", directoryHost, directoryPort);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendHeartbeat(String query) {
        try {
            InetAddress dirAddr = InetAddress.getByName(directoryHost);
            String msg = String.format("HEARTBEAT %d %d %d %s", selfClientTcpPort, selfDBTcpPort, dbManager.getDbVersion(), query);
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, dirAddr, directoryPort);

            socket.send(packet);

            System.out.printf("[HB Dir] Enviado ao Directory (%s:%d).%n", directoryHost, directoryPort);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ⚠️ NOVO: Método auxiliar para sincronização com ServerService.shutdown()
    public void shutdown() {
        this.running = false;
        this.interrupt();
    }
}