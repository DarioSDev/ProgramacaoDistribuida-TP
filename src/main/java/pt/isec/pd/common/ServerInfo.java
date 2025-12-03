package pt.isec.pd.common;

import java.net.InetAddress;
import java.time.Instant;

public class ServerInfo {
    private final InetAddress address;
    private final int tcpClientPort;
    private final int tcpDbPort;
    private final int udpPort;
    private final long registrationTime;  // ← NOVO: timestamp do registo
    private java.time.Instant lastHeartbeat;

    public ServerInfo(InetAddress address, int tcpClientPort, int tcpDbPort, int udpPort) {
        this.address = address;
        this.tcpClientPort = tcpClientPort;
        this.tcpDbPort = tcpDbPort;
        this.udpPort = udpPort;
        this.registrationTime = System.currentTimeMillis();  // ← agora!
        this.lastHeartbeat = java.time.Instant.now();
    }

    public InetAddress getAddress() { return address; }
    public int getTcpClientPort() { return tcpClientPort; }
    public int getTcpDbPort() { return tcpDbPort; }
    public int getUdpPort() { return udpPort; }
    public Instant getLastHeartbeat() { return lastHeartbeat; }
    public long getRegistrationTime() { return registrationTime; }

    public void setLastHeartbeat(Instant lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public String getKey() {
        return address.getHostAddress() + ":" + tcpClientPort;
    }

    @Override
    public String toString() {
        return String.format("%s:%d (client) | BD:%d | UDP:%d | reg:%d",
                address.getHostAddress(), tcpClientPort, tcpDbPort, udpPort, registrationTime);
    }
}

