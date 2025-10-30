// pt.isec.pd.server.Main.java
package pt.isec.pd.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Scanner;

@SpringBootApplication(scanBasePackages = "pt.isec.pd")
public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("Iniciando servidor...");

            // Verificar argumentos
            if (args.length < 2) {
                System.err.println("Uso: java -jar server.jar <IP_DIRETORIA>:<PORTA_UDP> <IP_MULTICAST>");
                System.err.println("Exemplo: java -cp server.jar pt.isec.pd.server.Main 127.0.0.1:9000 239.1.1.1");
                return;
            }

            // Parse: <IP>:<PORTA>
            String[] dirParts = args[0].split(":");
            if (dirParts.length != 2) {
                System.err.println("Formato inválido para diretoria: <IP>:<PORTA>");
                return;
            }

            String directoryHost = dirParts[0];
            int directoryPort;
            try {
                directoryPort = Integer.parseInt(dirParts[1]);
            } catch (NumberFormatException e) {
                System.err.println("Porta UDP inválida: " + dirParts[1]);
                return;
            }

            String multicastGroupIp = args[1];

            // Iniciar o ServerService com os parâmetros corretos
            ServerService serverService = new ServerService(directoryHost, directoryPort, multicastGroupIp);
            serverService.start();

            // Teste para fazer o server anunciar que vai encerrar
            new Scanner(System.in).nextLine();
            serverService.shutdown();

            // Opcional: ativar Spring Boot no futuro (REST API, etc.)
            // SpringApplication.run(Main.class, args);

        } catch (Exception e) {
            System.err.println("Erro fatal no servidor:");
            e.printStackTrace();
        }
    }
}