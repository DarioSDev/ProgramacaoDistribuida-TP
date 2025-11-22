package pt.isec.pd.client.gui.view;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import pt.isec.pd.client.ClientAPI;
import pt.isec.pd.client.StateManager;
import pt.isec.pd.common.User;

public class EditProfileTeacherView extends BorderPane {
    private final ClientAPI client;
    private final StateManager stateManager;
    private final User user;

    private final TextField nameField = new TextField();
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmPasswordField = new PasswordField();

    private final Button saveButton = new Button("Save");
    private final Button cancelButton = new Button("Cancel");

    private StackPane menuButton;

    private static final String COLOR_PRIMARY = "#FF7A00";
    private static final String COLOR_BG = "#1A1A1A";
    private static final String COLOR_BUTTON_TEXT = "#000000";
    private static final String COLOR_DROPDOWN_PROFILE = "#D9D9D9";
    private static final String COLOR_HOVER = "#F4B27C";
    private static final String COLOR_DROPDOWN_TEXT = "#1E1E1E";

    private static final int DROPDOWN_WIDTH = 200;
    private static final double DROPDOWN_OFFSET_Y = 130;
    private static final int FIELD_WIDTH = 300;
    private static final String DROPDOWN_RADIUS = "12";

    private static final String SVG_MENU =
            "M3.59615 7.25H17.9038C18.2331 7.25 18.5 6.91421 18.5 6.5C18.5 6.08579 "
                    + "18.2331 5.75 17.9038 5.75H3.59615ZM3 11C3 10.5858 3.26691 10.25 "
                    + "3.59615 10.25H17.9038C18.2331 10.25 18.5 10.5858 18.5 11C18.5 "
                    + "11.4142 18.2331 11.75 17.9038 11.75H3.59615C3.26691 11.75 3 "
                    + "11.4142 3 11ZM3.59615 14.75C3.26691 14.75 3 15.0858 3 15.5C3 "
                    + "15.9142 3.26691 16.25 3.59615 16.25H17.9038C18.2331 16.25 18.5 "
                    + "15.9142 18.5 15.5C18.5 15.0858 18.2331 14.75 17.9038 14.75H3.59615Z";

    private static final String SVG_LOGOUT =
            "M14.7961 0H12.1961C8.99609 0 6.99609 2 6.99609 5.2V9.25H13.2461C13.6561 "
                    + "9.25 13.9961 9.59 13.9961 10C13.9961 10.41 13.6561 10.75 13.2461 "
                    + "10.75H6.99609V14.8C6.99609 18 8.99609 20 12.1961 20H14.7861C17.9861 " +
                    "20 19.9861 18 19.9861 14.8V5.2C19.9961 2 17.9961 0 14.7961 0Z " +
                    "M2.5575 9.2498L4.6275 7.17984C4.7775 7.02984 4.8475 6.83984 " +
                    "4.8475 6.64984C4.8475 6.45984 4.7775 6.25984 4.6275 6.11984C4.3375 " +
                    "5.82984 3.8575 5.82984 3.5675 6.11984L0.2175 9.4698C-0.0725 9.7598 " +
                    "-0.0725 10.2398 0.2175 10.5298L3.5675 13.8798C3.8575 14.1698 4.3375 " +
                    "14.1698 4.6275 13.8798C4.9175 13.5898 4.9175 13.1098 4.6275 " +
                    "12.8198L2.5575 10.7498H6.9975V9.2498H2.5575Z";

