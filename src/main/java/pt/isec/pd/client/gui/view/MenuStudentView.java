package pt.isec.pd.client.gui.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import pt.isec.pd.client.ClientAPI;
import pt.isec.pd.client.StateManager;
import pt.isec.pd.common.User;

public class MenuStudentView extends BorderPane {

    private final ClientAPI client;
    private final StateManager stateManager;
    private final User user;

    private static final String COLOR_BG = "#1A1A1A";
    private static final String COLOR_PRIMARY = "#FF7A00";

    public MenuStudentView(ClientAPI client, StateManager stateManager, User user) {
        this.client = client;
        this.stateManager = stateManager;
        this.user = user;

        setStyle("-fx-background-color: " + COLOR_BG + ";");

        BorderPane layout = new BorderPane();

        HeaderStudentView header = new HeaderStudentView(stateManager, user);
        layout.setTop(header);

        VBox center = createCenterContent();
        layout.setCenter(center);

        StackPane root = new StackPane(layout);
        header.attachToRoot(root);

        this.setCenter(root);
    }

    private VBox createCenterContent() {
        VBox body = new VBox(20);
        body.setAlignment(Pos.CENTER);

        double ELEMENT_WIDTH = 360;
        double FONT_SIZE = 18;
        double RADIUS = 12;

        double ELEMENT_HEIGHT = 50;

        TextField codeField = new TextField();
        codeField.setPromptText("Question Code");
        codeField.setPrefHeight(ELEMENT_HEIGHT);

        codeField.setStyle("""
            -fx-background-color: white;
            -fx-text-fill: black;
            -fx-font-size: %fpx;
            -fx-background-radius: %f;
            -fx-padding: 0 50 0 20;
            -fx-prompt-text-fill: #999;
        """.formatted(FONT_SIZE, RADIUS));

        Button btnClear = new Button();
        SVGPath iconX = new SVGPath();
        iconX.setContent("M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z");
        iconX.setFill(Color.BLACK);
        iconX.setScaleX(0.7);
        iconX.setScaleY(0.7);

        btnClear.setGraphic(iconX);
        btnClear.setCursor(Cursor.HAND);
        btnClear.setStyle("-fx-background-color: transparent;");
        btnClear.setVisible(false);

        btnClear.setOnAction(e -> codeField.clear());
        codeField.textProperty().addListener((obs, oldVal, newVal) -> btnClear.setVisible(!newVal.isEmpty()));

        StackPane inputContainer = new StackPane(codeField, btnClear);
        inputContainer.setMaxWidth(ELEMENT_WIDTH);
        StackPane.setAlignment(btnClear, Pos.CENTER_RIGHT);
        StackPane.setMargin(btnClear, new Insets(0, 5, 0, 0));

        Button btnHistory = new Button("View History");
        btnHistory.setCursor(Cursor.HAND);
        btnHistory.setPrefWidth(ELEMENT_WIDTH);

        SVGPath histIcon = new SVGPath();
        histIcon.setContent(
                "M5.01113 11.5747L6.29289 10.2929C6.68342 9.90236 7.31658 9.90236 7.70711 10.2929" +
                        "C8.09763 10.6834 8.09763 11.3166 7.70711 11.7071L4.70711 14.7071" +
                        "C4.51957 14.8946 4.26522 15 4 15C3.73478 15 3.48043 14.8946 3.29289 14.7071" +
                        "L0.292893 11.7071C-0.0976312 11.3166 -0.0976312 10.6834 0.292893 10.2929" +
                        "C0.683417 9.90236 1.31658 9.90236 1.70711 10.2929L3.00811 11.5939" +
                        "C3.22118 6.25933 7.61318 2 13 2C18.5229 2 23 6.47715 23 12" +
                        "C23 17.5228 18.5229 22 13 22C9.85818 22 7.0543 20.5499 5.22264 18.2864" +
                        "C4.87523 17.8571 4.94164 17.2274 5.37097 16.88C5.80029 16.5326 6.42997 16.599 6.77738 17.0283" +
                        "C8.24563 18.8427 10.4873 20 13 20C17.4183 20 21 16.4183 21 12" +
                        "C21 7.58172 17.4183 4 13 4C8.72442 4 5.23222 7.35412 5.01113 11.5747ZM13 5" +
                        "C13.5523 5 14 5.44772 14 6V11.5858L16.7071 14.2929C17.0976 14.6834 17.0976 15.3166 16.7071 15.7071" +
                        "C16.3166 16.0976 15.6834 16.0976 15.2929 15.7071L12.2929 12.7071" +
                        "C12.1054 12.5196 12 12.2652 12 12V6C12 5.44772 12.4477 5 13 5Z"
        );
        histIcon.setFill(Color.BLACK);
        histIcon.setScaleX(1.0);
        histIcon.setScaleY(1.0);

        btnHistory.setGraphic(histIcon);

        btnHistory.setGraphicTextGap(40);

        btnHistory.setStyle("""
            -fx-font-size: %fpx;
            -fx-font-weight: bold;
            -fx-background-color: %s;
            -fx-text-fill: black;
            -fx-background-radius: %f;
            -fx-padding: 10 40;  
        """.formatted(FONT_SIZE, COLOR_PRIMARY, RADIUS));

        btnHistory.setOnAction(e -> stateManager.showStudentHistory(user));

        body.getChildren().addAll(inputContainer, btnHistory);

        return body;
    }
}