// pt.isec.pd.directory.DirectoryHeartbeatMonitor
package pt.isec.pd.directory;

import pt.isec.pd.common.ServerInfo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DirectoryHeartbeatMonitor extends Thread {
    private static final long TIMEOUT_MS = 17000; // 17 segundos - remove servidor
    private final List<ServerInfo> servers;

    public DirectoryHeartbeatMonitor(List<ServerInfo> servers) {
        this.servers = servers;
        setDaemon(true);
    }

    @Override
    public void run() {
        while (true) {
            List<ServerInfo> toRemove = new ArrayList<>();
            List<Long> ages = new ArrayList<>();

            synchronized (servers) {
                Instant now = Instant.now();
                ServerInfo currentPrimary = servers.isEmpty() ? null : servers.get(0);

                for (ServerInfo server : servers) {
                    long age = java.time.Duration.between(server.getLastHeartbeat(), now).toMillis();
                    if (age > TIMEOUT_MS) {
                        toRemove.add(server);
                        ages.add(age);
                    }
                }

                if (!toRemove.isEmpty()) {
                    boolean primaryRemoved = currentPrimary != null && toRemove.contains(currentPrimary);

                    servers.removeAll(toRemove);

                    System.out.println("[Directory] Servidores removidos por inatividade (17s):");
                    for (int i = 0; i < toRemove.size(); i++) {
                        ServerInfo server = toRemove.get(i);
                        long age = ages.get(i);
                        System.out.printf("   [Removed] %s (inativo há %.1fs)%n", server.getKey(), age / 1000.0);
                    }

                    if (primaryRemoved) {
                        if (servers.isEmpty()) {
                            System.out.println("[Directory] Nenhum servidor ativo. Sem principal.");
                        } else {
                            ServerInfo newPrimary = servers.get(0);
                            System.out.println("[Directory] NOVO PRINCIPAL: " + newPrimary.getKey());
                        }
                    }
                }
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
                break;
            }
        }
    }
}