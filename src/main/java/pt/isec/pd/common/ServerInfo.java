package pt.isec.pd.common;

import java.net.InetAddress;
import java.time.Instant;

public class ServerInfo {
    private final InetAddress address;
    private final int tcpPort;
    private Instant lastHeartbeat;

    public ServerInfo(InetAddress address, int tcpPort) {
        this.address = address;
        this.tcpPort = tcpPort;
        this.lastHeartbeat = Instant.now();
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(Instant lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public String getKey() {
        return address.getHostAddress() + ":" + tcpPort;
    }

    @Override
    public String toString() {
        return getKey();
    }
}

