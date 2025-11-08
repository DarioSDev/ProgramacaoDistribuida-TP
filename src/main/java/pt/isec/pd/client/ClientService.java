package pt.isec.pd.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

public class ClientService {

    private final String directoryHost;
    private final int directoryPort;

    public ClientService(String directoryHost, int directoryPort) {
        this.directoryHost = directoryHost;
        this.directoryPort = directoryPort;
    }

    public void start() {
        System.out.println("[Client] Iniciando conexão com o sistema...");

        String[] serverInfo = requestActiveServer();
        if (serverInfo == null) {
            System.err.println("[Client] ❌ Nenhum servidor disponível no Directory.");
            return;
        }

        String serverIp = serverInfo[0];
        int serverPort = Integer.parseInt(serverInfo[1]);

        System.out.printf("[Client] Servidor principal encontrado: %s:%d%n", serverIp, serverPort);

        // usar connectAndCommunicate com retry
        if (connectAndCommunicate(serverInfo[0], Integer.parseInt(serverInfo[1]))) {
            System.out.println("[Client] SUCESSO! Ligação estabelecida com o primary.");
        } else {
            System.err.println("[Client] FALHA após várias tentativas.");
        }
    }

    private boolean connectAndCommunicate(String serverIp, int serverPort) {
        for (int retry = 0; retry < 3; retry++) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(serverIp, serverPort), 5000);
                socket.setSoTimeout(10000);

                try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                    String welcome = in.readLine();
                    if (welcome == null) return false;

                    if (welcome.startsWith("ERRO:")) {
                        System.out.println("[Client] Servidor não é primary. Tentando novamente em 2s... (" + (retry+1) + "/3)");
                        Thread.sleep(2000);
                        // Pedir novo servidor ao Directory
                        String[] newServer = requestActiveServer();
                        if (newServer == null) return false;
                        serverIp = newServer[0];
                        serverPort = Integer.parseInt(newServer[1]);
                        continue;  // tenta com o novo
                    }

                    System.out.println("[Client] Mensagem do servidor: " + welcome);
                    out.println("HELLO_FROM_CLIENT");
                    System.out.println("[Client] Enviado: HELLO_FROM_CLIENT");

                    String confirmation = in.readLine();
                    if (confirmation != null) {
                        System.out.println("[Client] Confirmação do servidor: " + confirmation);
                        return true;
                    }
                }
            } catch (Exception e) {
                System.err.println("[Client] Erro na tentativa " + (retry+1) + ": " + e.getMessage());
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        }
        return false;
    }

    private String[] requestActiveServer() {
        try (DatagramSocket socket = new DatagramSocket()) {
            String msg = "REQUEST_SERVER";
            byte[] buf = msg.getBytes();
            InetAddress dirAddr = InetAddress.getByName(directoryHost);

            DatagramPacket packet = new DatagramPacket(buf, buf.length, dirAddr, directoryPort);
            socket.send(packet);
            System.out.printf("[Client] Pedido '%s' enviado para %s:%d%n", msg, directoryHost, directoryPort);

            socket.setSoTimeout(5000); // aumentei para 5s
            byte[] recvBuf = new byte[256];
            DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(recvPacket);

            String response = new String(recvPacket.getData(), 0, recvPacket.getLength()).trim();
            System.out.println("[Client] Resposta do Directory: '" + response + "'");

            if (response.equals("NO_SERVER_AVAILABLE")) {
                return null;
            }

            String[] parts = response.split("\\s+");
            if (parts.length == 2) {
                try {
                    Integer.parseInt(parts[1]); // validar porto
                    return parts;
                } catch (NumberFormatException e) {
                    System.err.println("[Client] Porto inválido recebido: " + parts[1]);
                    return null;
                }
            } else {
                System.err.println("[Client] Formato inesperado do Directory: " + response);
                return null;
            }

        } catch (SocketTimeoutException e) {
            System.err.println("[Client] Timeout: Directory não respondeu em 5s.");
        } catch (IOException e) {
            System.err.println("[Client] Erro UDP com Directory: " + e.getMessage());
        }
        return null;
    }

    // Método útil para futuro (ex: reconexão automática)
    public void reconnectToServer(String ip, int port) {
        System.out.printf("[Client] Tentando reconectar a %s:%d...%n", ip, port);
        connectAndCommunicate(ip, port);
    }
}