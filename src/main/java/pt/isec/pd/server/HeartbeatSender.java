package pt.isec.pd.server;

import java.net.*;
import java.nio.charset.StandardCharsets;

public class HeartbeatSender extends Thread {
    private final DatagramSocket socket;
    private final String directoryHost;
    private final int directoryPort;
    private final int tcpPort;
    private final int heartbeatInterval;

    public HeartbeatSender(DatagramSocket socket, String directoryHost, int directoryPort, int tcpPort,  int heartbeatInterval) {
        this.socket = socket;
        this.directoryHost = directoryHost;
        this.directoryPort = directoryPort;
        this.tcpPort = tcpPort;
        this.heartbeatInterval = heartbeatInterval;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            while (true) {
                String msg = "HEARTBEAT " + tcpPort;
                byte[] data = msg.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(
                        data, data.length, InetAddress.getByName(directoryHost), directoryPort
                );
                socket.send(packet);
                System.out.printf("[Server] ❤️ Heartbeat enviado (%s:%d)%n", directoryHost, tcpPort);
                Thread.sleep(heartbeatInterval);
            }
        } catch (Exception e) {
            System.err.println("[Server] Erro no envio de heartbeat: " + e.getMessage());
        }
    }
}
