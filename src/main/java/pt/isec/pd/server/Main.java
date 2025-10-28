package pt.isec.pd.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "pt.isec.pd")
public class Main {
    public static void main(String[] args) {
        System.out.printf("Hello server!");
        SpringApplication.run(Main.class, args);
    }
}
