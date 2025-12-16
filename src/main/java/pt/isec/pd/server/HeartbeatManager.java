package pt.isec.pd.server;

import pt.isec.pd.db.DatabaseManager;
import java.net.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HeartbeatManager extends Thread {

    private static final String MULTICAST_IP = "230.30.30.30";
    private static final int MULTICAST_PORT = 3030;

    private final DatagramSocket socket;       // para HEARTBEAT da diretoria
    private final String directoryHost;        // host da diretoria
    private final int directoryPort;           // porto UDP da diretoria
    private final int selfClientTcpPort;       // proprio porto TCP para clientes
    private final int selfDBTcpPort;           // proprio porto TCP para BD sync
    private final DatabaseManager dbManager;   // acesso à BD
    private final int heartbeatInterval;       // intervalo do sender

    private int primaryClientTcpPort;    // porto TCP primary (recebido da diretoria)
    private int primaryDBTcpPort;        // porto DB primary (recebido da diretoria)
    private InetAddress primaryAddress;  // IP primary (mas não usado na comparação)
    private final ServerService serverService; // Para iniciar o shutdown em caso de erro

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
        super.start();
        // [R2]
        new Thread(this::listenAllHeartbeats, "Heartbeat-Receiver-Multi").start();              // inicia receiver
    }

    // === THREAD SENDER ===
    // [R2]
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

    // SENDER periódico (sem query)
    // R2
    public void sendHeartbeat() {
        try {
            InetAddress mcAddr = InetAddress.getByName(MULTICAST_IP);
            String msgMC = String.format("HEARTBEAT %d %d %d",
                    selfClientTcpPort, selfDBTcpPort, dbManager.getDbVersion());
            byte[] bufMC = msgMC.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(bufMC, bufMC.length, mcAddr, MULTICAST_PORT));

            sendHeartbeatToDirectory(msgMC);

            System.out.println("\nhb enviado -> " + msgMC);

        } catch (IOException e) {
            System.out.println("\nhb enviado -> ERRO ao enviar: " + e.getMessage());
        }
    }

    // SENDER com query SQL incluída (USADO APENAS PELO PRIMARY)
    public void sendHeartbeat(String query) {
        try {
            InetAddress mcAddr = InetAddress.getByName(MULTICAST_IP);
            // ATENÇÃO: Aqui usamos dbManager.getDbVersion() que DEVE ser a versão ANTES
            // do incremento, mas a versão que o secundário deve receber é a versão *após* o incremento.
            // No DatabaseManager, garantimos que o incremento ocorre DEPOIS da chamada a sendHeartbeat(query).
            // Contudo, para simplificar e seguir a lógica comum de replicação, o HB deve ter a versão FINAL.
            // Como o DatabaseManager incrementa a versão após esta chamada, vamos assumir que o valor atual + 1 é o correto.

            // ATUALIZAÇÃO: O DatabaseManager foi refatorado para que sendHeartbeat seja chamado ANTES do incremento.
            // Por isso, devemos adicionar +1 ao valor atual para representar a versão que o Secundário deverá ter.
            int nextVersion = dbManager.getDbVersion() + 1;

            String msgMC = String.format("HEARTBEAT %d %d %d %s",
                    selfClientTcpPort, selfDBTcpPort, nextVersion, query);
            byte[] bufMC = msgMC.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(bufMC, bufMC.length, mcAddr, MULTICAST_PORT));

            sendHeartbeatToDirectory(msgMC);

            System.out.println("\nhb enviado (query) -> " + msgMC);

        } catch (IOException e) {
            System.out.println("\nhb enviado (query) -> ERRO ao enviar c/ query: " + e.getMessage());
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

    // === THREAD RECEIVER: OUVE TODOS E CLASSIFICA POR PORTOS ===
    private void listenAllHeartbeats() {
        try {
            MulticastSocket ms = new MulticastSocket(MULTICAST_PORT);
            ms.setReuseAddress(true);
            InetAddress group = InetAddress.getByName(MULTICAST_IP);
            ms.joinGroup(group);
            this.multicastSocket = ms;

            System.out.printf("[HB MULTI] A ouvir heartbeats multicast em %s:%d%n%n", MULTICAST_IP, MULTICAST_PORT);

            byte[] buffer = new byte[512];

            while (running) {
                DatagramPacket p = new DatagramPacket(buffer, buffer.length);
                ms.receive(p);
                String msg = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8).trim();

                // Parse básico do heartbeat: HEARTBEAT <portC> <portDB> <version> [query opcional]
                String[] parts = msg.split("\\s+", 5);

                if (parts.length < 4 || !parts[0].equals("HEARTBEAT")) {
                    System.out.println("hb recebido -> mal formatado: " + msg);
                    System.out.println("-----------------------------");
                    continue;
                }

                int senderClientPort = Integer.parseInt(parts[1]);
                int receivedVersion = Integer.parseInt(parts[3]);

                // Query: se houver 5 partes, a 5ª é a query, senão, null
                String sqlQuery = parts.length == 5 ? parts[4] : null;

                System.out.println("\n[HB Multi Recebido] De: " + p.getAddress().getHostAddress() + ":" + p.getPort());
                System.out.println("Conteúdo: " + msg);
                System.out.println("  → Versão Recebida: " + receivedVersion);

                // 1. É o próprio servidor?
                if (senderClientPort == selfClientTcpPort) {
                    System.out.println("→ CLASSIFICAÇÃO: É o **MEU** próprio heartbeat. (Deve ser ignorado)");
                }
                // 2. É o Primary atual?
                else if (senderClientPort == primaryClientTcpPort) {
                    System.out.println("→ CLASSIFICAÇÃO: É do servidor **PRINCIPAL** (Primary).");

                    int localVersion = dbManager.getDbVersion();

                    // Com Query SQL (Alteração)
                    if (sqlQuery != null) {
                        System.out.println("  → Query SQL incluída: SIM");

                        // Requisito: Versão recebida deve ser igual ao valor local acrescido de 1
                        // [R31]
                        if (receivedVersion == localVersion + 1) {
                            System.out.println("  → Sincronização OK. A aplicar query: " + sqlQuery);
                            // [CORREÇÃO] Chama o método de sincronização que não notifica o cluster.
                            dbManager.executeUpdateBySync(sqlQuery);
                        } else {
                            // [R32]
                            // Perda de sincronização
                            System.err.printf("  → ERRO: Perda de sincronização (V_Received:%d != V_Local:%d+1). Terminando.%n",
                                    receivedVersion, localVersion);
                            serverService.initiateShutdown();
                            break; // Termina o loop
                        }
                    }
                    // Sem Query SQL (Heartbeat periódico do Primary)
                    else {
                        System.out.println("  → Query SQL incluída: NÃO");

                        // Requisito: Sem query, mas com número de versão diferente => Termina
                        if (receivedVersion != localVersion) {
                            System.err.printf("  → ERRO: Perda de sincronização (V_Received:%d != V_Local:%d). Terminando.%n",
                                    receivedVersion, localVersion);
                            serverService.initiateShutdown();
                            break; // Termina o loop
                        }
                    }
                }
                // 3. É outro servidor Secundário?
                else {
                    System.out.println("→ CLASSIFICAÇÃO: É um servidor **SECUNDÁRIO** (Backup). (Deve ser ignorado)");
                }

                System.out.println("-----------------------------");
            }

        } catch (Exception e) {
            if (running) System.err.println("[HB Multi] Erro no receiver: " + e.getMessage());
        }
    }

    public synchronized void updatePrimary(InetAddress ip, int clientPort, int dbPort) {
        // Estas variáveis devem ser 'volatile' e não 'final'
        this.primaryAddress = ip;
        this.primaryClientTcpPort = clientPort;
        this.primaryDBTcpPort = dbPort;
        System.out.printf("[HB] Primary atualizado para: %s:%d%n", ip.getHostAddress(), clientPort);
    }

    // Shutdown único para parar as 2 threads
    public void shutdown() {
        running = false;
        this.interrupt();

        if (multicastSocket != null && !multicastSocket.isClosed()) {
            try {
                InetAddress group = InetAddress.getByName(MULTICAST_IP);
                multicastSocket.leaveGroup(group);
            } catch (IOException ignored) {}
            multicastSocket.close();
        }

        System.out.println("[HB] Heartbeat sender + receiver parados.");
    }
}