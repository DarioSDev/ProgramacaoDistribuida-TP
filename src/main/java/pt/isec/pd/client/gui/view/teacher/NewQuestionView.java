package pt.isec.pd.client.gui.view.teacher;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import pt.isec.pd.client.ClientAPI;
import pt.isec.pd.client.StateManager;
import pt.isec.pd.common.User;
import java.util.ArrayList;
import java.util.List;

public class NewQuestionView extends BorderPane {

    private final ClientAPI client;
    private final StateManager stateManager;
    private final User user;

    private static final String COLOR_PRIMARY = "#FF7A00";
    private static final String COLOR_BG = "#1A1A1A";
    private static final String COLOR_BUTTON_TEXT = "#000000";
    private static final String COLOR_DROPDOWN_PROFILE = "#D9D9D9";
    private static final String COLOR_HOVER = "#F4B27C";
    private static final int DROPDOWN_WIDTH = 200;
    private static final double DROPDOWN_OFFSET_Y = 100;
    private StackPane overlay;
    private VBox submitPopup;


    private static final String SVG_MENU =
            "M3.59615 7.25H17.9038C18.2331 7.25 18.5 6.91421 18.5 6.5C18.5 6.08579 18.2331 5.75 17.9038 5.75H3.59615ZM3 11C3 10.5858 3.26691 10.25 3.59615 10.25H17.9038C18.2331 10.25 18.5 10.5858 18.5 11C18.5 11.4142 18.2331 11.75 17.9038 11.75H3.59615C3.26691 11.75 3 11.4142 3 11ZM3.59615 14.75C3.26691 14.75 3 15.0858 3 15.5C3 15.9142 3.26691 16.25 3.59615 16.25H17.9038C18.2331 16.25 18.5 15.9142 18.5 15.5C18.5 15.0858 18.2331 14.75 17.9038 14.75H3.59615Z";

    private static final String SVG_PROFILE =
            "M23.3792 3.50708H20.4692L17.1526 0.190441C16.8987 -0.0634803 16.4871 -0.0634803 16.2332 0.190441L12.9166 3.50698H10.0065C9.67322 3.50698 9.40308 3.77717 9.40308 4.11045V8.63899L10.0744 8.22218C10.555 7.92382 11.137 7.88566 11.6404 8.09111L13.8894 6.31774C14.2278 6.05093 14.7183 6.10905 14.985 6.4473C15.2518 6.7856 15.1937 7.27615 14.8554 7.54286L12.663 9.27156C12.8403 9.98064 12.552 10.7526 11.8978 11.1588L10.61 11.9584L9.40312 12.7078V14.021C9.40312 14.3544 9.67331 14.6245 10.0066 14.6245H23.3792C23.7125 14.6245 23.9827 14.3543 23.9827 14.021V4.11054C23.9827 3.77726 23.7125 3.50708 23.3792 3.50708ZM14.7553 3.50708L16.6929 1.5695L18.6305 3.50708H14.7553Z " +
                    "M11.7203 9.23481C11.4685 8.82929 10.9357 8.7047 10.5303 8.95647L8.78011 10.0431C8.77284 8.59169 8.77669 9.36217 8.772 8.4272C8.76637 7.30525 7.84894 6.39246 6.72694 6.39246H5.87672C5.66054 6.95313 5.11543 8.36687 4.95221 8.7902L6.68194 9.58422V8.43405C6.68194 8.33416 6.76289 8.25316 6.86283 8.25316C6.96277 8.25316 7.04372 8.33411 7.04372 8.43405C7.04372 8.43466 7.04372 8.43531 7.04372 8.43592L7.0628 11.6638C7.11666 12.3111 7.83577 12.6638 8.37956 12.3262L11.442 10.4247C11.8475 10.1731 11.9721 9.64032 11.7203 9.23481Z " +
                    "M2.10455 13.511L2.10498 22.963C2.10498 23.5358 2.56922 24 3.14199 24C3.71475 24 4.179 23.5358 4.179 22.963V14.6741H4.57289C4.48983 14.6468 2.10455 13.511 2.10455 13.511Z " +
                    "M6.68194 12.4288L5.92158 14.0851C5.69827 14.5715 5.14815 14.833 4.62671 14.6904V22.963C4.62671 23.5357 5.09096 24 5.66372 24C6.23649 24 6.70074 23.5357 6.70074 22.963C6.6998 22.4403 6.68194 13.005 6.68194 12.4288Z " +
                    "M4.99039 7.79854L4.63901 7.15955L4.95073 6.59255C4.99718 6.50803 4.93596 6.4043 4.8394 6.4043H3.98295C3.88653 6.4043 3.82512 6.50794 3.87162 6.59255L4.18404 7.16067L3.83206 7.80084C3.79119 7.87523 3.77787 7.96153 3.79437 8.04473L3.91756 8.66517C4.28637 8.53115 4.56317 8.61168 4.87671 8.75559L5.02718 8.04698C5.04509 7.96247 5.03201 7.87425 4.99039 7.79854Z " +
                    "M6.47404 10.2328L4.47216 9.31376C4.25663 9.21485 4.002 9.30949 3.90319 9.5247L3.38531 10.6529L4.43972 11.2387C5.19295 11.6572 5.46431 12.607 5.04581 13.3602C4.91597 13.594 4.73452 13.7808 4.52395 13.9155L4.73831 14.0139C4.95384 14.1128 5.20847 14.0182 5.30728 13.803L6.68498 10.8017C6.78384 10.5864 6.68939 10.3316 6.47404 10.2328Z " +
                    "M4.40287 5.82162C5.39201 5.82162 6.19387 5.01976 6.19387 4.03062C6.19387 3.04148 5.39201 2.23962 4.40287 2.23962C3.41373 2.23962 2.61188 3.04148 2.61188 4.03062C2.61188 5.01976 3.41373 5.82162 4.40287 5.82162Z " +
                    "M4.10164 11.8471L1.74876 10.5398C1.75358 9.75301 1.75719 9.17406 1.76169 8.43587C1.76221 8.34128 1.83913 8.26502 1.93363 8.26525C2.02813 8.26548 2.10458 8.34222 2.10458 8.43676V9.94126L2.79257 10.3235L3.28878 9.24264C3.40124 8.99762 3.59792 8.80698 3.83492 8.69861C3.73794 8.44703 3.04321 6.6453 2.94571 6.39246H2.07843C0.956382 6.39246 0.0390398 7.3053 0.0334148 8.42622C0.031071 8.80464 0.0196336 10.6637 0.0172898 11.0412C0.0153211 11.3569 0.185758 11.6486 0.461758 11.8019L3.26225 13.3578C3.67939 13.5897 4.20556 13.4394 4.43736 13.0222C4.66911 12.605 4.51883 12.0789 4.10164 11.8471Z";

