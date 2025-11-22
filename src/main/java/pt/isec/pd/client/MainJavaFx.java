package pt.isec.pd.client;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainJavaFx extends Application {

    @Override
    public void start(Stage stage) {

        boolean USE_REAL_SERVER = false;

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
