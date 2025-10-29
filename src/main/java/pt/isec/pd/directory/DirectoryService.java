// pt.isec.pd.directory.DirectoryService
package pt.isec.pd.directory;

import pt.isec.pd.common.MessageType;
import pt.isec.pd.common.ServerInfo;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DirectoryService {
    private static final int HEARTBEAT_TO_SERVERS_MS = 13000; // 13s - Directory → Servidores
    private static final int SERVER_TIMEOUT_MS = 17000;       // 17s - Directory remove servidor
    private final int udpPort;
    private final List<ServerInfo> activeServers = new CopyOnWriteArrayList<>();
    private boolean running = true;

    public DirectoryService(int udpPort) {
        this.udpPort = udpPort;
    }

    public void start() {
        System.out.println("[Directory] Serviço de diretoria a escutar no porto UDP " + udpPort);
        System.out.println("[Directory] Config: Heartbeat → servidores: " + (HEARTBEAT_TO_SERVERS_MS / 1000) + "s | Timeout remoção: " + (SERVER_TIMEOUT_MS / 1000) + "s");

        new DirectoryHeartbeatMonitor(activeServers).start();
        startDirectoryHeartbeatSender();

        try (DatagramSocket socket = new DatagramSocket(udpPort)) {
            byte[] buffer = new byte[1024];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength()).trim().replace("\"", "");
                System.out.printf("[Directory] Datagram recebido de %s:%d -> '%s'%n",
                        packet.getAddress().getHostAddress(), packet.getPort(), message);

                String[] parts = message.trim().split("\\s+");
                MessageType type = MessageType.fromString(parts[0]);
                if (type == null) {
                    System.out.println("[Directory] Tipo de mensagem inválido: " + parts[0]);
                    continue;
                }

                switch (type) {
                    case REGISTER -> handleRegister(parts, packet.getAddress(), packet.getPort());
                    case HEARTBEAT -> handleHeartbeat(parts, packet.getAddress());
                    case REQUEST_SERVER -> handleRequest(socket, packet.getAddress(), packet.getPort());
                    default -> System.out.println("[Directory] Mensagem desconhecida: " + message);
                }
            }
        } catch (IOException e) {
            System.err.println("[Directory] Erro: " + e.getMessage());
        }
    }

    private void handleRegister(String[] parts, InetAddress address, int sourcePort) throws IOException {
        if (parts.length < 4) {
            System.out.println("[Directory] REGISTER inválido: faltam portos");
            return;
        }

        int tcpClientPort, tcpDbPort, udpPort;
        try {
            tcpClientPort = Integer.parseInt(parts[1]);
            tcpDbPort = Integer.parseInt(parts[2]);
            udpPort = Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            System.out.println("[Directory] Portos inválidos no REGISTER");
            return;
        }

        String key = address.getHostAddress() + ":" + tcpClientPort;

        synchronized (activeServers) {
            boolean exists = activeServers.stream().anyMatch(s -> s.getKey().equals(key));
            ServerInfo newServer = new ServerInfo(address, tcpClientPort, tcpDbPort, udpPort);

            if (!exists) {
                activeServers.add(newServer);
                System.out.println("[Directory] Novo servidor registado: " + newServer);
            } else {
                ServerInfo existing = activeServers.stream()
                        .filter(s -> s.getKey().equals(key))
                        .findFirst().orElse(null);
                if (existing != null) {
                    existing.setLastHeartbeat(java.time.Instant.now());
                    System.out.println("[Directory] Servidor já registado (atualizado): " + key);
                }
                newServer = existing;
            }

            // Confirmação com o principal atual
            ServerInfo primary = activeServers.get(0);
            String confirmMsg = String.format("PRIMARY %s %d",
                    primary.getAddress().getHostAddress(),
                    primary.getTcpDbPort());

            DatagramSocket socket = new DatagramSocket();
            byte[] buf = confirmMsg.getBytes();
            DatagramPacket confirmPacket = new DatagramPacket(buf, buf.length, address, sourcePort);
            socket.send(confirmPacket);
            socket.close();

            System.out.println("[Directory] Confirmação enviada: " + confirmMsg + " → " + key);
            System.out.println("[Directory] Servidor principal atual: " + primary.getKey());
            logActiveServers();
        }
    }

    private void handleHeartbeat(String[] parts, InetAddress address) {
        if (parts.length < 2) return;

        int tcpClientPort = Integer.parseInt(parts[1]);
        String key = address.getHostAddress() + ":" + tcpClientPort;

        synchronized (activeServers) {
            ServerInfo server = activeServers.stream()
                    .filter(s -> s.getKey().equals(key))
                    .findFirst()
                    .orElse(null);

            if (server != null) {
                server.setLastHeartbeat(java.time.Instant.now());
                // Log opcional: descomentar para ver todos os heartbeats
                // System.out.println("[Directory] Heartbeat de " + key);
            }
        }
    }

    private void handleRequest(DatagramSocket socket, InetAddress clientAddr, int clientPort) throws IOException {
        synchronized (activeServers) {
            if (activeServers.isEmpty()) {
                String msg = "NO_SERVER_AVAILABLE";
                socket.send(new DatagramPacket(msg.getBytes(), msg.length(), clientAddr, clientPort));
                System.out.println("[Directory] Cliente → nenhum servidor disponível");
                return;
            }

            ServerInfo primary = activeServers.get(0);
            String response = primary.getAddress().getHostAddress() + " " + primary.getTcpClientPort();
            socket.send(new DatagramPacket(response.getBytes(), response.length(), clientAddr, clientPort));
            System.out.println("[Directory] Cliente → redirecionado para principal: " + response + " (" + primary.getKey() + ")");
        }
    }

    private void startDirectoryHeartbeatSender() {
        new Thread(() -> {
            while (running) {
                synchronized (activeServers) {
                    for (ServerInfo server : activeServers) {
                        try {
                            String msg = "DIRECTORY_HEARTBEAT";
                            byte[] buf = msg.getBytes();
                            DatagramSocket ds = new DatagramSocket();
                            DatagramPacket packet = new DatagramPacket(
                                    buf, buf.length,
                                    server.getAddress(),
                                    server.getUdpPort()
                            );
                            ds.send(packet);
                            ds.close();
                        } catch (IOException e) {
                            System.out.println("[Directory] Falha ao enviar heartbeat para " + server.getKey());
                        }
                    }
                }
                try {
                    Thread.sleep(HEARTBEAT_TO_SERVERS_MS); // 13 segundos
                } catch (InterruptedException ignored) {}
            }
        }, "Directory-Heartbeat-Sender").start();
    }

    private void logActiveServers() {
        if (activeServers.isEmpty()) {
            System.out.println("[Directory] Nenhum servidor ativo.");
            return;
        }
        System.out.println("[Directory] Servidores ativos (ordem de registo):");
        int i = 1;
        for (ServerInfo s : activeServers) {
            System.out.printf("   %d. %s (hb: %s)%s%n",
                    i++, s.getKey(), s.getLastHeartbeat(),
                    (i == 2 ? " ← PRINCIPAL" : ""));
        }
    }
}