    private static final String SVG_LOGOUT =
            "M14.7961 0H12.1961C8.99609 0 6.99609 2 6.99609 5.2V9.25H13.2461C13.6561 9.25 13.9961 9.59 13.9961 10C13.9961 10.41 13.6561 10.75 13.2461 10.75H6.99609V14.8C6.99609 18 8.99609 20 12.1961 20H14.7861C17.9861 20 19.9861 18 19.9861 14.8V5.2C19.9961 2 17.9961 0 14.7961 0Z " +
                    "M2.5575 9.2498L4.6275 7.17984C4.7775 7.02984 4.8475 6.83984 4.8475 6.64984C4.8475 6.45984 4.7775 6.25984 4.6275 6.11984C4.3375 5.82984 3.8575 5.82984 3.5675 6.11984L0.2175 9.4698C-0.0725 9.7598 -0.0725 10.2398 0.2175 10.5298L3.5675 13.8798C3.8575 14.1698 4.3375 14.1698 4.6275 13.8798C4.9175 13.5898 4.9175 13.1098 4.6275 12.8198L2.5575 10.7498H6.9975V9.2498H2.5575Z";

    private static final String SVG_PLUS_CIRCLE =
            "M11 17H13V13H17V11H13V7H11V11H7V13H11V17ZM12 22C10.6167 22 9.31667 21.7417 8.1 21.225C6.88333 20.6917 5.825 19.975 4.925 19.075C4.025 18.175 3.30833 17.1167 2.775 15.9C2.25833 14.6833 2 13.3833 2 12C2 10.6167 2.25833 9.31667 2.775 8.1C3.30833 6.88333 4.025 5.825 4.925 4.925C5.825 4.025 6.88333 3.31667 8.1 2.8C9.31667 2.26667 10.6167 2 12 2C13.3833 2 14.6833 2.26667 15.9 2.8C17.1167 3.31667 18.175 4.025 19.075 4.925C19.975 5.825 20.6833 6.88333 21.2 8.1C21.7333 9.31667 22 10.6167 22 12C22 13.3833 21.7333 14.6833 21.2 15.9C20.6833 17.1167 19.975 18.175 19.075 19.075C18.175 19.975 17.1167 20.6917 15.9 21.225C14.6833 21.7417 13.3833 22 12 22ZM12 20C14.2333 20 16.125 19.225 17.675 17.675C19.225 16.125 20 14.2333 20 12C20 9.76667 19.225 7.875 17.675 6.325C16.125 4.775 14.2333 4 12 4C9.76667 4 7.875 4.775 6.325 6.325C4.775 7.875 4 9.76667 4 12C4 14.2333 4.775 16.125 6.325 17.675C7.875 19.225 9.76667 20 12 20Z";

