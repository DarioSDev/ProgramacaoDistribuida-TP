package pt.isec.pd.client;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainJavaFx extends Application {

    ClientService client;

    @Override
    public void init() throws Exception {
        super.init();
        Parameters args = getParameters();
        if (args.getRaw().size() != 2) {
            System.err.println("Uso: java -jar client.jar <IP_DIRETORIA> <PORTO_UDP>");
            System.exit(1);
        }

        String directoryHost = args.getRaw().get(0);
        int directoryPort = Integer.parseInt(args.getRaw().get(1));

        client = new ClientService(directoryHost, directoryPort);
        client.start();
    }

    @Override
    public void start(Stage stage) {

        boolean USE_REAL_SERVER = true;

        ClientAPI clientAPI;

        if (USE_REAL_SERVER) {
            var params = getParameters();
            String directoryHost = params.getRaw().get(0);
            int directoryPort = Integer.parseInt(params.getRaw().get(1));

            ClientService real = new ClientService(directoryHost, directoryPort);

            Thread t = new Thread(real::start);
            t.setDaemon(true);
            t.start();

            clientAPI = real;

        } else {
            clientAPI = new ClientServiceMock();
        }

        StateManager sm = new StateManager(stage, clientAPI);

        stage.setWidth(900);
        stage.setHeight(700);
        stage.setMinWidth(900);
        stage.setMinHeight(700);

        sm.showLogin();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
