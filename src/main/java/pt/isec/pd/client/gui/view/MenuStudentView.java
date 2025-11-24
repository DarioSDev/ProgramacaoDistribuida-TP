package pt.isec.pd.client.gui.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
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

    private StackPane root;

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

        root = new StackPane(layout);
        header.attachToRoot(root);

        setCenter(root);
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
        iconX.setScaleX(1.0);
        iconX.setScaleY(1.0);

        btnClear.setGraphic(iconX);
        btnClear.setStyle("-fx-background-color: transparent;");
        btnClear.setCursor(Cursor.HAND);
        btnClear.setVisible(false);

        btnClear.setOnAction(e -> codeField.clear());
        codeField.textProperty().addListener((o, ov, nv) -> btnClear.setVisible(!nv.isEmpty()));

        StackPane inputContainer = new StackPane(codeField, btnClear);
        inputContainer.setMaxWidth(ELEMENT_WIDTH);

        StackPane.setAlignment(btnClear, Pos.CENTER_RIGHT);
        StackPane.setMargin(btnClear, new Insets(0, 5, 0, 0));

        codeField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                submitCode(codeField.getText());
            }
        });

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

    private void submitCode(String code) {
        if (code == null || code.isBlank()) {
            showPopup("The code provided is invalid!");
            return;
        }

        String validation = client.validateQuestionCode(code.trim());

        if (!"VALID".equals(validation)) {
            showPopup("The code provided is invalid!");
            return;
        }

        stateManager.showQuestionView(user, code.trim());
    }

    private void showPopup(String text) {

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.45);");

        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setStyle("""
            -fx-background-color: #D9D9D9;
            -fx-border-color: #FF0000;
            -fx-border-width: 3px;
            -fx-background-radius: 14px;
            -fx-border-radius: 14px;
        """);

        box.setPrefWidth(420);
        box.setPrefHeight(260);
        box.setMaxWidth(420);
        box.setMaxHeight(260);

        SVGPath icon = new SVGPath();
        icon.setContent("M32.3039 32.5965L19.1207 6.21369C18.0213 4.92877 16.2374 4.92877 15.138 6.21369L1.9537 32.5965C0.854302 33.8802 0.854302 35.9638 1.9537 37.25H32.3029C33.4044 35.9638 33.4044 33.8802 32.3039 32.5965ZM15.9152 15.6845C15.9152 14.6642 16.6228 13.8383 17.4948 13.8383C18.3667 13.8383 19.0744 14.6642 19.0744 15.6845V24.2998C19.0744 25.3189 18.3667 26.146 17.4948 26.146C16.6228 26.146 15.9152 25.3189 15.9152 24.2998V15.6845ZM17.5042 32.3097C16.6323 32.3097 15.9246 31.4851 15.9246 30.4635C15.9246 29.4445 16.6323 28.6174 17.5042 28.6174C18.3762 28.6174 19.0838 29.4445 19.0838 30.4635C19.0838 31.4851 18.3762 32.3097 17.5042 32.3097Z");
        icon.setFill(Color.RED);

        icon.setScaleX(1.5);
        icon.setScaleY(1.5);

        Label msg = new Label(text);
        msg.setStyle("-fx-font-size: 19px; -fx-text-fill: black;");

        Button ok = new Button("Ok");
        ok.setPrefWidth(100);
        ok.setStyle("""
            -fx-background-color: #FF7A00;
            -fx-text-fill: black;
            -fx-font-size: 16px;
            -fx-background-radius: 10px;
            -fx-border-color: #2C2C2C;
            -fx-border-width: 1px;
            -fx-border-radius: 10px;
        """);

        ok.setOnAction(e -> root.getChildren().remove(overlay));

        box.getChildren().addAll(icon, msg, ok);

        overlay.getChildren().add(box);
        StackPane.setAlignment(box, Pos.CENTER);

        root.getChildren().add(overlay);
    }

}
