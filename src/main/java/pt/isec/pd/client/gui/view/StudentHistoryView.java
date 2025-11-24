package pt.isec.pd.client.gui.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import pt.isec.pd.client.ClientAPI;
import pt.isec.pd.client.StateManager;
import pt.isec.pd.common.User;

public class StudentHistoryView extends BorderPane {

    private final ClientAPI client;
    private final StateManager stateManager;
    private final User user;

    private static final String COLOR_PRIMARY = "#FF7A00";
    private static final String COLOR_BG = "#1A1A1A";

    public StudentHistoryView(ClientAPI client, StateManager stateManager, User user) {
        this.client = client;
        this.stateManager = stateManager;
        this.user = user;

        setStyle("-fx-background-color: " + COLOR_BG + ";");

        HeaderView header = new HeaderView(stateManager, user);
        setTop(header);

        StackPane root = new StackPane();
        header.attachToRoot(root);

        createContent();
        setCenter(root);
    }

    private void createContent() {
        Label title = new Label("Your History");
        title.setStyle("""
            -fx-font-size: 26px;
            -fx-text-fill: white;
            -fx-font-weight: bold;
        """);

        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(40, 0, 0, 0));

        setCenter(title);
    }
}
