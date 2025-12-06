package pt.isec.pd.client.gui.view.student;

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
import pt.isec.pd.common.User;

import java.io.IOException;
import java.util.List;

public class QuestionView extends BorderPane {

    private final ClientAPI client;
    private final StateManager stateManager;
    private final User user;
    private final String code;

    private StackPane root;
    private Button btnSubmit;
    private final ToggleGroup toggleGroup = new ToggleGroup();

    private static final String COLOR_BG = "#1A1A1A";
    private static final String COLOR_PRIMARY = "#FF7A00";
    private static final String COLOR_BUTTON_TEXT = "#000000";

    private static final String SVG_UPLOAD =
            "M17.8622 6.13763L12.7717 1.047C12.7461 1.02145 12.7193 0.997407 12.6914 0.974485C12.6793 0.964501 12.6662 0.955642 12.6536 0.946173C12.6376 0.934267 12.622 0.921938 12.6057 0.91097C12.5904 0.900798 12.5743 0.891845 12.5586 0.882376C12.5439 0.873517 12.5293 0.864188 12.5142 0.856126C12.4981 0.847548 12.4815 0.840188 12.4649 0.83236C12.4491 0.824954 12.4335 0.817032 12.4176 0.810376C12.4016 0.803767 12.385 0.798376 12.3687 0.79247C12.3514 0.786235 12.3342 0.779579 12.3167 0.774282C12.3005 0.769313 12.2837 0.765704 12.2671 0.761532C12.2492 0.757032 12.2314 0.752017 12.2132 0.74836C12.194 0.744517 12.1743 0.742173 12.1548 0.739407C12.139 0.737157 12.1235 0.734157 12.1077 0.732563C12.0723 0.729142 12.0364 0.727173 12 0.727173C11.9636 0.727173 11.9277 0.729142 11.8922 0.73261C11.8769 0.734157 11.8616 0.737063 11.8462 0.73922C11.8263 0.742079 11.8063 0.74447 11.7867 0.748313C11.7691 0.751782 11.7517 0.756751 11.7343 0.76111C11.7172 0.765423 11.6998 0.769173 11.6831 0.774282C11.6661 0.779438 11.6495 0.785907 11.6329 0.79186C11.616 0.797907 11.5988 0.803485 11.5825 0.810329C11.567 0.816751 11.5519 0.824438 11.5367 0.831563C11.5196 0.839626 11.5023 0.847267 11.4857 0.856126C11.4713 0.863767 11.4575 0.872767 11.4434 0.881157C11.4269 0.890954 11.4101 0.900423 11.3941 0.911063C11.3789 0.921329 11.3643 0.93286 11.3494 0.944017C11.3357 0.954188 11.3217 0.963798 11.3084 0.974579C11.2823 0.996048 11.257 1.01902 11.2325 1.0433C11.2311 1.04447 11.2299 1.0455 11.2287 1.04663L6.13762 6.13758C5.71166 6.56363 5.71157 7.25433 6.13762 7.68033C6.56357 8.10638 7.25437 8.10628 7.68037 7.68042L10.9091 4.45186V17.8181C10.9091 18.4206 11.3976 18.909 12 18.909C12.6025 18.909 13.091 18.4206 13.091 17.8181V4.45191L16.3195 7.68042C16.5325 7.89342 16.8117 7.99997 17.0908 7.99997C17.37 7.99997 17.6492 7.89342 17.8622 7.68042C18.2883 7.25438 18.2883 6.56363 17.8622 6.13763Z " +
                    "M22.9091 11.6364C22.3066 11.6364 21.8182 12.1248 21.8182 12.7273V21.0909H2.1818V12.7273C2.1818 12.1248 1.69336 11.6364 1.09087 11.6364C0.488391 11.6364 0 12.1248 0 12.7273V22.1818C0 22.7843 0.488438 23.2727 1.09092 23.2727H22.9091C23.5116 23.2727 24 22.7843 24 22.1818V12.7273C24 12.1248 23.5116 11.6364 22.9091 11.6364Z";

