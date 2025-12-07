// pt.isec.pd.directory.DirectoryService
package pt.isec.pd.directory;

import pt.isec.pd.common.core.MessageType;
import pt.isec.pd.common.core.ServerInfo;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DirectoryService {
    private static final int HEARTBEAT_TO_SERVERS_MS = 13000;
    private static final int SERVER_TIMEOUT_MS = 17000;
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
                    case HEARTBEAT -> handleHeartbeat(parts, packet.getAddress(), packet.getPort()); // 👈 ALTERADO
                    case REQUEST_SERVER -> handleRequest(socket, packet.getAddress(), packet.getPort());
                    case UNREGISTER -> handleUnregister(parts, packet.getAddress());
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
        ServerInfo newServer = new ServerInfo(address, tcpClientPort, tcpDbPort, udpPort);

        synchronized (activeServers) {
            // Remove se já existir (re-registo)
            activeServers.removeIf(s -> s.getKey().equals(key));

            // Adiciona no FIM (ordem de chegada)
            activeServers.add(newServer);

            // Ordena por registrationTime ASC (mais antigo primeiro)
            activeServers.sort((a, b) -> Long.compare(a.getRegistrationTime(), b.getRegistrationTime()));

            newServer.setLastHeartbeat(java.time.Instant.now());

            ServerInfo primary = activeServers.get(0);  // SEMPRE o mais antigo!
            String confirmMsg = String.format("PRIMARY %s %d %d",
                    primary.getAddress().getHostAddress(),
                    primary.getTcpClientPort(),
                    primary.getTcpDbPort());

            DatagramSocket socket = new DatagramSocket();
            byte[] buf = confirmMsg.getBytes();
            DatagramPacket confirmPacket = new DatagramPacket(buf, buf.length, address, sourcePort);
            socket.send(confirmPacket);
            socket.close();

            System.out.println("[Directory] SERVIDOR REGISTADO: " + key);
            System.out.println("[Directory] PRIMARY (mais antigo): " + primary.getKey() + " | reg: " + primary.getRegistrationTime());
            logActiveServers();
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

            // PRIMARY = mais antigo vivo
            ServerInfo primary = activeServers.get(0);
            String response = primary.getAddress().getHostAddress() + " " + primary.getTcpClientPort();
            socket.send(new DatagramPacket(response.getBytes(), response.length(), clientAddr, clientPort));
            System.out.println("[Directory] Cliente → PRIMARY (mais antigo): " + response + " | reg: " + primary.getRegistrationTime());
        }
    }

// pt.isec.pd.directory.DirectoryService

    private void handleHeartbeat(String[] parts, InetAddress address, int sourcePort) throws IOException {
        if (parts.length < 2) return;

        int tcpClientPort;
        try {
            tcpClientPort = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            System.out.println("[Directory] Porto inválido no HEARTBEAT");
            return;
        }

        String key = address.getHostAddress() + ":" + tcpClientPort;

        // ⚠️ NOVO: Abrir socket apenas para resposta
        try (DatagramSocket responseSocket = new DatagramSocket()) {

            synchronized (activeServers) {
                ServerInfo server = activeServers.stream()
                        .filter(s -> s.getKey().equals(key))
                        .findFirst()
                        .orElse(null);

                if (server != null) {
                    // 1. Atualizar o Heartbeat
                    server.setLastHeartbeat(java.time.Instant.now());
                    // System.out.println("[Directory] Heartbeat de " + key);

                    // 2. Determinar o Primary atual (sempre o índice 0 após a ordenação do Monitor)
                    ServerInfo primary = activeServers.get(0);

                    // 3. Construir a mensagem de resposta
                    String responseMsg = String.format("PRIMARY %s %d %d",
                            primary.getAddress().getHostAddress(),
                            primary.getTcpClientPort(),
                            primary.getTcpDbPort());

                    // 4. Enviar a resposta de volta para o Servidor (IP:Porta UDP que enviou o HB)
                    byte[] buf = responseMsg.getBytes();
                    DatagramPacket confirmPacket = new DatagramPacket(buf, buf.length, address, sourcePort);
                    responseSocket.send(confirmPacket);

                    // Opcional: Log
                    // System.out.println("[Directory] HB respondido a " + key + " com: " + responseMsg);
                }
            }
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

    private void handleUnregister(String[] parts, InetAddress address) {
        if (parts.length < 2) return;

        int tcpClientPort = Integer.parseInt(parts[1]);
        String key = address.getHostAddress() + ":" + tcpClientPort;

        synchronized (activeServers) {
            ServerInfo server = activeServers.stream()
                    .filter(s -> s.getKey().equals(key))
                    .findFirst()
                    .orElse(null);

            if (server != null) {
                activeServers.remove(server);
                System.out.println("[Directory] UNREGISTER recebido: " + key + " removido.");
                if (activeServers.isEmpty()) {
                    System.out.println("[Directory] Nenhum servidor ativo.");
                } else {
                    System.out.println("[Directory] Novo principal: " + activeServers.get(0).getKey());
                }
                logActiveServers();
            }
        }
    }

    private void logActiveServers() {
        if (activeServers.isEmpty()) {
            System.out.println("[Directory] Nenhum servidor ativo.");
            return;
        }
        System.out.println("[Directory] Servidores ativos (ordem: mais antigo → mais recente):");
        int i = 1;
        for (ServerInfo s : activeServers) {
            System.out.printf("   %d. %s | reg: %d%s%n",
                    i++, s.getKey(), s.getRegistrationTime(),
                    (i == 2 ? " ← PRIMARY" : ""));
        }
    }
}