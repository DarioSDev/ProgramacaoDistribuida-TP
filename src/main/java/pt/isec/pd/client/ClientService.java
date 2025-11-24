package pt.isec.pd.client;

import pt.isec.pd.common.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.concurrent.TimeUnit;

public class ClientService {

    private static final int MAX_SAME_SERVER_RETRY = 2; // Máximo de 2 tentativas para o mesmo servidor falhado
    private static final long SAME_SERVER_RETRY_DELAY_MS = 20000; // 20 segundos de espera
    private static final int CONNECTION_TIMEOUT_MS = 5000;

    private final String directoryHost;
    private final int directoryPort;

    // Estado da conexão TCP atual
    private String currentServerIp = null;
    private int currentServerPort = -1;

    // Contagem de falhas para o mesmo IP:PORT
    private int sameServerFailureCount = 0;
    private Socket activeSocket = null;
    private PrintWriter out = null;
    private BufferedReader in = null;
    private volatile boolean running = true;

    public ClientService(String directoryHost, int directoryPort) {
        this.directoryHost = directoryHost;
        this.directoryPort = directoryPort;
    }

    public void start() {
        System.out.println("[Client] Iniciando conexão com o sistema...");

        while (running) {

            // Tenta estabelecer ou restabelecer a sessão
            if (activeSocket == null || activeSocket.isClosed()) {
                if (!tryConnectAndAuthenticate()) {
                    System.err.println("[Client] ❌ Falha crítica: Não foi possível conectar ao servidor principal. A terminar.");
                    break;
                }
            }

            // Se a conexão for bem-sucedida, entra no loop de escuta/sessão
            listenAndMaintainSession();
        }
        closeResources();
        System.out.println("[Client] Cliente encerrado.");
    }

    private boolean tryConnectAndAuthenticate() {
        // O loop principal agora é controlado pela lógica de failover/sleep,
        // não por um contador arbitrário.
        while (running) {
            String[] serverInfo = requestActiveServer();

            if (serverInfo == null) {
                System.err.println("[Client] ❌ Nenhum servidor disponível. Tentando novamente em 5s...");
                sameServerFailureCount = 0;
                sleep(5000);
                continue;
            }

            String newIp = serverInfo[0];
            int newPort = Integer.parseInt(serverInfo[1]);

            boolean sameServer = newIp.equals(currentServerIp) && newPort == currentServerPort;

            // ⚠️ Lógica de Failover Lento (20s)
            if (sameServer && sameServerFailureCount >= 1) {
                sameServerFailureCount++;

                if (sameServerFailureCount > MAX_SAME_SERVER_RETRY) {
                    System.err.println("[Client] ❌ Tentativas esgotadas para o mesmo servidor. Desistindo.");
                    return false;
                }

                System.out.printf("[Client] 🕒 Servidor (%s:%d) é o mesmo que falhou. Esperando %d segundos antes de nova consulta... (%d/%d)%n",
                        newIp, newPort, SAME_SERVER_RETRY_DELAY_MS / 1000, sameServerFailureCount, MAX_SAME_SERVER_RETRY);

                sleep(SAME_SERVER_RETRY_DELAY_MS);
                continue;
            }

            // 3. Tentativa de Conexão TCP Imediata (para Primary Novo ou Reconexão Imediata)
            if (attemptTcpConnection(newIp, newPort)) {
                System.out.printf("[Client] 🟢 Conexão e autenticação bem-sucedidas com %s:%d.%n", newIp, newPort);
                currentServerIp = newIp;
                currentServerPort = newPort;
                sameServerFailureCount = 0;
                return true;
            }

            // 4. Falha na Conexão TCP Imediata
            System.err.println("[Client] ⚠️ Falha na conexão TCP imediata. Assumindo falha do servidor indicado.");

            // Ativa a contagem de falha para forçar a espera de 20s na próxima iteração se o Directory persistir.
            sameServerFailureCount = 1;

            sleep(2000);
        }
        return false;
    }

    private boolean attemptTcpConnection(String ip, int port) {
        closeResources();
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), CONNECTION_TIMEOUT_MS);

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // 1. Receber mensagem de boas-vindas
            String welcome = in.readLine();
            if (welcome == null) throw new IOException("Conexão fechada após estabelecimento.");

            // 2. Tratar rejeição (não é Primary)
            if (welcome.startsWith("ERRO:")) {
                System.err.println("[Client] Servidor rejeitou a conexão: " + welcome);
                socket.close();
                return false;
            }

            System.out.println("[Client] Servidor: " + welcome);

            // 3. Autenticação (ou primeira mensagem)
            out.println(Command.CLIENT_REGISTER_REQUEST);

            System.out.println("[Client] Enviado: " + Command.CLIENT_REGISTER_REQUEST);

            // 4. Receber confirmação
            String confirmation = in.readLine();
            if (confirmation == null) throw new IOException("Servidor fechou após autenticação.");
            System.out.println("[Client] Confirmação: " + confirmation);

            activeSocket = socket;
            return true;

        } catch (IOException e) {
            System.err.printf("[Client] Falha TCP com %s:%d: %s%n", ip, port, e.getMessage());
            return false;
        }
    }

    private void listenAndMaintainSession() {
        boolean hardFailure = false;

        try {
            if (activeSocket != null && !activeSocket.isClosed()) {
                // Set timeout to 0 (blocking read) as per the final server setup
                activeSocket.setSoTimeout(0);
            }

            // Assume que o Servidor mantém a ligação ativa.
            while (running && activeSocket != null && !activeSocket.isClosed()) {
                String serverMsg = in.readLine();

                if (serverMsg == null) {
                    // Servidor encerrou a ligação (EOF)
                    System.out.println("[Client] ℹ️ Servidor fechou a ligação (EOF). Reconectando imediatamente...");
                    break;
                }

                if (!serverMsg.isEmpty()) {
                    System.out.println("[Client] [Mensagem do Servidor] " + serverMsg);
                }
            }

        } catch (IOException e) {
            System.err.println("[Client] 🛑 Conexão TCP perdida inesperadamente: " + e.getMessage() + ". Iniciando Failover Crítico...");
            hardFailure = true;
        } finally {
            closeResources();
            if (hardFailure) {
                // Falha crítica: Ativa a espera de 20s se o Directory der o mesmo Primary
                sameServerFailureCount = 1;
            } else {
                // Encerramento suave (EOF): Tenta reconectar imediatamente (sem espera de 20s)
                sameServerFailureCount = 0;
            }
        }
    }

    private String[] requestActiveServer() {
        try (DatagramSocket socket = new DatagramSocket()) {
            String msg = "REQUEST_SERVER";
            byte[] buf = msg.getBytes();
            InetAddress dirAddr = InetAddress.getByName(directoryHost);

            DatagramPacket packet = new DatagramPacket(buf, buf.length, dirAddr, directoryPort);
            socket.send(packet);
            System.out.printf("[Client] Pedido '%s' enviado para %s:%d%n", msg, directoryHost, directoryPort);

            socket.setSoTimeout(5000);
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
                    Integer.parseInt(parts[1]);
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

    private void closeResources() {
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (activeSocket != null) activeSocket.close(); } catch (IOException ignored) {}
        activeSocket = null;
        in = null;
        out = null;
    }

    private void sleep(long millis) {
        try { TimeUnit.MILLISECONDS.sleep(millis); } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}