    private static final String SVG_BACK =
            "M0 8.50732C0 8.06201 0.182454 7.65381 0.547363 7.28271L7.57031 0.41748C7.71257 0.281413 7.86719 0.17627 8.03418 0.102051C8.20736 0.0340169 8.3929 0 8.59082 0C8.86914 0 9.11963 0.0649414 9.34229 0.194824C9.57113 0.330892 9.75049 0.510254 9.88037 0.73291C10.0164 0.955566 10.0845 1.20296 10.0845 1.4751C10.0845 1.88949 9.92676 2.25439 9.61133 2.56982L3.10791 8.8877V8.13623L9.61133 14.4355C9.92676 14.751 10.0845 15.1128 10.0845 15.521C10.0845 15.7931 10.0164 16.0405 9.88037 16.2632C9.75049 16.4858 9.57113 16.6621 9.34229 16.792C9.11963 16.9281 8.86914 16.9961 8.59082 16.9961C8.1888 16.9961 7.84863 16.8569 7.57031 16.5786L0.547363 9.72266C0.361816 9.53711 0.225749 9.34538 0.139160 9.14746C0.0525716 8.94954 0.0061849 8.73617 0 8.50732Z";

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

        content.setPadding(new Insets(30, 60, 30, 60));
        content.setMaxWidth(1000);

        // [R24] TODO APENAS PODE REQUISITAR A PERGUNTA DENTRO DO PRAZO
        // [R25] TODO APENAS PODE REQUISITAR A PERGUNTA DENTRO DO PRAZO
        ClientAPI.QuestionData data = client.getQuestionByCode(code);

        String questionTextStr = (data != null ? data.text() : "Error loading question.");
        List<String> optionsList = (data != null ? data.options() : List.of("Option 1", "Option 2"));

        Label title = new Label("Question");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: bold;");

        Label questionText = new Label(questionTextStr);
        questionText.setWrapText(true);

        questionText.setMaxWidth(840);

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

        styleButton(btnSubmit, createUploadIcon());
        btnSubmit.setGraphicTextGap(35);

        styleButton(btnBack, createBackIcon());

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

        try {
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

            showSubmittedPopup();
        } catch (IOException e) {
            System.out.println("Error submitting answer: " + e.getMessage());
        }

    }

    private void showSubmittedPopup() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.45);");

        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setPrefSize(400, 150);
        box.setMaxSize(400, 150);

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

        Label msg = new Label("Answer submitted. Click to go back to main menu");
        msg.setWrapText(true);
        msg.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: black; -fx-alignment: center;");

        box.getChildren().addAll(check, msg);
        overlay.getChildren().add(box);
        root.getChildren().add(overlay);

        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                root.getChildren().remove(overlay);
                stateManager.showStudentMenu(user);
            }
        });
    }

    private void styleButton(Button btn, SVGPath icon) {
        btn.setCursor(Cursor.HAND);
        btn.setPrefWidth(260);
        btn.setPrefHeight(40);

        icon.setFill(Color.BLACK);
        icon.setScaleX(0.8);
        icon.setScaleY(0.8);

        btn.setGraphic(icon);
        btn.setGraphicTextGap(60);

        btn.setStyle("""
            -fx-background-color: #FF7A00;
            -fx-text-fill: black;
            -fx-font-size: 16px;
            -fx-font-weight: bold;
            -fx-background-radius: 12;
            -fx-alignment: CENTER_LEFT;
            -fx-padding: 0 0 0 35;
        """);
    }

    private SVGPath createUploadIcon() {
        SVGPath svg = new SVGPath();
        svg.setContent(SVG_UPLOAD);
        return svg;
    }

    private SVGPath createBackIcon() {
        SVGPath svg = new SVGPath();
        svg.setContent(SVG_BACK);
        return svg;
    }
}