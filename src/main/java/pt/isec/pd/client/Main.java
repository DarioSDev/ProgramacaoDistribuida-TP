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
    public static void main(String[] args) {
        Application.launch(MainJavaFx.class, args);
    }

}
