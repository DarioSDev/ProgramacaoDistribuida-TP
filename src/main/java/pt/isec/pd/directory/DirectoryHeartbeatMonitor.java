package pt.isec.pd.directory;

import pt.isec.pd.common.ServerInfo;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;

public class DirectoryHeartbeatMonitor extends Thread {
    private final List<ServerInfo> servers;
    private static final long TIMEOUT_SECONDS = 17;

    public DirectoryHeartbeatMonitor(List<ServerInfo> servers) {
        this.servers = servers;
        setDaemon(true);
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(5000);
                Instant now = Instant.now();

                servers.removeIf(s -> {
                    boolean expired = Duration.between(s.getLastHeartbeat(), now).getSeconds() > TIMEOUT_SECONDS;
                    if (expired)
                        System.out.println("[Directory] Servidor removido (timeout): " + s);
                    return expired;
                });

            } catch (InterruptedException ignored) {}
        }
    }
}