    private static final String SVG_PROFILE =
            "M23.3792 3.50708H20.4692L17.1526 0.190441C16.8987 -0.0634803 16.4871 -0.0634803 16.2332 0.190441L12.9166 3.50698H10.0065C9.67322 3.50698 9.40308 3.77717 9.40308 4.11045V8.63899L10.0744 8.22218C10.555 7.92382 11.137 7.88566 11.6404 8.09111L13.8894 6.31774C14.2278 6.05093 14.7183 6.10905 14.985 6.4473C15.2518 6.7856 15.1937 7.27615 14.8554 7.54286L12.663 9.27156C12.8403 9.98064 12.552 10.7526 11.8978 11.1588L10.61 11.9584L9.40312 12.7078V14.021C9.40312 14.3544 9.67331 14.6245 10.0066 14.6245H23.3792C23.7125 14.6245 23.9827 14.3543 23.9827 14.021V4.11054C23.9827 3.77726 23.7125 3.50708 23.3792 3.50708ZM14.7553 3.50708L16.6929 1.5695L18.6305 3.50708H14.7553Z " +
                    "M11.7203 9.23481C11.4685 8.82929 10.9357 8.7047 10.5303 8.95647L8.78011 10.0431C8.77284 8.59169 8.77669 9.36217 8.772 8.4272C8.76637 7.30525 7.84894 6.39246 6.72694 6.39246H5.87672C5.66054 6.95313 5.11543 8.36687 4.95221 8.7902L6.68194 9.58422V8.43405C6.68194 8.33416 6.76289 8.25316 6.86283 8.25316C6.96277 8.25316 7.04372 8.33411 7.04372 8.43405C7.04372 8.43466 7.04372 8.43531 7.04372 8.43592L7.0628 11.6638C7.11666 12.3111 7.83577 12.6638 8.37956 12.3262L11.442 10.4247C11.8475 10.1731 11.9721 9.64032 11.7203 9.23481Z " +
                    "M2.10455 13.511L2.10498 22.963C2.10498 23.5358 2.56922 24 3.14199 24C3.71475 24 4.179 23.5358 4.179 22.963V14.6741H4.57289C4.48983 14.6468 2.10455 13.511 2.10455 13.511Z " +
                    "M6.68194 12.4288L5.92158 14.0851C5.69827 14.5715 5.14815 14.833 4.62671 14.6904V22.963C4.62671 23.5357 5.09096 24 5.66372 24C6.23649 24 6.70074 23.5357 6.70074 22.963C6.6998 22.4403 6.68194 13.005 6.68194 12.4288Z " +
                    "M4.99039 7.79854L4.63901 7.15955L4.95073 6.59255C4.99718 6.50803 4.93596 6.4043 4.8394 6.4043H3.98295C3.88653 6.4043 3.82512 6.50794 3.87162 6.59255L4.18404 7.16067L3.83206 7.80084C3.79119 7.87523 3.77787 7.96153 3.79437 8.04473L3.91756 8.66517C4.28637 8.53115 4.56317 8.61168 4.87671 8.75559L5.02718 8.04698C5.04509 7.96247 5.03201 7.87425 4.99039 7.79854Z " +
                    "M6.47404 10.2328L4.47216 9.31376C4.25663 9.21485 4.002 9.30949 3.90319 9.5247L3.38531 10.6529L4.43972 11.2387C5.19295 11.6572 5.46431 12.607 5.04581 13.3602C4.91597 13.594 4.73452 13.7808 4.52395 13.9155L4.73831 14.0139C4.95384 14.1128 5.20847 14.0182 5.30728 13.803L6.68498 10.8017C6.78384 10.5864 6.68939 10.3316 6.47404 10.2328Z " +
                    "M4.40287 5.82162C5.39201 5.82162 6.19387 5.01976 6.19387 4.03062C6.19387 3.04148 5.39201 2.23962 4.40287 2.23962C3.41373 2.23962 2.61188 3.04148 2.61188 4.03062C2.61188 5.01976 3.41373 5.82162 4.40287 5.82162Z " +
                    "M4.10164 11.8471L1.74876 10.5398C1.75358 9.75301 1.75719 9.17406 1.76169 8.43587C1.76221 8.34128 1.83913 8.26502 1.93363 8.26525C2.02813 8.26548 2.10458 8.34222 2.10458 8.43676V9.94126L2.79257 10.3235L3.28878 9.24264C3.40124 8.99762 3.59792 8.80698 3.83492 8.69861C3.73794 8.44703 3.04321 6.6453 2.94571 6.39246H2.07843C0.956382 6.39246 0.0390398 7.3053 0.0334148 8.42622C0.031071 8.80464 0.0196336 10.6637 0.0172898 11.0412C0.0153211 11.3569 0.185758 11.6486 0.461758 11.8019L3.26225 13.3578C3.67939 13.5897 4.20556 13.4394 4.43736 13.0222C4.66911 12.605 4.51883 12.0789 4.10164 11.8471Z";

    private final VBox dropdownMenu = new VBox();
    private boolean dropdownVisible = false;

