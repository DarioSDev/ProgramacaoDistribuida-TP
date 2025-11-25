package pt.isec.pd.client.gui.view.teacher;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import pt.isec.pd.client.ClientAPI;
import pt.isec.pd.client.StateManager;
import pt.isec.pd.client.gui.view.student.HeaderStudentView;
import pt.isec.pd.common.User;

import java.util.List;

public class QuestionView extends BorderPane {

    private final ClientAPI client;
    private final StateManager stateManager;
    private final User user;
    private final String code;

    private static final String COLOR_BG = "#1A1A1A";
    private static final String COLOR_PRIMARY = "#FF7A00";
    private static final String COLOR_BUTTON_TEXT = "#000000";

    private Button btnSubmit;
    private StackPane root;
    private final ToggleGroup toggleGroup = new ToggleGroup();

    private static final String SVG_UPLOAD =
            "M9 16h6v-6h4l-7-7-7 7h4v6zm-4 2h14v2H5z";

    private static final String SVG_BACK =
            "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z";

    private static final String SVG_CHECK_CIRCLE =
            "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z";

    private static final String SVG_CROSS_CIRCLE =
            "M12 2C6.47 2 2 6.47 2 12s4.47 10 10 10 10-4.47 10-10S17.53 2 12 2zm5 13.59L15.59 17 12 13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z";

    public QuestionView(ClientAPI client, StateManager stateManager, User user, String code) {
        this.client = client;
        this.stateManager = stateManager;
        this.user = user;
        this.code = code;

        setStyle("-fx-background-color: " + COLOR_BG + ";");

        BorderPane layout = new BorderPane();
        HeaderStudentView header = new HeaderStudentView(stateManager, user);
        layout.setTop(header);

        VBox questionContent = createQuestionArea();

        StackPane centerContainer = new StackPane(questionContent);
        centerContainer.setAlignment(Pos.TOP_CENTER);
        centerContainer.setPadding(new Insets(0, 0, 20, 0));

        layout.setCenter(centerContainer);

        root = new StackPane(layout);
        header.attachToRoot(root);

        setCenter(root);
    }

    private VBox createQuestionArea() {
        VBox content = new VBox(25);
        content.setAlignment(Pos.TOP_LEFT);
        content.setPadding(new Insets(50, 20, 40, 20));
        content.setMaxWidth(1000);

        ClientAPI.QuestionData data = client.getQuestionByCode(code);

        String questionTextStr = (data != null ? data.text() : "Error loading question.");
        List<String> optionsList = (data != null ? data.options() : List.of("Option 1", "Option 2"));

        Label title = new Label("Question");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label questionText = new Label(questionTextStr);
        questionText.setWrapText(true);
        questionText.setMaxWidth(960);
        questionText.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

        VBox questionBox = new VBox(10, title, questionText);
        VBox optionsBox = new VBox(10);
        char letter = 'a';

        for (String opt : optionsList) {
            RadioButton rb = new RadioButton(letter + ") " + opt);
            rb.setToggleGroup(toggleGroup);
            rb.setStyle(String.format("-fx-text-fill: white; -fx-font-size: 13px; -fx-mark-color: %s; -fx-accent: %s;", COLOR_PRIMARY, COLOR_PRIMARY));
            optionsBox.getChildren().add(rb);
            letter++;
        }

        btnSubmit = new Button("Submit");
        Button btnBack = new Button("Back");
        styleMainButton(btnSubmit);
        styleMainButton(btnBack);
        btnSubmit.setPrefWidth(260);
        btnBack.setPrefWidth(260);

        btnSubmit.setGraphic(createUploadIcon());
        btnSubmit.setGraphicTextGap(20);
        btnBack.setGraphic(createBackIcon());
        btnBack.setGraphicTextGap(30);

        btnSubmit.setOnAction(e -> submitAnswer());
        btnBack.setOnAction(e -> stateManager.showStudentMenu(user));

        VBox buttons = new VBox(20, btnSubmit, btnBack);
        buttons.setAlignment(Pos.CENTER);
        HBox buttonsContainer = new HBox(buttons);
        buttonsContainer.setAlignment(Pos.TOP_CENTER);
        buttonsContainer.setPadding(new Insets(40, 0, 0, 0));

        content.getChildren().addAll(questionBox, optionsBox, buttonsContainer);

        return content;
    }

