package pt.isec.pd.client.gui.view.teacher;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import pt.isec.pd.client.ClientAPI;
import pt.isec.pd.client.StateManager;
import pt.isec.pd.common.Question;
import pt.isec.pd.common.User;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class EditQuestionView extends BorderPane {

    private final ClientAPI client;
    private final StateManager stateManager;
    private final User user;
    private final Question question;

    private static final String COLOR_PRIMARY = "#FF7A00";
    private static final String COLOR_BG = "#1A1A1A";
    private static final String COLOR_BUTTON_TEXT = "#000000";

    private static final String SVG_PLUS_CIRCLE =
            "M11 17H13V13H17V11H13V7H11V11H7V13H11V17ZM12 22C10.6167 22 9.31667 21.7417 8.1 21.225C6.88333 20.6917 5.825 19.975 4.925 19.075C4.025 18.175 3.30833 17.1167 2.775 15.9C2.25833 14.6833 2 13.3833 2 12C2 10.6167 2.25833 9.31667 2.775 8.1C3.30833 6.88333 4.025 5.825 4.925 4.925C5.825 4.025 6.88333 3.31667 8.1 2.8C9.31667 2.26667 10.6167 2 12 2C13.3833 2 14.6833 2.26667 15.9 2.8C17.1167 3.31667 18.175 4.025 19.075 4.925C19.975 5.825 20.6833 6.88333 21.2 8.1C21.7333 9.31667 22 10.6167 22 12C22 13.3833 21.7333 14.6833 21.2 15.9C20.6833 17.1167 19.975 18.175 19.075 19.075C18.175 19.975 17.1167 20.6917 15.9 21.225C14.6833 21.7417 13.3833 22 12 22ZM12 20C14.2333 20 16.125 19.225 17.675 17.675C19.225 16.125 20 14.2333 20 12C20 9.76667 19.225 7.875 17.675 6.325C16.125 4.775 14.2333 4 12 4C9.76667 4 7.875 4.775 6.325 6.325C4.775 7.875 4 9.76667 4 12C4 14.2333 4.775 16.125 6.325 17.675C7.875 19.225 9.76667 20 12 20Z";

    private static final String SVG_UPLOAD =
            "M17.8622 6.13763L12.7717 1.047C12.7461 1.02145 12.7193 0.997407 12.6914 0.974485C12.6793 0.964501 12.6662 0.955642 12.6536 0.946173C12.6376 0.934267 12.622 0.921938 12.6057 0.91097C12.5904 0.900798 12.5743 0.891845 12.5586 0.882376C12.5439 0.873517 12.5293 0.864188 12.5142 0.856126C12.4981 0.847548 12.4815 0.840188 12.4649 0.83236C12.4491 0.824954 12.4335 0.817032 12.4176 0.810376C12.4016 0.803767 12.385 0.798376 12.3687 0.79247C12.3514 0.786235 12.3342 0.779579 12.3167 0.774282C12.3005 0.769313 12.2837 0.765704 12.2671 0.761532C12.2492 0.757032 12.2314 0.752017 12.2132 0.74836C12.194 0.744517 12.1743 0.742173 12.1548 0.739407C12.139 0.737157 12.1235 0.734157 12.1077 0.732563C12.0723 0.729142 12.0364 0.727173 12 0.727173C11.9636 0.727173 11.9277 0.729142 11.8922 0.73261C11.8769 0.734157 11.8616 0.737063 11.8462 0.73922C11.8263 0.742079 11.8063 0.74447 11.7867 0.748313C11.7691 0.751782 11.7517 0.756751 11.7343 0.76111C11.7172 0.765423 11.6998 0.769173 11.6831 0.774282C11.6661 0.779438 11.6495 0.785907 11.6329 0.79186C11.616 0.797907 11.5988 0.803485 11.5825 0.810329C11.567 0.816751 11.5519 0.824438 11.5367 0.831563C11.5196 0.839626 11.5023 0.847267 11.4857 0.856126C11.4713 0.863767 11.4575 0.872767 11.4434 0.881157C11.4269 0.890954 11.4101 0.900423 11.3941 0.911063C11.3789 0.921329 11.3643 0.93286 11.3494 0.944017C11.3357 0.954188 11.3217 0.963798 11.3084 0.974579C11.2823 0.996048 11.257 1.01902 11.2325 1.0433C11.2311 1.04447 11.2299 1.0455 11.2287 1.04663L6.13762 6.13758C5.71166 6.56363 5.71157 7.25433 6.13762 7.68033C6.56357 8.10638 7.25437 8.10628 7.68037 7.68042L10.9091 4.45186V17.8181C10.9091 18.4206 11.3976 18.909 12 18.909C12.6025 18.909 13.091 18.4206 13.091 17.8181V4.45191L16.3195 7.68042C16.5325 7.89342 16.8117 7.99997 17.0908 7.99997C17.37 7.99997 17.6492 7.89342 17.8622 7.68042C18.2883 7.25438 18.2883 6.56363 17.8622 6.13763Z " +
                    "M22.9091 11.6364C22.3066 11.6364 21.8182 12.1248 21.8182 12.7273V21.0909H2.1818V12.7273C2.1818 12.1248 1.69336 11.6364 1.09087 11.6364C0.488391 11.6364 0 12.1248 0 12.7273V22.1818C0 22.7843 0.488438 23.2727 1.09092 23.2727H22.9091C23.5116 23.2727 24 22.7843 24 22.1818V12.7273C24 12.1248 23.5116 11.6364 22.9091 11.6364Z";

    private static final String SVG_BACK =
            "M0 8.50732C0 8.06201 0.182454 7.65381 0.547363 7.28271L7.57031 0.41748C7.71257 0.281413 7.86719 0.17627 8.03418 0.102051C8.20736 0.0340169 8.3929 0 8.59082 0C8.86914 0 9.11963 0.0649414 9.34229 0.194824C9.57113 0.330892 9.75049 0.510254 9.88037 0.73291C10.0164 0.955566 10.0845 1.20296 10.0845 1.4751C10.0845 1.88949 9.92676 2.25439 9.61133 2.56982L3.10791 8.8877V8.13623L9.61133 14.4355C9.92676 14.751 10.0845 15.1128 10.0845 15.521C10.0845 15.7931 10.0164 16.0405 9.88037 16.2632C9.75049 16.4858 9.57113 16.6621 9.34229 16.792C9.11963 16.9281 8.86914 16.9961 8.59082 16.9961C8.1888 16.9961 7.84863 16.8569 7.57031 16.5786L0.547363 9.72266C0.361816 9.53711 0.225749 9.34538 0.13916 9.14746C0.0525716 8.94954 0.0061849 8.73617 0 8.50732Z";

    private final TextArea questionArea = new TextArea();
    private final VBox optionsColumn = new VBox(6);
    private final VBox radiosColumn = new VBox(6);
    private final List<OptionRow> optionRows = new ArrayList<>();
    private final ToggleGroup correctToggle = new ToggleGroup();

    private final TextField startDateField = new TextField();
    private final TextField startTimeField = new TextField();
    private final TextField endDateField = new TextField();
    private final TextField endTimeField = new TextField();

    private final Button addOptionButton = new Button("Add Option");
    private final Button saveButton = new Button("Save & Submit");
    private final Button backButton = new Button("Back");
    private final Button deleteButton = new Button("Delete Question");

    private StackPane overlay;
    private VBox submitPopup;

    public EditQuestionView(ClientAPI client, StateManager stateManager, User user, Question question) {
        this.client = client;
        this.stateManager = stateManager;
        this.user = user;
        this.question = question;

        setStyle("-fx-background-color: " + COLOR_BG + ";");

        BorderPane layout = new BorderPane();

        HeaderView header = new HeaderView(stateManager, user);
        layout.setTop(header);

        VBox center = createCenterContent();
        center.setAlignment(Pos.TOP_CENTER);
        layout.setCenter(center);

        StackPane root = new StackPane(layout);
        header.attachToRoot(root);
        this.setCenter(root);

        createSubmitPopup();
        root.getChildren().add(overlay);
        StackPane.setAlignment(overlay, Pos.CENTER);

        initOptionsFromQuestion();
        fillDateTimeFromQuestion();
        questionArea.setText(question.getQuestion());
    }

    private VBox createCenterContent() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(20, 0, 0, 0));

        Label questionLabel = new Label("Edit Question");
        questionLabel.setStyle("""
                -fx-text-fill: white;
                -fx-font-size: 20px;
                -fx-font-weight: bold;
                """);

        styleDeleteButton(deleteButton);
        deleteButton.setOnAction(e -> handleDelete());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox titleBar = new HBox(10, questionLabel, spacer, deleteButton);
        titleBar.setAlignment(Pos.CENTER_LEFT);

        titleBar.setMaxWidth(800);

        questionArea.setPromptText("Question...");
        questionArea.setWrapText(true);
        questionArea.setPrefRowCount(2);
        questionArea.setPrefHeight(55);
        questionArea.setStyle("""
            -fx-background-color: white;
            -fx-control-inner-background: white;
            -fx-text-fill: black;
            -fx-font-size: 14px;
            -fx-background-insets: 0;
            -fx-padding: 10;
            -fx-focus-color: transparent;
            -fx-faint-focus-color: transparent;
            -fx-highlight-fill: transparent;
            -fx-highlight-text-fill: black;
            -fx-background-radius: 8;
            -fx-border-radius: 8;
            """);
        questionArea.setMaxWidth(800);
        questionArea.setMinWidth(400);
        questionArea.setPrefWidth(Region.USE_COMPUTED_SIZE);

        HBox questionAreaContainer = new HBox(questionArea);
        questionAreaContainer.setAlignment(Pos.CENTER);
        HBox.setHgrow(questionArea, Priority.ALWAYS);

        questionAreaContainer.setMaxWidth(800);
        VBox.setMargin(questionAreaContainer, new Insets(0, 0, 0, 0));


        optionsColumn.setAlignment(Pos.TOP_LEFT);
        radiosColumn.setAlignment(Pos.TOP_CENTER);

        Label correctLabel = new Label("Correct Option");
        correctLabel.setStyle(String.format("""
                -fx-text-fill: %s;
                -fx-font-size: 12px;
                -fx-font-weight: bold;
                """, COLOR_PRIMARY));
        VBox correctColumn = new VBox(6, correctLabel, radiosColumn);
        correctColumn.setAlignment(Pos.TOP_CENTER);

        styleManualDateField(startDateField, "Start Date");
        styleManualDateField(endDateField, "End Date");
        styleManualTimeField(startTimeField, "Start Time");
        styleManualTimeField(endTimeField, "End Time");

        VBox datesColumn = new VBox(12,
                startDateField,
                startTimeField,
                endDateField,
                endTimeField
        );
        datesColumn.setAlignment(Pos.TOP_LEFT);

        optionsColumn.setTranslateY(10);
        radiosColumn.setTranslateY(10);
        datesColumn.setTranslateY(10);

        styleAddOptionButton(addOptionButton);
        addOptionButton.setGraphic(createPlusIcon());
        addOptionButton.setGraphicTextGap(6);
        addOptionButton.setOnAction(e -> addOptionRow(null));

        HBox addOptionContainer = new HBox(addOptionButton);
        addOptionContainer.setAlignment(Pos.CENTER_LEFT);
        addOptionContainer.setPadding(new Insets(0, 0, 0, 18));
        addOptionContainer.setTranslateY(10);

        VBox optionsWithButton = new VBox(6, optionsColumn, addOptionContainer);
        optionsWithButton.setAlignment(Pos.TOP_LEFT);
        optionsWithButton.setPadding(new Insets(0, 0, 0, 38));

        HBox middleArea = new HBox(40, optionsWithButton, correctColumn, datesColumn);
        middleArea.setAlignment(Pos.TOP_CENTER);

        styleButton(saveButton, createUploadIcon());
        saveButton.setGraphicTextGap(35);
        styleButton(backButton, createBackIcon());

        saveButton.setOnAction(e -> handleSave());
        backButton.setOnAction(e -> {
            if (stateManager != null)
                stateManager.showQuestionHistory(user);
        });

        VBox buttonsBox = new VBox(20, saveButton, backButton);
        buttonsBox.setAlignment(Pos.CENTER);
        VBox.setMargin(buttonsBox, new Insets(50, 0, 0, 0));

        root.getChildren().addAll(
                titleBar,
                questionAreaContainer,
                middleArea,
                buttonsBox
        );

        return root;
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

    private void initOptionsFromQuestion() {
        optionsColumn.getChildren().clear();
        radiosColumn.getChildren().clear();
        optionRows.clear();

        String[] opts = question.getOptions();
        if (opts == null || opts.length == 0) {
            addOptionRow(null);
            addOptionRow(null);
            addOptionRow(null);
        } else {
            for (String opt : opts) {
                addOptionRow(opt);
            }
        }

        String correctLetter = question.getCorrectOption();
        if (correctLetter != null && !correctLetter.isBlank()) {
            int idx = correctLetter.charAt(0) - 'a';
            if (idx >= 0 && idx < optionRows.size()) {
                optionRows.get(idx).radioButton.setSelected(true);
            }
        } else if (!optionRows.isEmpty()) {
            optionRows.get(0).radioButton.setSelected(true);
        }
    }

    private void fillDateTimeFromQuestion() {
        LocalDateTime st = question.getStartTime();
        LocalDateTime en = question.getEndTime();
        if (st != null) {
            startDateField.setText(formatDate(st.toLocalDate()));
            startTimeField.setText(formatTime(st.toLocalTime()));
        }
        if (en != null) {
            endDateField.setText(formatDate(en.toLocalDate()));
            endTimeField.setText(formatTime(en.toLocalTime()));
        }
    }

    private String formatDate(LocalDate d) {
        return String.format("%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }

    private String formatTime(LocalTime t) {
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    private void addOptionRow(String initialText) {
        char letter = (char) ('a' + optionRows.size());

        Label letterLabel = new Label(letter + ")");
        letterLabel.setStyle("""
            -fx-text-fill: white;
            -fx-font-size: 12px;
            """);

        TextField optionField = new TextField();
        optionField.setPromptText("Option");
        if (initialText != null)
            optionField.setText(initialText);

        optionField.setPrefWidth(230);
        optionField.setMinHeight(28);
        optionField.setStyle("""
            -fx-background-color: white;
            -fx-text-fill: black;
            -fx-font-size: 16px;
            -fx-padding: 2 8;
            -fx-background-radius: 12;
            -fx-border-color: transparent;
            -fx-alignment: CENTER_LEFT;
            -fx-padding: 0 0 0 35;
            """);

        Button removeBtn = new Button("x");
        removeBtn.setCursor(Cursor.HAND);
        removeBtn.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            """);

        RadioButton radio = new RadioButton();
        radio.setToggleGroup(correctToggle);
        radio.setPrefHeight(28);
        radio.setMinHeight(28);
        radio.setMaxHeight(28);
        radio.setStyle(String.format("""
            -fx-background-color: transparent;
            -fx-padding: 0;
            -fx-border-color: transparent;
            -fx-mark-color: %s;
            -fx-accent: %s;
            """, COLOR_PRIMARY, COLOR_PRIMARY));

        if (optionRows.isEmpty()) {
            VBox.setMargin(radio, new Insets(-20, 0, 0, 0));
        } else {
            VBox.setMargin(radio, new Insets(0, 0, 0, 0));
        }

        HBox row = new HBox(10, letterLabel, optionField, removeBtn);
        row.setAlignment(Pos.CENTER_LEFT);

        optionsColumn.getChildren().add(row);
        radiosColumn.getChildren().add(radio);

        OptionRow opt = new OptionRow(letterLabel, optionField, removeBtn, radio, row);
        optionRows.add(opt);

        removeBtn.setOnAction(e -> removeOptionRow(opt));
    }

    private void removeOptionRow(OptionRow row) {
        if (optionRows.size() <= 2)
            return;

        optionsColumn.getChildren().remove(row.optionLine);
        radiosColumn.getChildren().remove(row.radioButton);
        optionRows.remove(row);

        for (int i = 0; i < optionRows.size(); i++) {
            char letter = (char) ('a' + i);
            optionRows.get(i).letterLabel.setText(letter + ")");
        }
    }


    private void styleManualDateField(TextField field, String placeholder) {
        field.setPromptText(placeholder);
        field.setPrefWidth(150);
        field.setMinHeight(28);
        field.setMaxHeight(28);

        field.setStyle("""
                -fx-background-color: white;
                -fx-text-fill: black;
                -fx-font-size: 13px;
                -fx-background-radius: 8;
                -fx-border-color: transparent;
                -fx-padding: 0 8;
                """);

        field.textProperty().addListener((obs, oldV, newV) -> {
            newV = newV.replaceAll("[^0-9/]", "");
            if (newV.length() == 2 && !oldV.endsWith("/"))
                newV += "/";
            if (newV.length() == 5 && oldV.length() < 5)
                newV += "/";
            if (newV.length() > 10)
                newV = oldV;
            field.setText(newV);
        });

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                if (!isValidDate(field.getText())) {
                    field.setStyle("""
                            -fx-background-color: #ffcccc;
                            -fx-text-fill: black;
                            -fx-font-size: 13px;
                            -fx-background-radius: 8;
                            -fx-border-color: red;
                            -fx-padding: 0 8;
                            """);
                }
            } else {
                field.setStyle("""
                        -fx-background-color: white;
                        -fx-text-fill: black;
                        -fx-font-size: 13px;
                        -fx-background-radius: 8;
                        -fx-border-color: transparent;
                        -fx-padding: 0 8;
                        """);
            }
        });
    }

    private void styleManualTimeField(TextField field, String placeholder) {
        field.setPromptText(placeholder);
        field.setPrefWidth(150);
        field.setMinHeight(28);
        field.setMaxHeight(28);

        field.setStyle("""
                -fx-background-color: white;
                -fx-text-fill: black;
                -fx-font-size: 13px;
                -fx-background-radius: 8;
                -fx-border-color: transparent;
                -fx-padding: 0 8;
                """);

        field.textProperty().addListener((obs, oldV, newV) -> {
            newV = newV.replaceAll("[^0-9:]", "");
            if (newV.length() == 2 && !oldV.endsWith(":"))
                newV += ":";
            if (newV.length() > 5)
                newV = oldV;
            field.setText(newV);
        });

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                if (!isValidTime(field.getText())) {
                    field.setStyle("""
                            -fx-background-color: #ffcccc;
                            -fx-text-fill: black;
                            -fx-font-size: 13px;
                            -fx-background-radius: 8;
                            -fx-border-color: red;
                            -fx-padding: 0 8;
                            """);
                }
            } else {
                field.setStyle("""
                        -fx-background-color: white;
                        -fx-text-fill: black;
                        -fx-font-size: 13px;
                        -fx-background-radius: 8;
                        -fx-border-color: transparent;
                        -fx-padding: 0 8;
                        """);
            }
        });
    }

    private boolean isValidDate(String text) {
        try {
            if (text == null || !text.matches("\\d{2}/\\d{2}/\\d{4}"))
                return false;
            String[] p = text.split("/");
            int d = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1]);
            int y = Integer.parseInt(p[2]);
            LocalDate.of(y, m, d);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidTime(String text) {
        try {
            if (text == null || !text.matches("\\d{2}:\\d{2}"))
                return false;
            String[] p = text.split(":");
            int h = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1]);
            return h >= 0 && h <= 23 && m >= 0 && m <= 59;
        } catch (Exception e) {
            return false;
        }
    }

    private void styleAddOptionButton(Button btn) {
        btn.setCursor(Cursor.HAND);
        btn.setStyle(String.format("""
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-font-size: 13px;
                -fx-font-weight: bold;
                -fx-background-radius: 8;
                -fx-padding: 0 8;
                """, COLOR_PRIMARY, COLOR_BUTTON_TEXT));
        btn.setPrefWidth(230);
        btn.setMinHeight(28);
        btn.setPrefHeight(28);
        btn.setMaxHeight(28);
    }

    private SVGPath createPlusIcon() {
        SVGPath svg = new SVGPath();
        svg.setContent(SVG_PLUS_CIRCLE);
        svg.setFill(Color.BLACK);
        svg.setScaleX(0.6);
        svg.setScaleY(0.6);
        return svg;
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

    private void handleSave() {
        try {
            String text = questionArea.getText().trim();
            if (text.isEmpty()) {
                showAlert("Validation", "Question text is required.");
                return;
            }

            List<String> options = new ArrayList<>();
            int correctIndex = -1;

            for (int i = 0; i < optionRows.size(); i++) {
                OptionRow or = optionRows.get(i);
                String optText = or.textField.getText().trim();
                if (optText.isEmpty()) {
                    continue;
                }
                options.add(optText);
                if (or.radioButton.isSelected()) {
                    correctIndex = i;
                }
            }

            if (options.size() < 2) {
                showAlert("Validation", "At least two options are required.");
                return;
            }

            if (correctIndex < 0 || correctIndex >= options.size()) {
                showAlert("Validation", "Select the correct option.");
                return;
            }

            if (!isValidDate(startDateField.getText()) || !isValidDate(endDateField.getText()) ||
                    !isValidTime(startTimeField.getText()) || !isValidTime(endTimeField.getText())) {
                showAlert("Validation", "Invalid date or time.");
                return;
            }

            LocalDate sd = parseDate(startDateField.getText());
            LocalDate ed = parseDate(endDateField.getText());
            LocalTime st = parseTime(startTimeField.getText());
            LocalTime et = parseTime(endTimeField.getText());

            LocalDateTime start = LocalDateTime.of(sd, st);
            LocalDateTime end = LocalDateTime.of(ed, et);

            if (!end.isAfter(start)) {
                showAlert("Validation", "End datetime must be after start datetime.");
                return;
            }

            question.setQuestion(text);
            question.setOptions(options.toArray(new String[0]));
            char correctLetter = (char) ('a' + correctIndex);
            question.setCorrectOption(String.valueOf(correctLetter));
            question.setStartTime(start);
            question.setEndTime(end);

            overlay.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Error while saving the question.");
        }
    }

    private LocalDate parseDate(String text) {
        String[] p = text.split("/");
        int d = Integer.parseInt(p[0]);
        int m = Integer.parseInt(p[1]);
        int y = Integer.parseInt(p[2]);
        return LocalDate.of(y, m, d);
    }

    private LocalTime parseTime(String text) {
        String[] p = text.split(":");
        int h = Integer.parseInt(p[0]);
        int m = Integer.parseInt(p[1]);
        return LocalTime.of(h, m);
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void createSubmitPopup() {
        overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.45);");
        overlay.setVisible(false);
        overlay.setPickOnBounds(true);

        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(25));
        box.setMinWidth(360);
        box.setPrefWidth(360);
        box.setMaxWidth(360);
        box.setMinHeight(200);
        box.setPrefHeight(200);
        box.setMaxHeight(200);

        box.setStyle("""
            -fx-background-color: #D9D9D9;
            -fx-background-radius: 14;
            -fx-border-color: #FF7A00;
            -fx-border-width: 3;
            -fx-border-radius: 14;
            -fx-padding: 30;
            """);

        SVGPath check = new SVGPath();
        check.setContent("M16 2.66675C8.66663 2.66675 2.66663 8.66675 2.66663 16.0001C2.66663 23.3334 8.66663 29.3334 16 29.3334C23.3333 29.3334 29.3333 23.3334 29.3333 16.0001C29.3333 8.66675 23.3333 2.66675 16 2.66675ZM14.4 22.4001L9.46663 17.4667L11.3333 15.6001L14.2666 18.5334L22 10.4001L24 12.4001L14.4 22.4001Z");
        check.setFill(Color.BLACK);
        check.setScaleX(1.2);
        check.setScaleY(1.2);

        Label title = new Label("Question Updated!");
        title.setStyle("""
        -fx-font-size: 16px;
        -fx-text-fill: black;
        -fx-font-weight: bold;
        """);

        Label msg = new Label("The question was successfully edited.");
        msg.setStyle("-fx-text-fill: black; -fx-font-size: 14px;");

        box.getChildren().addAll(check, title, msg);

        submitPopup = box;
        overlay.getChildren().add(submitPopup);
        StackPane.setAlignment(submitPopup, Pos.CENTER);

        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                overlay.setVisible(false);
                if (stateManager != null)
                    stateManager.showQuestionHistory(user);
            }
        });
    }

    private static class OptionRow {
        final Label letterLabel;
        final TextField textField;
        final Button removeButton;
        final RadioButton radioButton;
        final HBox optionLine;

        OptionRow(Label letterLabel, TextField textField,
                  Button removeButton, RadioButton radioButton,
                  HBox optionLine) {
            this.letterLabel = letterLabel;
            this.textField = textField;
            this.removeButton = removeButton;
            this.radioButton = radioButton;
            this.optionLine = optionLine;
        }
    }

    private void styleDeleteButton(Button btn) {
        btn.setCursor(Cursor.HAND);
        btn.setPrefWidth(180);
        btn.setPrefHeight(40);
        btn.setStyle("""
            -fx-background-color: #A00000;
            -fx-text-fill: white;
            -fx-font-size: 12px;
            -fx-font-weight: bold;
            -fx-background-radius: 12;
            -fx-alignment: CENTER;
        """);
    }

    private void handleDelete() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.TRANSPARENT);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getScene().setFill(Color.TRANSPARENT);

        dialogPane.setStyle("""
        -fx-background-color: transparent;
        -fx-padding: 0;
    """);

        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.initStyle(StageStyle.TRANSPARENT);

        VBox mainBox = new VBox(15);
        mainBox.setAlignment(Pos.CENTER);
        mainBox.setPadding(new Insets(20, 20, 20, 20));
        mainBox.setStyle("""
        -fx-background-color: #D9D9D9;
        -fx-background-radius: 16;
        -fx-border-color: red;
        -fx-border-width: 4;
        -fx-border-radius: 16;
    """);

        SVGPath customIcon = new SVGPath();
        customIcon.setContent(
                "M32.3039 32.5965L19.1207 6.21369C18.0213 4.92877 16.2374 4.92877 15.138 6.21369L1.9537 32.5965C0.8543 33.8802 0.8543 35.9638 1.9537 37.25H32.3039C33.4044 35.9638 33.4044 33.8802 32.3039 32.5965ZM15.9152 15.6845C15.9152 14.6642 16.6228 13.8383 17.4948 13.8383C18.3667 13.8383 19.0744 14.6642 19.0744 15.6845V24.2998C19.0744 25.3189 18.3667 26.146 17.4948 26.146C16.6228 26.146 15.9152 25.3189 15.9152 24.2998V15.6845ZM17.5042 32.3097C16.6323 32.3097 15.9246 31.4851 15.9246 30.4635C15.9246 29.4445 16.6323 28.6174 17.5042 28.6174C18.3762 28.6174 19.0838 30.4635 17.5042 32.3097Z");
        customIcon.setFill(Color.RED);
        customIcon.setScaleX(1.4);
        customIcon.setScaleY(1.4);

        Label titleLabel = new Label("Warning!");
        titleLabel.setStyle("""
            -fx-font-size: 18px;
            -fx-font-weight: bold;
            -fx-text-fill: black;
            """);

        Label contentLabel = new Label(
                "Are you sure you want to permanently delete this question? This action cannot be undone."
        );
        contentLabel.setStyle("-fx-text-fill: black; -fx-font-size: 13px;");
        contentLabel.setWrapText(true);

        VBox contentBox = new VBox(12, customIcon, titleLabel, contentLabel);
        contentBox.setAlignment(Pos.CENTER);

        Button okButton = new Button("OK");
        okButton.setCursor(Cursor.HAND);
        okButton.setStyle("""
        -fx-background-color: #FF7A00;
        -fx-text-fill: black;
        -fx-font-size: 14px;
        -fx-font-weight: bold;
        -fx-background-radius: 8;
        -fx-padding: 8 28;
    """);

        Button cancelButton = new Button("Cancel");
        cancelButton.setCursor(Cursor.HAND);
        cancelButton.setStyle("""
        -fx-background-color: white;
        -fx-text-fill: black;
        -fx-font-size: 14px;
        -fx-font-weight: bold;
        -fx-background-radius: 8;
        -fx-padding: 8 28;
    """);

        HBox buttonsBox = new HBox(15, okButton, cancelButton);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.setPadding(new Insets(10, 0, 5, 0));

        mainBox.getChildren().addAll(contentBox, buttonsBox);

        dialogPane.setContent(mainBox);

        mainBox.setOpacity(0);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(180), mainBox);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        okButton.setOnAction(ev -> {
            Stage dlgStage = (Stage) dialogPane.getScene().getWindow();
            dlgStage.close();

            try {
                boolean success = client.deleteQuestion(question.getId());

                if (success) {
                    showDeleteSuccessPopup();
                } else {
                    showAlert("Error", "Could not delete question.");
                }

            } catch (IOException e) {
                showAlert("Connection Error", "Failed to communicate with server.");
            }
        });

        cancelButton.setOnAction(ev -> {
            Stage dlgStage = (Stage) dialogPane.getScene().getWindow();
            dlgStage.close();
        });

        dialog.showAndWait();
    }

    private void showDeleteSuccessPopup() {
        overlay.setVisible(true);

        overlay.getChildren().clear();

        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(25));
        box.setMinWidth(360);
        box.setPrefWidth(360);
        box.setMaxWidth(360);
        box.setMinHeight(200);
        box.setPrefHeight(200);
        box.setMaxHeight(200);

        box.setStyle("""
            -fx-background-color: #D9D9D9;
            -fx-background-radius: 14;
            -fx-border-color: #A00000;
            -fx-border-width: 3;
            -fx-border-radius: 14;
            """);

        SVGPath trashCan = new SVGPath();
        trashCan.setContent("M6 19C6 20.1 6.9 21 8 21H16C17.1 21 18 20.1 18 19V7H6V19ZM19 4H15.5L14.5 3H9.5L8.5 4H5V6H19V4Z");
        trashCan.setFill(Color.BLACK);
        trashCan.setScaleX(1.2);
        trashCan.setScaleY(1.2);

        Label title = new Label("Question Deleted!");
        title.setStyle("""
            -fx-font-size: 16px;
            -fx-text-fill: red;
            -fx-font-weight: bold;
            """);

        Label msg = new Label("The question was successfully deleted.");
        msg.setStyle("-fx-text-fill: black; -fx-font-size: 14px;");

        box.getChildren().addAll(trashCan, title, msg);

        overlay.getChildren().add(box);
        StackPane.setAlignment(box, Pos.CENTER);

        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                overlay.setVisible(false);
                if (stateManager != null)
                    stateManager.showQuestionHistory(user);
            }
        });
    }

}