package pt.isec.pd.common;

import java.net.InetAddress;
import java.time.Instant;

public class ServerInfo {
    private final InetAddress address;
    private final int tcpClientPort;    // para clientes
    private final int tcpDbPort;        // para sync BD
    private final int udpPort;          // para heartbeat do Directory
    private Instant lastHeartbeat;

    public ServerInfo(InetAddress address, int tcpClientPort, int tcpDbPort, int udpPort) {
        this.address = address;
        this.tcpClientPort = tcpClientPort;
        this.tcpDbPort = tcpDbPort;
        this.udpPort = udpPort;
        this.lastHeartbeat = Instant.now();
    }

    public InetAddress getAddress() { return address; }
    public int getTcpClientPort() { return tcpClientPort; }
    public int getTcpDbPort() { return tcpDbPort; }
    public int getUdpPort() { return udpPort; }
    public Instant getLastHeartbeat() { return lastHeartbeat; }

    public void setLastHeartbeat(Instant lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public String getKey() {
        return address.getHostAddress() + ":" + tcpClientPort;
    }

    @Override
    public String toString() {
        return String.format("%s (client:%d, db:%d)", getKey(), tcpClientPort, tcpDbPort);
    }
}