    private void submitAnswer() {
        RadioButton selected = (RadioButton) toggleGroup.getSelectedToggle();
        if (selected == null) return;

        int index = selected.getText().charAt(0) - 'a';

        boolean isCorrect = client.submitAnswer(user, code, index);

        btnSubmit.setDisable(true);
        btnSubmit.setStyle("""
            -fx-background-color: #BDBDBD;
            -fx-text-fill: black;
            -fx-font-size: 16px;
            -fx-font-weight: bold;
            -fx-background-radius: 12;
            -fx-padding: 4 28;
            -fx-alignment: CENTER;
        """);

        showSubmittedPopup(isCorrect);
    }

    private void showSubmittedPopup(boolean isCorrect) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.45);");

        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setPrefSize(360, 150);
        box.setMaxSize(360, 150);

        box.setStyle("""
                -fx-background-color: #D9D9D9;
                -fx-background-radius: 14;
                -fx-border-color: #FF7A00;
                -fx-border-width: 3;
                -fx-border-radius: 14;
        """);

        SVGPath check = new SVGPath();

        check.setContent("M16 2.66675C8.636 2.66675 2.66675 8.636 2.66675 16C2.66675 23.364 8.636 29.3333 16 29.3333C23.364 29.3333 29.3333 23.364 29.3333 16C29.3333 8.636 23.364 2.66675 16 2.66675ZM14.4 22.4L9.46675 17.4667L11.3334 15.6L14.2667 18.5334L22 10.4L24 12.4L14.4 22.4Z");
        check.setFill(Color.BLACK);
        check.setScaleX(0.8);
        check.setScaleY(0.8);

        Label msg = new Label("Answer Submitted!");
        msg.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: black;");

        box.getChildren().addAll(check, msg);
        overlay.getChildren().add(box);
        root.getChildren().add(overlay);

        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                root.getChildren().remove(overlay);
                showResultPopup(isCorrect);
            }
        });
    }

    private void showResultPopup(boolean isCorrect) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.6);");

        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setPrefSize(360, 150);
        box.setMaxSize(360, 150);

        box.setStyle("""
            -fx-background-color: #D9D9D9;
            -fx-background-radius: 14;
            -fx-border-color: #FF7A00;
            -fx-border-width: 3;
            -fx-border-radius: 14;
        """);

        SVGPath icon = new SVGPath();
        Label msg = new Label();

        if (isCorrect) {
            icon.setContent(SVG_CHECK_CIRCLE);
            icon.setFill(Color.GREEN
            );
            msg.setText("Correct Answer!");
        } else {
            icon.setContent(SVG_CROSS_CIRCLE);
            icon.setFill(Color.RED);
            msg.setText("Wrong Answer!");
        }

        icon.setScaleX(1.5);
        icon.setScaleY(1.5);

        msg.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: black; -fx-padding: 10 0 0 0;");

        box.getChildren().addAll(icon, msg);
        overlay.getChildren().add(box);
        root.getChildren().add(overlay);

        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                root.getChildren().remove(overlay);
            }
        });
    }

    private void styleMainButton(Button btn) {
        btn.setCursor(Cursor.HAND);
        btn.setStyle(String.format("""
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-font-size: 16px;
                -fx-font-weight: bold;
                -fx-background-radius: 14;
                -fx-padding: 4 28;
                -fx-alignment: CENTER;
                """, COLOR_PRIMARY, COLOR_BUTTON_TEXT));
        btn.setPrefHeight(38);
    }

    private SVGPath createUploadIcon() {
        SVGPath svg = new SVGPath();
        svg.setContent(SVG_UPLOAD);
        svg.setFill(Color.BLACK);
        svg.setScaleX(1.1);
        svg.setScaleY(1.1);
        return svg;
    }

    private SVGPath createBackIcon() {
        SVGPath svg = new SVGPath();
        svg.setContent(SVG_BACK);
        svg.setFill(Color.BLACK);
        svg.setScaleX(1.1);
        svg.setScaleY(1.1);
        return svg;
    }
}