package pt.isec.pd.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainJavaFx extends Application {
    @Override
    public void start(Stage stage) {
        Label label = new Label("Client App");
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 300, 200);
        stage.setTitle("Client | TP PD");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
