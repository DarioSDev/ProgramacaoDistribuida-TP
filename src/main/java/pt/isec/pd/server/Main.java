package pt.isec.pd.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "pt.isec.pd")
public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("Hello server!");

            if (args.length < 2) {
                System.out.println("Uso: java pt.isec.pd.server.Main <portoTCP> <portoDiretoria>");
                return;
            }

            int tcpPort = Integer.parseInt(args[0]);
            int directoryPort = Integer.parseInt(args[1]);

            ServerService serverService = new ServerService("127.0.0.1", directoryPort, tcpPort);
            serverService.start();

            // Se quisermos ativar o Spring Boot no futuro:
            // SpringApplication.run(Main.class, args);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