    private static final String SVG_UPLOAD =
            "M17.8622 6.13763L12.7717 1.047C12.7461 1.02145 12.7193 0.997407 12.6914 0.974485C12.6793 0.964501 12.6662 0.955642 12.6536 0.946173C12.6376 0.934267 12.622 0.921938 12.6057 0.91097C12.5904 0.900798 12.5743 0.891845 12.5586 0.882376C12.5439 0.873517 12.5293 0.864188 12.5142 0.856126C12.4981 0.847548 12.4815 0.840188 12.4649 0.83236C12.4491 0.824954 12.4335 0.817032 12.4176 0.810376C12.4016 0.803767 12.385 0.798376 12.3687 0.79247C12.3514 0.786235 12.3342 0.779579 12.3167 0.774282C12.3005 0.769313 12.2837 0.765704 12.2671 0.761532C12.2492 0.757032 12.2314 0.752017 12.2132 0.74836C12.194 0.744517 12.1743 0.742173 12.1548 0.739407C12.139 0.737157 12.1235 0.734157 12.1077 0.732563C12.0723 0.729142 12.0364 0.727173 12 0.727173C11.9636 0.727173 11.9277 0.729142 11.8922 0.73261C11.8769 0.734157 11.8616 0.737063 11.8462 0.73922C11.8263 0.742079 11.8063 0.74447 11.7867 0.748313C11.7691 0.751782 11.7517 0.756751 11.7343 0.76111C11.7172 0.765423 11.6998 0.769173 11.6831 0.774282C11.6661 0.779438 11.6495 0.785907 11.6329 0.79186C11.616 0.797907 11.5988 0.803485 11.5825 0.810329C11.567 0.816751 11.5519 0.824438 11.5367 0.831563C11.5196 0.839626 11.5023 0.847267 11.4857 0.856126C11.4713 0.863767 11.4575 0.872767 11.4434 0.881157C11.4269 0.890954 11.4101 0.900423 11.3941 0.911063C11.3789 0.921329 11.3643 0.93286 11.3494 0.944017C11.3357 0.954188 11.3217 0.963798 11.3084 0.974579C11.2823 0.996048 11.257 1.01902 11.2325 1.0433C11.2311 1.04447 11.2299 1.0455 11.2287 1.04663L6.13762 6.13758C5.71166 6.56363 5.71157 7.25433 6.13762 7.68033C6.56357 8.10638 7.25437 8.10628 7.68037 7.68042L10.9091 4.45186V17.8181C10.9091 18.4206 11.3976 18.909 12 18.909C12.6025 18.909 13.091 18.4206 13.091 17.8181V4.45191L16.3195 7.68042C16.5325 7.89342 16.8117 7.99997 17.0908 7.99997C17.37 7.99997 17.6492 7.89342 17.8622 7.68042C18.2883 7.25438 18.2883 6.56363 17.8622 6.13763Z " +
                    "M22.9091 11.6364C22.3066 11.6364 21.8182 12.1248 21.8182 12.7273V21.0909H2.1818V12.7273C2.1818 12.1248 1.69336 11.6364 1.09087 11.6364C0.488391 11.6364 0 12.1248 0 12.7273V22.1818C0 22.7843 0.488438 23.2727 1.09092 23.2727H22.9091C23.5116 23.2727 24 22.7843 24 22.1818V12.7273C24 12.1248 23.5116 11.6364 22.9091 11.6364Z";

    private static final String SVG_BACK =
            "M0 8.50732C0 8.06201 0.182454 7.65381 0.547363 7.28271L7.57031 0.41748C7.71257 0.281413 7.86719 0.17627 8.03418 0.102051C8.20736 0.0340169 8.3929 0 8.59082 0C8.86914 0 9.11963 0.0649414 9.34229 0.194824C9.57113 0.330892 9.75049 0.510254 9.88037 0.73291C10.0164 0.955566 10.0845 1.20296 10.0845 1.4751C10.0845 1.88949 9.92676 2.25439 9.61133 2.56982L3.10791 8.8877V8.13623L9.61133 14.4355C9.92676 14.751 10.0845 15.1128 10.0845 15.521C10.0845 15.7931 10.0164 16.0405 9.88037 16.2632C9.75049 16.4858 9.57113 16.6621 9.34229 16.792C9.11963 16.9281 8.86914 16.9961 8.59082 16.9961C8.1888 16.9961 7.84863 16.8569 7.57031 16.5786L0.547363 9.72266C0.361816 9.53711 0.225749 9.34538 0.139160 9.14746C0.0525716 8.94954 0.0061849 8.73617 0 8.50732Z";

