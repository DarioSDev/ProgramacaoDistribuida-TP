package pt.isec.pd.server;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

public class ServerService {
    private static final int HEARTBEAT_INTERVAL = 5000; // 5 segundos

    private final String directoryHost;
    private final int directoryPort;
    private final int tcpPort;
    private final DatagramSocket socket;

    public ServerService(String directoryHost, int directoryPort, int tcpPort) throws SocketException {
        this.directoryHost = directoryHost;
        this.directoryPort = directoryPort;
        this.tcpPort = tcpPort;
        this.socket = new DatagramSocket();
    }

    public void start() {
        try {
            registerServer();
            HeartbeatSender sender = new HeartbeatSender(socket, directoryHost, directoryPort, tcpPort, HEARTBEAT_INTERVAL);
            sender.run();
            System.out.println("[ServerService] Servidor registado e heartbeat iniciado.");
        } catch (IOException e) {
            System.err.println("[ServerService] Erro ao iniciar o servidor: " + e.getMessage());
        }
    }

    private void registerServer() throws IOException {
        String msg = "REGISTER " + tcpPort;
        byte[] data = msg.getBytes(StandardCharsets.UTF_8);
        InetAddress address = InetAddress.getByName(directoryHost);
        DatagramPacket packet = new DatagramPacket(data, data.length, address, directoryPort);
        socket.send(packet);
        System.out.printf("[ServerService] Enviado registo: %s:%d -> %s%n", directoryHost, directoryPort, msg);
    }

    private void sendHeartbeat() {
        try {
            String msg = "HEARTBEAT " + tcpPort;
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            InetAddress address = InetAddress.getByName(directoryHost);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, directoryPort);
            socket.send(packet);
            System.out.printf("[ServerService] → Heartbeat enviado (%d)%n", tcpPort);
        } catch (IOException e) {
            System.err.println("[ServerService] Erro ao enviar heartbeat: " + e.getMessage());
        }
    }
}
