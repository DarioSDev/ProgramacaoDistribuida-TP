package pt.isec.pd.server;

import pt.isec.pd.db.DatabaseManager;
import java.net.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HeartbeatManager extends Thread {

    private final DatagramSocket socket;       // para HEARTBEAT da diretoria
    private final String directoryHost;        // host da diretoria
    private final int directoryPort;           // porto UDP da diretoria
    private final int selfClientTcpPort;       // proprio porto TCP para clientes
    private final int selfDBTcpPort;           // proprio porto TCP para BD sync
    private final DatabaseManager dbManager;   // acesso à BD
    private final int heartbeatInterval;       // intervalo do sender

    private final int primaryClientTcpPort;    // porto TCP primary (recebido da diretoria)
    private final int primaryDBTcpPort;        // porto DB primary (recebido da diretoria)
    private final InetAddress primaryAddress;  // IP primary (mas não usado na comparação)
    private final ServerService serverService;

    private volatile boolean running = true;

    private MulticastSocket multicastSocket;

    public HeartbeatManager(DatagramSocket socket, String directoryHost, int directoryPort,
                            int selfClientTcpPort, int selfDBTcpPort,
                            DatabaseManager dbManager, int heartbeatInterval,
                            InetAddress primaryAddress,
                            int primaryClientTcpPort,
                            int primaryDBTcpPort,
                            ServerService serverService) {
        this.socket = socket;
        this.directoryHost = directoryHost;
        this.directoryPort = directoryPort;
        this.selfClientTcpPort = selfClientTcpPort;
        this.selfDBTcpPort = selfDBTcpPort;
        this.dbManager = dbManager;
        this.heartbeatInterval = heartbeatInterval;
        this.primaryAddress = primaryAddress;
        this.primaryClientTcpPort = primaryClientTcpPort;
        this.primaryDBTcpPort = primaryDBTcpPort;
        this.serverService = serverService;
        setDaemon(true);
        setName("Heartbeat-Sender-Dir");
    }

    @Override
    public void start() {
        super.start();                                                                                // inicia sender
        new Thread(this::listenAllHeartbeats, "Heartbeat-Receiver-Multi").start();              // inicia receiver
    }

    // === THREAD SENDER ===
    @Override
    public void run() {
        try {
            while (running) {
                sendHeartbeat();
                Thread.sleep(heartbeatInterval);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[HB Dir] Sender interrompido.");
        } catch (Exception e) {
            System.err.println("[HB Dir] Erro no sender: " + e.getMessage());
        } finally {
            running = false;
        }
    }

    // SENDER sem query
    public void sendHeartbeat() {
        try {
            InetAddress mcAddr = InetAddress.getByName("230.30.30.30");
            String msgMC = String.format("HEARTBEAT %d %d %d",
                    selfClientTcpPort, selfDBTcpPort, dbManager.getDbVersion());
            byte[] bufMC = msgMC.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(bufMC, bufMC.length, mcAddr, 3030));

            sendHeartbeatToDirectory(msgMC);

            System.out.println("\nhb enviado -> " + msgMC);

        } catch (IOException e) {
            System.out.println("\nhb enviado -> ERRO ao enviar: " + e.getMessage());
        }
    }

    // SENDER com query SQL incluída
    public void sendHeartbeat(String query) {
        try {
            InetAddress mcAddr = InetAddress.getByName("230.30.30.30");
            int newVersion = dbManager.getDbVersion();
            String msgMC = String.format("HEARTBEAT %d %d %d %s",
                    selfClientTcpPort, selfDBTcpPort, newVersion, query);
            byte[] bufMC = msgMC.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(bufMC, bufMC.length, mcAddr, 3030));

            sendHeartbeatToDirectory(msgMC);

            System.out.println("\nhb enviado -> " + msgMC);

        } catch (IOException e) {
            System.out.println("\nhb enviado -> ERRO ao enviar c/ query: " + e.getMessage());
        }
    }

    // Envia ao Directory
    private void sendHeartbeatToDirectory(String msg) {
        try {
            InetAddress dirAddr = InetAddress.getByName(directoryHost);
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(data, data.length, dirAddr, directoryPort));
            System.out.println("hb enviado (dir) -> " + msg);
        } catch (IOException ignored) {}
    }

    // === THREAD RECEIVER: OUVE TODOS E CLASSIFICA POR PORTOS (debug) ===
    private void listenAllHeartbeats() {
        try {
            MulticastSocket ms = new MulticastSocket(3030);
            ms.setReuseAddress(true);
            InetAddress group = InetAddress.getByName("230.30.30.30");
            ms.joinGroup(group);
            this.multicastSocket = ms;

            System.out.println("[HB MULTI] A ouvir heartbeats multicast em 230.30.30.30:3030\n");

            byte[] buffer = new byte[512];

            while (running) {
                DatagramPacket p = new DatagramPacket(buffer, buffer.length);
                ms.receive(p);
                String msg = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8).trim();

                // Parse básico do heartbeat: HEARTBEAT <clientPort> <dbPort> <version> [query opcional]
                String[] parts = msg.split("\\s+", 5);

                // Verifica se tem pelo menos: HEARTBEAT <portC> <portDB> <version>
                if (parts.length < 4 || !parts[0].equals("HEARTBEAT")) {
                    System.out.println("hb recebido -> mal formatado ou não é HEARTBEAT: " + msg);
                    System.out.println("-----------------------------");
                    continue;
                }

                // Captura as portas do servidor que enviou o HB
                int senderClientPort = Integer.parseInt(parts[1]);
                int senderDbPort = Integer.parseInt(parts[2]);

                // A query opcional pode estar ou não presente
                String sqlQuery = parts.length == 5 ? parts[4] : "Nenhuma query incluída";

                System.out.println("\n[HB Multi Recebido] De: " + p.getAddress().getHostAddress() + ":" + p.getPort());
                System.out.println("Conteúdo: " + msg);

                // 1. É o próprio servidor?
                if (senderClientPort == selfClientTcpPort && senderDbPort == selfDBTcpPort) {
                    System.out.println("→ CLASSIFICAÇÃO: É o **MEU** próprio heartbeat. (Deve ser ignorado)");
                }
                // 2. É o Primary atual (conforme registado no Directory)?
                else if (senderClientPort == primaryClientTcpPort) { // Basta verificar a porta do cliente, que é única
                    System.out.println("→ CLASSIFICAÇÃO: É do servidor **PRINCIPAL** (Primary).");
                    System.out.println("  → Query SQL incluída: " + (parts.length == 5 ? "SIM" : "NÃO"));
                    if (parts.length == 5) {
                        dbManager.executeUpdate(sqlQuery);
                    }
                }
                // 3. É outro servidor Secundário?
                else {
                    System.out.println("→ CLASSIFICAÇÃO: É um servidor **SECUNDÁRIO** (Backup). (Deve ser ignorado)");
                }

                System.out.println("-----------------------------");
            }

        } catch (Exception e) {
            System.err.println("[HB Multi] Erro no receiver: " + e.getMessage());
        }
    }

    // Shutdown único para parar as 2 threads
    public void shutdown() {
        running = false;
        this.interrupt();

        if (multicastSocket != null && !multicastSocket.isClosed())
            multicastSocket.close();

        System.out.println("[HB] Heartbeat sender + receiver parados.");
    }
}
