package pt.isec.pd.server;

import pt.isec.pd.common.MessageType; // Presumida a existência
import java.net.*;
import java.nio.charset.StandardCharsets;

public class HeartbeatSender extends Thread {
    private final DatagramSocket socket;
    private final String directoryHost;
    private final int directoryPort;
    private final int tcpPort;
    private final int heartbeatInterval;

    // ⚠️ NOVO: Flag para controle explícito de execução (coerência com ServerService)
    private volatile boolean running = true;

    public HeartbeatSender(DatagramSocket socket, String directoryHost, int directoryPort, int tcpPort, int heartbeatInterval) {
        this.socket = socket;
        this.directoryHost = directoryHost;
        this.directoryPort = directoryPort;
        this.tcpPort = tcpPort;
        this.heartbeatInterval = heartbeatInterval;
        setDaemon(true);
        setName("Heartbeat-Sender-Dir");
    }

    @Override
    public void run() {
        try {
            InetAddress dirAddr = InetAddress.getByName(directoryHost);

            // ⚠️ ALTERADO: Usar a flag 'running'
            while (running) {
                // String msg = "HEARTBEAT " + tcpPort; // Original
                String msg = String.format("HEARTBEAT %d", tcpPort); // Uso de String.format

                byte[] data = msg.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(data, data.length, dirAddr, directoryPort);

                socket.send(packet);

                // Log mais conciso
                System.out.printf("[HB Dir] Enviado ao Directory (%s:%d).%n", directoryHost, directoryPort);

                Thread.sleep(heartbeatInterval);
            }
        } catch (InterruptedException e) {
            // Interrupção esperada pelo shutdown() do ServerService
            Thread.currentThread().interrupt();
            System.out.println("[HB Dir] Interrompido.");
        } catch (SocketException e) {
            // Esperado se o socket for fechado (pode ocorrer antes da InterruptedException)
            if (running) {
                System.err.println("[HB Dir] Socket fechado: " + e.getMessage());
            } else {
                System.out.println("[HB Dir] Socket fechado durante o shutdown.");
            }
        } catch (Exception e) {
            System.err.println("[HB Dir] Erro fatal: " + e.getMessage());
        } finally {
            this.running = false; // Garantir que a flag é limpa
        }
    }

    // ⚠️ NOVO: Método auxiliar para sincronização com ServerService.shutdown()
    public void shutdown() {
        this.running = false;
        this.interrupt();
    }
}