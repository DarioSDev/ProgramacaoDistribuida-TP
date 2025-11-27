package pt.isec.pd.server;

import pt.isec.pd.db.DatabaseManager;
import java.net.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HeartbeatManager extends Thread {
    private final DatagramSocket socket;
    private final String directoryHost;
    private final int directoryPort;
    private final int selfClientTcpPort;
    private final int selfDBTcpPort;
    private final DatabaseManager dbManager;
    private final int heartbeatInterval;

    // ✅ NOVO: guarda o IP real deste servidor
    private final InetAddress selfIp;
    private final InetAddress primaryIp;

    private volatile boolean running = true;
    private MulticastSocket multicastSocket;

    public HeartbeatManager(DatagramSocket socket, String directoryHost, int directoryPort,
                            int selfClientTcpPort, int selfDBTcpPort,
                            DatabaseManager dbManager, int heartbeatInterval, InetAddress primaryIp) throws UnknownHostException {
        this.socket = socket;
        this.directoryHost = directoryHost;
        this.directoryPort = directoryPort;
        this.selfClientTcpPort = selfClientTcpPort;
        this.selfDBTcpPort = selfDBTcpPort;
        this.dbManager = dbManager;
        this.heartbeatInterval = heartbeatInterval;
        this.primaryIp = primaryIp;

        // ✅ guarda o IP desta máquina
        this.selfIp = InetAddress.getLocalHost();

        setDaemon(true);
        setName("Heartbeat-Sender-Dir");
    }

    // === ARRANQUE AUTOMÁTICO DAS 2 THREADS ===
    @Override
    public void start() {
        super.start(); // thread do sender (run())
        new Thread(() -> listenMulticastHeartbeats(), "Heartbeat-Receiver-Multi").start();
    }

    // === THREAD PRINCIPAL: SENDER (não apagado, não mexido na assinatura) ===
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

    // === SENDER SEM QUERY ===
    public void sendHeartbeat() {
        try {
            InetAddress dirAddr = InetAddress.getByName(directoryHost);
            String msg = String.format("HEARTBEAT %d %d %d",
                    selfClientTcpPort, selfDBTcpPort, dbManager.getDbVersion());
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, dirAddr, directoryPort);
            socket.send(packet);

            System.out.println("hb enviado (dir) -> " + msg);

        } catch (IOException e) {
            System.out.println("hb enviado (dir) -> ERRO ao enviar heartbeat: " + e.getMessage());
        }
    }

    // === SENDER COM QUERY SQL ===
    public void sendHeartbeat(String query) {
        try {
            InetAddress dirAddr = InetAddress.getByName(directoryHost);
            String msg = String.format("HEARTBEAT %d %d %d %s",
                    selfClientTcpPort, selfDBTcpPort, dbManager.getDbVersion(), query);
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, dirAddr, directoryPort);
            socket.send(packet);

            System.out.println("hb enviado (dir) -> " + msg);

        } catch (IOException e) {
            System.out.println("hb enviado (dir) -> ERRO ao enviar heartbeat com query: " + e.getMessage());
        }
    }

    // === RECEIVER MULTICAST: OUVE TODOS ===
    private void listenMulticastHeartbeats() {
        try {
            String multiIp = "230.30.30.30";
            int multiPort = 3030;

            MulticastSocket ms = new MulticastSocket(multiPort);
            ms.setReuseAddress(true);
            InetAddress group = InetAddress.getByName(multiIp);
            ms.joinGroup(group);

            multicastSocket = ms;

            System.out.println("[HB MULTI] A ouvir heartbeats multicast em " + multiIp + ":" + multiPort);

            byte[] buf = new byte[512];

            while (running) {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                ms.receive(p);
                String m = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8).trim();

                InetAddress origem = p.getAddress();

                // ✅ classifica a origem e imprime debug exato
                if (origem.equals(selfIp)) {
                    System.out.println("hb recebido - é meu " + m);
                } else if (origem.equals(primaryIp)) {
                    System.out.println("hb recebido - é primary " + m);
                } else {
                    System.out.println("hb recebido - é secondary " + m);
                }

                System.out.println("-----------------------------");
            }

            ms.leaveGroup(group);
            ms.close();

        } catch (Exception e) {
            System.err.println("[HB MULTI] Erro no receiver: " + e.getMessage());
        }
    }

    // === SHUTDOWN: PARA AMBAS AS THREADS ===
    public void shutdown() {
        running = false;
        this.interrupt();

        if (multicastSocket != null && !multicastSocket.isClosed()) {
            multicastSocket.close();
        }

        System.out.println("[HB] Heartbeat sender + receiver parados.");
    }
}