    public EditProfileTeacherView(ClientAPI client, StateManager stateManager, User user) {
        this.client = client;
        this.stateManager = stateManager;
        this.user = user;

        setStyle("-fx-background-color: " + COLOR_BG + ";");

        setupDropdown();

        BorderPane page = new BorderPane();
        page.setTop(buildHeader());
        page.setCenter(buildContent());
        page.setBottom(buildFooter());

        StackPane root = new StackPane();
        root.getChildren().addAll(page, dropdownMenu);

        dropdownMenu.toFront();
        StackPane.setAlignment(dropdownMenu, Pos.TOP_RIGHT);

        setCloseOnExternalClick(root);

        setCenter(root);
        loadUserData();
    }

    private void setCloseOnExternalClick(StackPane root) {
        root.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {

            if (!dropdownVisible) return;

            Bounds bounds = dropdownMenu.localToScene(dropdownMenu.getBoundsInLocal());

            double x = e.getSceneX();
            double y = e.getSceneY();

            boolean inside =
                    x >= bounds.getMinX() &&
                            x <= bounds.getMaxX() &&
                            y >= bounds.getMinY() &&
                            y <= bounds.getMaxY();

            if (!inside) {
                toggleDropdown();
            }
        });
    }

    private BorderPane buildHeader() {
        Label title = new Label("Questia");
        title.setStyle(String.format("""
            -fx-font-size: 80px;
            -fx-text-fill: %s;
            -fx-font-weight: bold;
        """, COLOR_PRIMARY));

        VBox titleArea = new VBox(0, title);
        titleArea.setAlignment(Pos.CENTER_LEFT);

        menuButton = createMenuButton();
        menuButton.setOnMouseClicked(e -> {
            e.consume();
            toggleDropdown();
        });

        dropdownMenu.setTranslateX(-40);
        dropdownMenu.setTranslateY(DROPDOWN_OFFSET_Y);

        BorderPane header = new BorderPane();
        header.setLeft(titleArea);
        header.setRight(menuButton);

        BorderPane.setAlignment(titleArea, Pos.CENTER_LEFT);
        BorderPane.setAlignment(menuButton, Pos.CENTER_RIGHT);

        BorderPane.setMargin(titleArea, new Insets(40, 0, 0, 40));
        BorderPane.setMargin(menuButton, new Insets(35, 40, 0, 0));

        return header;
    }

    private StackPane createMenuButton() {
        StackPane menuButton = new StackPane();
        menuButton.setCursor(Cursor.HAND);

        final double BUTTON_SIZE = 50;
        menuButton.setPrefSize(BUTTON_SIZE, BUTTON_SIZE);
        menuButton.setMaxSize(BUTTON_SIZE, BUTTON_SIZE);
        menuButton.setMinSize(BUTTON_SIZE, BUTTON_SIZE);

        menuButton.setPadding(new Insets(11));

        menuButton.setStyle(String.format("""
            -fx-background-color: %s;
            -fx-background-radius: 10;
        """, COLOR_PRIMARY));

        SVGPath menuIcon = new SVGPath();
        menuIcon.setContent(SVG_MENU);
        menuIcon.setFill(Color.web(COLOR_BUTTON_TEXT));

        menuIcon.setScaleX(1.0);
        menuIcon.setScaleY(1.0);

        menuButton.getChildren().add(menuIcon);

        return menuButton;
    }

    private void setupDropdown() {
        dropdownMenu.setVisible(false);
        dropdownMenu.setManaged(false);
        dropdownMenu.setSpacing(0);
        dropdownMenu.setPadding(Insets.EMPTY);
        dropdownMenu.setFillWidth(true);

        dropdownMenu.setPrefWidth(DROPDOWN_WIDTH);
        dropdownMenu.setMaxWidth(Region.USE_PREF_SIZE);
        dropdownMenu.setMinWidth(Region.USE_PREF_SIZE);

        dropdownMenu.setStyle("""
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 12, 0.1, 0, 3);
            -fx-background-color: transparent;
        """);

        Button menuBtn = createDropdownButton("Menu", SVG_PROFILE, COLOR_DROPDOWN_PROFILE);
        Button logoutBtn = createDropdownButton("Logout", SVG_LOGOUT, COLOR_DROPDOWN_PROFILE);

        menuBtn.setStyle(menuBtn.getStyle() + String.format(
                "-fx-background-radius: %s %s 0 0;",
                DROPDOWN_RADIUS, DROPDOWN_RADIUS
        ));

        logoutBtn.setStyle(logoutBtn.getStyle() + String.format(
                "-fx-background-radius: 0 0 %s %s;",
                DROPDOWN_RADIUS, DROPDOWN_RADIUS
        ));

        applyDropdownHoverEffect(menuBtn, COLOR_DROPDOWN_PROFILE);
        applyDropdownHoverEffect(logoutBtn, COLOR_DROPDOWN_PROFILE);

        menuBtn.setOnAction(e -> {
            toggleDropdown();
            stateManager.showTeacherMenu(user);
        });

        logoutBtn.setOnAction(e -> {
            toggleDropdown();
            stateManager.showLogin();
        });

        dropdownMenu.getChildren().setAll(menuBtn, logoutBtn);
    }

    private Button createDropdownButton(String text, String svgContent, String bgColor) {
        Button btn = new Button(text);
        btn.setCursor(Cursor.HAND);
        btn.setBorder(Border.EMPTY);
        btn.setGraphic(createDropdownIcon(svgContent));
        btn.setGraphicTextGap(30);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);

        btn.setStyle(String.format("""
        -fx-background-color: %s;
        -fx-font-size: 16px;
        -fx-text-fill: %s;
        -fx-padding: 10 22;
    """, bgColor, COLOR_DROPDOWN_TEXT));

        return btn;
    }

    private void applyDropdownHoverEffect(Button btn, String originalBgColor) {
        String originalStyle = btn.getStyle();
        String hoverBgColorStyle = String.format("-fx-background-color: %s;", COLOR_HOVER);
        String originalBgColorStyle = String.format("-fx-background-color: %s;", originalBgColor);

        btn.setOnMouseEntered(e -> {
            String newStyle = originalStyle.replace(originalBgColorStyle, hoverBgColorStyle);
            btn.setStyle(newStyle);
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle(originalStyle);
        });
    }

    private SVGPath createDropdownIcon(String svgContent) {
        SVGPath svg = new SVGPath();
        svg.setContent(svgContent);
        svg.setFill(Color.web(COLOR_PRIMARY));
        svg.setScaleX(1.0);
        svg.setScaleY(1.0);
        return svg;
    }

    private void toggleDropdown() {
        dropdownVisible = !dropdownVisible;
        dropdownMenu.setVisible(dropdownVisible);
        dropdownMenu.setManaged(dropdownVisible);
    }

    private VBox buildContent() {
        styleField(nameField, "Professor Mock");
        styleField(emailField, "teacher@isec.pt");
        styleField(passwordField, "Password");
        styleField(confirmPasswordField, "Confirm Password");

        VBox fields = new VBox(18, nameField, emailField, passwordField, confirmPasswordField);
        fields.setAlignment(Pos.CENTER);

        saveButton.setStyle("""
            -fx-background-color:#FF7A00;
            -fx-text-fill:#1E1E1E;
            -fx-font-size:16px;
            -fx-font-weight:bold;
            -fx-background-radius:10px;
            -fx-padding:10 30;
        """);
        saveButton.setPrefWidth(150);

        HBox saveArea = new HBox(saveButton);
        saveArea.setAlignment(Pos.CENTER);

        VBox root = new VBox(40, fields, saveArea);
        root.setAlignment(Pos.CENTER);

        return root;
    }

    private HBox buildFooter() {
        cancelButton.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: white;
            -fx-border-color: #FF0000;
            -fx-border-width: 2;
            -fx-background-radius: 5;
            -fx-border-radius: 5;
            -fx-font-size:16px;
            -fx-padding: 5 10;
        """);
        cancelButton.setPrefWidth(100);

        cancelButton.setOnAction(e -> {
            if (dropdownVisible) {
                toggleDropdown();
            }
            stateManager.showTeacherMenu(user);
        });

        HBox box = new HBox(cancelButton);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(0, 40, 50, 0));

        return box;
    }

    private void styleField(TextField field, String placeholder) {
        field.setPromptText(placeholder);
        field.setPrefWidth(FIELD_WIDTH);
        field.setMaxWidth(FIELD_WIDTH);

        field.getStyleClass().remove("text-field");

        field.setStyle("""
            -fx-background-color:white;
            -fx-text-fill:#1E1E1E;
            -fx-prompt-text-fill:#1E1E1E;
            -fx-padding:10;
            -fx-background-radius:10;
            -fx-border-radius:10;
            -fx-font-size:15px;
        """);
    }

    private void loadUserData() {
        nameField.setText(user.getName());
        emailField.setText(user.getEmail());
        nameField.setPromptText("Professor Mock");
        emailField.setPromptText("teacher@isec.pt");
    }
}