    private final VBox dropdownMenu = new VBox();
    private boolean dropdownVisible = false;

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
    private final Button submitButton = new Button("Submit");
    private final Button backButton = new Button("Back");

    public NewQuestionView(ClientAPI client, StateManager stateManager, User user) {
        this.client = client;
        this.stateManager = stateManager;
        this.user = user;

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

        initOptions();
    }

    private VBox createCenterContent() {

        VBox root = new VBox(20);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(20, 0, 0, 0));

        Label questionLabel = new Label("Question");
        questionLabel.setStyle("""
                -fx-text-fill: white;
                -fx-font-size: 20px;
                -fx-font-weight: bold;
                """);
        HBox questionLabelBox = new HBox(questionLabel);
        questionLabelBox.setAlignment(Pos.CENTER_LEFT);
        questionLabelBox.setPadding(new Insets(0, 0, 0, -20));
        questionLabelBox.setMaxWidth(760);

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
        addOptionButton.setOnAction(e -> addOptionRow());

        HBox addOptionContainer = new HBox(addOptionButton);
        addOptionContainer.setAlignment(Pos.CENTER_LEFT);
        addOptionContainer.setPadding(new Insets(0, 0, 0, 18));
        addOptionContainer.setTranslateY(10);

        VBox optionsWithButton = new VBox(6, optionsColumn, addOptionContainer);
        optionsWithButton.setAlignment(Pos.TOP_LEFT);
        optionsWithButton.setPadding(new Insets(0, 0, 0, 38));

        HBox middleArea = new HBox(40, optionsWithButton, correctColumn, datesColumn);
        middleArea.setAlignment(Pos.TOP_CENTER);

        styleButton(submitButton, createUploadIcon());
        submitButton.setGraphicTextGap(35);
        styleButton(backButton, createBackIcon());

        submitButton.setOnAction(e -> handleSubmit());
        backButton.setOnAction(e -> {
            if (stateManager != null)
                stateManager.showMenu(user);
        });

        VBox buttonsBox = new VBox(40, submitButton, backButton);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.setSpacing(20);

        VBox.setMargin(buttonsBox, new Insets(50, 0, 0, 0));

        root.getChildren().addAll(
                questionLabelBox,
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

    private void initOptions() {
        addOptionRow();
        addOptionRow();
        addOptionRow();
    }

    private void addOptionRow() {
        char letter = (char) ('a' + optionRows.size());

        Label letterLabel = new Label(letter + ")");
        letterLabel.setStyle("""
            -fx-text-fill: white;
            -fx-font-size: 12px;
            """);

        TextField optionField = new TextField();
        optionField.setPromptText("Option");
        optionField.setPrefWidth(230);
        optionField.setMinHeight(28);
        optionField.setStyle("""
            -fx-background-color: white;
            -fx-text-fill: black;
            -fx-font-size: 13px;
            -fx-padding: 2 8;
            -fx-background-radius: 8;
            -fx-border-color: transparent;
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

            java.time.LocalDate.of(y, m, d);
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
            int min = Integer.parseInt(p[1]);

            return h >= 0 && h <= 23 && min >= 0 && min <= 59;
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

    private void handleSubmit() {
        overlay.setVisible(true);
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

        Label title = new Label("Question Submitted!");
        title.setStyle("""
        -fx-font-size: 16px;
        -fx-text-fill: black;
        -fx-font-weight: bold;
    """);

        Label codeLabel = new Label("Code");
        codeLabel.setStyle("-fx-text-fill: black; -fx-font-size: 14px;");

        Label codeNumber = new Label("123456");
        codeNumber.setStyle("""
        -fx-text-fill: #FF7A00;
        -fx-font-size: 18px;
        -fx-font-weight: bold;
    """);

        box.getChildren().addAll(check, title, codeLabel, codeNumber);

        submitPopup = box;
        overlay.getChildren().add(submitPopup);
        StackPane.setAlignment(submitPopup, Pos.CENTER);

        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                overlay.setVisible(false);
            }
        });

    }
}