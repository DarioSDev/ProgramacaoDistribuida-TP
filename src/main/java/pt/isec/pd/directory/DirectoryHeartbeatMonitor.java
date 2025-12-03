// pt.isec.pd.directory.DirectoryHeartbeatMonitor
package pt.isec.pd.directory;

import pt.isec.pd.common.ServerInfo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class DirectoryHeartbeatMonitor extends Thread {
    private static final long TIMEOUT_MS = 17000; // 17 segundos - tempo limite
    private static final long CHECK_INTERVAL_MS = 5000; // Intervalo de verificação

    // NOTA: 'servers' deve ser o lock para todas as operações de leitura/escrita.
    private final List<ServerInfo> servers;
    private volatile boolean running = true; // Para um encerramento limpo
    private final DirectoryService directoryService;

    public DirectoryHeartbeatMonitor(List<ServerInfo> servers, DirectoryService directoryService) {
        this.servers = servers;
        setDaemon(true);
        setName("Heartbeat-Monitor");
        this.directoryService = directoryService;
    }

    @Override
    public void run() {
        while (running) {
            List<ServerInfo> toRemove = new ArrayList<>();
            List<Long> ages = new ArrayList<>();

            // Acesso sincronizado à lista partilhada
            synchronized (servers) {
                Instant now = Instant.now();
                ServerInfo currentPrimary = servers.isEmpty() ? null : servers.get(0);

                // 1. Identificar servidores inativos
                for (ServerInfo server : servers) {
                    // Calcula a idade com a API java.time
                    long age = server.getLastHeartbeat().until(now, ChronoUnit.MILLIS);

                    if (age > TIMEOUT_MS) {
                        toRemove.add(server);
                        ages.add(age);
                    }
                }

                if (!toRemove.isEmpty()) {
                    boolean primaryRemoved = currentPrimary != null && toRemove.contains(currentPrimary);

                    // 2. Remover servidores
                    servers.removeAll(toRemove);

                    System.out.println("\n[Directory Monitor] Servidores removidos por inatividade:");
                    for (int i = 0; i < toRemove.size(); i++) {
                        ServerInfo server = toRemove.get(i);
                        long age = ages.get(i);
                        System.out.printf("   ❌ [Removed] %s (inativo há %.1fs)%n", server.getKey(), age / 1000.0);
                    }

                    // 3. Promover novo Principal, se necessário
                    if (primaryRemoved && !servers.isEmpty()) {
                        ServerInfo newPrimary = servers.get(0);
                        System.out.println("   👑 [NEW PRIMARY] Promovido: " + newPrimary.getKey());
                        directoryService.notifyAllServersAboutNewPrimary();
                    } else if (primaryRemoved && servers.isEmpty()) {
                        System.out.println("[Directory Monitor] Nenhum servidor ativo. Sem principal.");
                    }
                }
            } // Fim do synchronized

            try {
                Thread.sleep(CHECK_INTERVAL_MS);
            } catch (InterruptedException e) {
                // Interrompido (shutdown externo)
                Thread.currentThread().interrupt();
                running = false;
            }
        }
        System.out.println("[Directory Monitor] Encerrado.");
    }

    // Novo método para encerramento limpo
    public void shutdown() {
        this.running = false;
        this.interrupt();
    }
}