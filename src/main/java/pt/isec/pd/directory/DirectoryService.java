package pt.isec.pd.directory;

import pt.isec.pd.common.MessageType;
import pt.isec.pd.common.ServerInfo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DirectoryService {

    private final int udpPort;
    private final List<ServerInfo> activeServers = new CopyOnWriteArrayList<>();
    private boolean running = true;

    public DirectoryService(int udpPort) {
        this.udpPort = udpPort;
    }

    public void start() {
        System.out.println("[Directory] Serviço de diretoria a escutar no porto UDP " + udpPort);

        // Thread que remove servidores inativos
        new DirectoryHeartbeatMonitor(activeServers).start();

        try (DatagramSocket socket = new DatagramSocket(udpPort)) {
            byte[] buffer = new byte[1024];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength()).trim().replace("\"", "");
                System.out.printf("[Directory] Datagram recebido de %s:%d -> '%s'%n",
                        packet.getAddress().getHostAddress(), packet.getPort(), message);

                System.out.printf(
                        "[Directory] Datagram recebido de %s:%d -> '%s'%n",
                        packet.getAddress().getHostAddress(),
                        packet.getPort(),
                        message
                );

                String[] parts = message.trim().split("\\s+");
                MessageType type = MessageType.fromString(parts[0]);
                if (type == null) {
                    System.out.println("[Directory] Tipo de mensagem inválido: " + parts[0]);
                    continue;
                }

                switch (type) {
                    case REGISTER -> handleRegister(parts, packet.getAddress());
                    case HEARTBEAT -> handleHeartbeat(parts, packet.getAddress());
                    case REQUEST_SERVER -> handleRequest(socket, packet.getAddress(), packet.getPort());
                    default -> System.out.println("[Directory] ⚠️ Mensagem desconhecida: " + message);
                }
            }
        } catch (IOException e) {
            System.err.println("[Directory] Erro: " + e.getMessage());
        }
    }

    private void handleRegister(String[] parts, InetAddress address) {
        if (parts.length < 2) {
            System.out.println("[Directory] ⚠️ Mensagem REGISTER inválida");
            return;
        }

        int tcpPort = Integer.parseInt(parts[1]);
        String key = address.getHostAddress() + ":" + tcpPort;

        boolean exists = activeServers.stream().anyMatch(s -> s.getKey().equals(key));
        if (!exists) {
            ServerInfo info = new ServerInfo(address, tcpPort);
            activeServers.add(info);
            System.out.println("[Directory] ✅ Novo servidor registado: " + info);
        } else {
            System.out.println("[Directory] ℹ️ Servidor já registado: " + key);
        }
    }

    private void handleHeartbeat(String[] parts, InetAddress address) {
        if (parts.length < 2) {
            System.out.println("[Directory] ⚠️ Mensagem HEARTBEAT inválida");
            return;
        }

        int tcpPort = Integer.parseInt(parts[1]);
        String key = address.getHostAddress() + ":" + tcpPort;

        for (ServerInfo s : activeServers) {
            if (s.getKey().equals(key)) {
                s.setLastHeartbeat(Instant.now());
                System.out.println("[Directory] 💓 Heartbeat recebido de " + s);
                return;
            }
        }

        System.out.println("[Directory] ❌ Heartbeat ignorado (não registado): " + key);
    }

    private void handleRequest(DatagramSocket socket, InetAddress clientAddr, int clientPort) throws IOException {
        if (activeServers.isEmpty()) {
            String msg = "NO_SERVER_AVAILABLE";
            socket.send(new DatagramPacket(msg.getBytes(), msg.length(), clientAddr, clientPort));
            System.out.println("[Directory] Pedido de cliente -> nenhum servidor disponível");
            return;
        }

        ServerInfo oldest = activeServers.get(0);
        String response = oldest.getAddress().getHostAddress() + " " + oldest.getTcpPort();
        socket.send(new DatagramPacket(response.getBytes(), response.length(), clientAddr, clientPort));
        System.out.println("[Directory] Pedido de cliente -> respondeu com " + response);
    }
}
