package pt.isec.pd.client;
import pt.isec.pd.common.MessageType;

import javafx.application.Application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Main {
//    public static void main(String[] args) {
//        Application.launch(MainJavaFx.class, args);
//    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java -jar client.jar <IP_DIRETORIA> <PORTO_UDP>");
            return;
        }

        String directoryHost = args[0];
        int directoryPort = Integer.parseInt(args[1]);

        ClientService client = new ClientService(directoryHost, directoryPort);
        client.start();
    }
}
