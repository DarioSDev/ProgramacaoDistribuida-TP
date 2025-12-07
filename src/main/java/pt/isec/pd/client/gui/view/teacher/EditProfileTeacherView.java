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
import pt.isec.pd.common.entities.User;

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

    private final VBox dropdownMenu = new VBox();
    private boolean dropdownVisible = false;
    private final StackPane overlayContainer = new StackPane();

    private static final String COLOR_BG = "#1A1A1A";
    private static final String COLOR_PRIMARY = "#FF7A00";
    private static final String COLOR_DROPDOWN_BG = "#D9D9D9";
    private static final String COLOR_DROPDOWN_TEXT = "#1E1E1E";
    private static final String COLOR_HOVER = "#F4B27C";
    private static final String COLOR_DROPDOWN_PROFILE = "#D9D9D9";
    private static final String COLOR_DROPDOWN_LOGOUT = "#F4B27C";

    private static final int DROPDOWN_WIDTH = 200;

    private static final String SVG_LOGOUT =
            "M14.7961 0H12.1961C8.99609 0 6.99609 2 6.99609 5.2V9.25H13.2461C13.6561 "
                    + "9.25 13.9961 9.59 13.9961 10C13.9961 10.41 13.6561 10.75 13.2461 "
                    + "10.75H6.99609V14.8C6.99609 18 8.99609 20 12.1961 20H14.7861C17.9861 20 "
                    + "19.9861 18 19.9861 14.8V5.2C19.9961 2 17.9961 0 14.7961 0Z "
                    + "M2.5575 9.2498L4.6275 7.17984C4.7775 7.02984 4.8475 6.83984 "
                    + "4.8475 6.64984C4.8475 6.45984 4.7775 6.25984 4.6275 "
                    + "6.11984C4.3375 5.82984 3.8575 5.82984 3.5675 6.11984L0.2175 "
                    + "9.4698C-0.0725 9.7598 -0.0725 10.2398 0.2175 10.5298L3.5675 "
                    + "13.8798C3.8575 14.1698 4.3375 14.1698 4.6275 13.8798C4.9175 "
                    + "13.5898 4.9175 13.1098 4.6275 12.8198L2.5575 10.7498H6.9975V9.2498H2.5575Z";

    private static final String SVG_HOME =
            "M10 20V14H14V20H19V12H22L12 3 2 12H5V20H10Z";

    public EditProfileTeacherView(ClientAPI client, StateManager stateManager, User user) {
        this.client = client;
        this.stateManager = stateManager;
        this.user = user;

        setStyle("-fx-background-color: " + COLOR_BG + ";");

        BorderPane main = new BorderPane();

        HeaderView header = new HeaderView(stateManager, user);
        main.setTop(header);

        main.setCenter(buildContent());
        main.setBottom(buildFooter());

        StackPane root = new StackPane(main, overlayContainer);
        header.attachToRoot(root);

        overlayContainer.setMouseTransparent(true);
        StackPane.setAlignment(overlayContainer, Pos.CENTER);
        this.setCenter(root);

        loadUserData();
    }

    private void applyDropdownHoverEffect(Button btn, String normalColor) {
        String normal = "-fx-background-color: " + normalColor + ";";
        String hover = "-fx-background-color: " + COLOR_HOVER + ";";

        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle().replace(normal, hover)));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().replace(hover, normal)));
    }

    private void toggleDropdown() {
        dropdownVisible = !dropdownVisible;
        dropdownMenu.setVisible(dropdownVisible);
        dropdownMenu.setManaged(dropdownVisible);
    }

    private void setupDropdown() {
        dropdownMenu.setVisible(false);
        dropdownMenu.setManaged(false);
        dropdownMenu.setSpacing(0);
        dropdownMenu.setPadding(Insets.EMPTY);

        dropdownMenu.setPrefWidth(DROPDOWN_WIDTH);
        dropdownMenu.setMinWidth(DROPDOWN_WIDTH);
        dropdownMenu.setMaxWidth(DROPDOWN_WIDTH);
        dropdownMenu.setFillWidth(true);

        dropdownMenu.setStyle("""
        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 12, 0.1, 0, 3);
        -fx-background-color: transparent;
    """);

        Button menuBtn = createDropdownButton("Home", SVG_HOME, COLOR_DROPDOWN_PROFILE);
        menuBtn.setStyle(menuBtn.getStyle() + "-fx-background-radius: 12 12 0 0;");

        menuBtn.setOnAction(e -> {
            toggleDropdown();
            stateManager.showTeacherMenu(user);
        });

        Button logoutBtn = createDropdownButton("Logout", SVG_LOGOUT, COLOR_DROPDOWN_PROFILE);
        logoutBtn.setStyle(logoutBtn.getStyle() + "-fx-background-radius: 0 0 12 12;");

        logoutBtn.setOnAction(e -> {
            toggleDropdown();
            stateManager.showLogin();
        });

        applyDropdownHoverEffect(menuBtn, COLOR_DROPDOWN_PROFILE);
        applyDropdownHoverEffect(logoutBtn, COLOR_DROPDOWN_PROFILE);

        dropdownMenu.getChildren().setAll(menuBtn, logoutBtn);
    }

    private Button createDropdownButton(String text, String iconSVG, String bgColor) {
        Button btn = new Button(text);
        btn.setCursor(Cursor.HAND);

        btn.setGraphic(makeIcon(iconSVG));
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

    private SVGPath makeIcon(String content) {
        SVGPath svg = new SVGPath();
        svg.setContent(content);
        svg.setFill(Color.web(COLOR_PRIMARY));
        return svg;
    }

    private void applyHover(Button btn) {
        String original = btn.getStyle();

        btn.setOnMouseEntered(e ->
                btn.setStyle(original.replace(COLOR_DROPDOWN_BG, COLOR_HOVER))
        );

        btn.setOnMouseExited(e ->
                btn.setStyle(original)
        );
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
            -fx-background-radius:10;
            -fx-padding:10 30;
        """);

        saveButton.setOnAction(e -> {

            if (nameField.getText().isBlank()
                    || emailField.getText().isBlank()) {
                showFeedback("Please fill in all required fields.", false);
                return;
            }
            showFeedback("Profile updated successfully!", true);
        });


        VBox root = new VBox(40, fields, saveButton);
        root.setAlignment(Pos.CENTER);
        return root;
    }


    private HBox buildFooter() {
        cancelButton.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: white;
            -fx-border-color: red;
            -fx-border-width: 2;
            -fx-background-radius: 5;
            -fx-border-radius: 5;
            -fx-font-size:16px;
            -fx-padding: 5 20;
        """);

        cancelButton.setOnAction(e -> stateManager.showTeacherMenu(user));

        HBox box = new HBox(cancelButton);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(0, 40, 40, 0));

        return box;
    }

    private static final int FIELD_WIDTH = 280;

    private void styleField(TextField field, String placeholder) {
        field.setPromptText(placeholder);

        field.setPrefWidth(FIELD_WIDTH);
        field.setMaxWidth(FIELD_WIDTH);

        field.setStyle("""
        -fx-background-color: white;
        -fx-text-fill: black;
        -fx-padding: 10;
        -fx-background-radius: 10;
        -fx-border-radius: 10;
        -fx-font-size: 14px;
    """);
    }

    private void loadUserData() {
        nameField.setText(user.getName());
        emailField.setText(user.getEmail());
    }

    private void showFeedback(String message, boolean success) {
        Label msg = new Label(message);
        msg.setStyle("""
        -fx-background-color: %s;
        -fx-text-fill: #1E1E1E;
        -fx-font-size: 16px;
        -fx-padding: 12 22;
        -fx-background-radius: 12;
    """.formatted(success ? "#8FFF8F" : "#FF8F8F"));

        msg.setOpacity(0);
        overlayContainer.getChildren().add(msg);

        javafx.animation.FadeTransition fadeIn =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), msg);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        javafx.animation.FadeTransition fadeOut =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(250), msg);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(javafx.util.Duration.seconds(2));

        fadeOut.setOnFinished(e -> overlayContainer.getChildren().remove(msg));

        fadeIn.play();
        fadeOut.play();
    }

}
