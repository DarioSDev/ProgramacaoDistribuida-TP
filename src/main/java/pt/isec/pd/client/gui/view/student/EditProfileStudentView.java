package pt.isec.pd.client.gui.view.student;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pt.isec.pd.client.ClientAPI;
import pt.isec.pd.client.StateManager;
import pt.isec.pd.common.User;

public class EditProfileStudentView extends BorderPane {

    private final ClientAPI client;
    private final StateManager stateManager;
    private final User user;

    private final TextField nameField = new TextField();
    private final TextField idNumberField = new TextField();
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmPasswordField = new PasswordField();

    private final Button saveButton = new Button("Save");
    private final Button cancelButton = new Button("Cancel");

    private static final String COLOR_BG = "#1A1A1A";
    private static final String COLOR_PRIMARY = "#FF7A00";

    private static final int FIELD_WIDTH = 280;
    private final StackPane overlayContainer = new StackPane();

    public EditProfileStudentView(ClientAPI client, StateManager stateManager, User user) {
        this.client = client;
        this.stateManager = stateManager;
        this.user = user;

        setStyle("-fx-background-color: " + COLOR_BG + ";");

        BorderPane main = new BorderPane();

        HeaderStudentView header = new HeaderStudentView(stateManager, user);
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

    private VBox buildContent() {
        styleField(nameField, "Name");
        styleField(idNumberField, "Id Number");
        styleField(emailField, "Email");
        styleField(passwordField, "Password");
        styleField(confirmPasswordField, "Confirm Password");

        VBox fields = new VBox(18,
                nameField,
                idNumberField,
                emailField,
                passwordField,
                confirmPasswordField
        );
        fields.setAlignment(Pos.CENTER);

        VBox root = new VBox(40, fields, saveButton);
        root.setAlignment(Pos.CENTER);

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
                    || idNumberField.getText().isBlank()
                    || emailField.getText().isBlank()) {
                showFeedback( "Please fill in all required fields.", false);
                return;
            }
            showFeedback("Profile updated successfully!", true);
        });

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

        cancelButton.setOnAction(e -> stateManager.showStudentMenu(user));

        HBox box = new HBox(cancelButton);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(0, 40, 40, 0));

        return box;
    }

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
        idNumberField.setText(user.getIdNumber());
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
