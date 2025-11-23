package pt.isec.pd.client.gui.view;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import pt.isec.pd.client.ClientAPI;
import pt.isec.pd.client.StateManager;
import pt.isec.pd.common.User;

public class LoginView extends BorderPane {
    private final ClientAPI clientService;
    private final StateManager stateManager;

    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Button loginButton = new Button("Login");
    private final Button registerButton = new Button("Register");
    private final Label messageLabel = new Label();

    private static final String COLOR_PRIMARY = "#FF7A00";
    private static final String COLOR_BG = "#1a1a1a";
    private static final String BUTTON_STYLE =
            "-fx-font-weight: bold;" +
                    "-fx-cursor: hand;" +
                    "-fx-background-radius: 8px;";

    public LoginView(ClientAPI clientService, StateManager stateManager) {
        this.clientService = clientService;
        this.stateManager = stateManager;

        setStyle("-fx-background-color: " + COLOR_BG + ";");

        VBox content = createCenter();
        setCenter(content);

        paddingProperty().bind(
                Bindings.createObjectBinding(
                        () -> new Insets(getHeight() * 0.08, 0, getHeight() * 0.08, 0),
                        heightProperty()
                )
        );

        setupActions();
    }

    private VBox createCenter() {
        Label title = new Label("Questia");

        title.styleProperty().bind(
                Bindings.createStringBinding(
                        () -> String.format(
                                "-fx-font-size: %.0fpx; -fx-text-fill: %s; -fx-font-weight: bold;",
                                Math.max(48, getHeight() * 0.13),
                                COLOR_PRIMARY
                        ),
                        heightProperty()
                )
        );

        styleInput(emailField, "Email");
        styleInput(passwordField, "Password");

        VBox inputs = new VBox(20, emailField, passwordField);
        inputs.setAlignment(Pos.CENTER);

        emailField.prefWidthProperty().bind(
                Bindings.min(400, widthProperty().multiply(0.35))
        );
        emailField.maxWidthProperty().bind(emailField.prefWidthProperty());

        passwordField.prefWidthProperty().bind(
                Bindings.min(400, widthProperty().multiply(0.35))
        );
        passwordField.maxWidthProperty().bind(passwordField.prefWidthProperty());

        loginButton.setStyle(
                BUTTON_STYLE +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 8px 20px;" +
                        "-fx-background-color: " + COLOR_PRIMARY + ";" +
                        "-fx-text-fill: #0F0F0F;"
        );

        registerButton.setStyle(
                BUTTON_STYLE +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 8px 20px;" +
                        "-fx-background-color: transparent;" +
                        "-fx-border-color: " + COLOR_PRIMARY + ";" +
                        "-fx-border-width: 2px;" +
                        "-fx-text-fill: #A0A0A0;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-border-radius: 8px;"
        );

        loginButton.prefWidthProperty().bind(
                Bindings.min(220, widthProperty().multiply(0.17))
        );
        registerButton.prefWidthProperty().bind(
                Bindings.min(180, widthProperty().multiply(0.14))
        );

        messageLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

        VBox buttons = new VBox();
        buttons.setAlignment(Pos.CENTER);
        buttons.getChildren().addAll(loginButton, registerButton, messageLabel);

        buttons.spacingProperty().bind(heightProperty().multiply(0.03));

        VBox content = new VBox();
        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(title, inputs, buttons);

        content.spacingProperty().bind(heightProperty().multiply(0.05));

        return content;
    }

    private void styleInput(TextField field, String prompt) {
        field.setPromptText(prompt);
        field.setStyle(
                "-fx-background-color: white;" +
                        "-fx-text-fill: black;" +
                        "-fx-prompt-text-fill: #1E1E1E;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 8px;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-border-color: transparent;"
        );
    }

    private void setupActions() {
        loginButton.setOnAction(e -> handleLogin());
        registerButton.setOnAction(e -> stateManager.showRegister());
    }

    private void handleLogin() {
        String email = getEmail();
        String password = getPassword();

        if (email.isBlank() || password.isBlank()) {
            setMessage("Preencha todos os campos.", true);
            return;
        }

        setControlsDisabled(true);
        setMessage("A ligar ao servidor...", false);

        new Thread(() -> {
            try {
                String response = clientService.sendLogin(email, password);

                Platform.runLater(() -> {
                    if (response == null) {
                        setMessage("Servidor não respondeu.", true);
                    } else if (response.startsWith("OK")) {
                        String[] parts = response.split(";");
                        String role = parts.length > 1 ? parts[1] : "student";
                        String name = parts.length > 2 ? parts[2] : email;
                        String extra = parts.length > 3 ? parts[3] : "";

                        User u = new User(name, email, password, role, extra);
                        stateManager.showMenu(u);
                    } else {
                        setMessage("Credenciais inválidas.", true);
                    }

                    setControlsDisabled(false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    setMessage("Erro de ligação ao servidor.", true);
                    setControlsDisabled(false);
                });
            }
        }).start();
    }

    public String getEmail() { return emailField.getText(); }
    public String getPassword() { return passwordField.getText(); }

    public void setMessage(String msg, boolean error) {
        messageLabel.setText(msg);
        if (error)
            messageLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        else
            messageLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
    }

    public void setControlsDisabled(boolean disabled) {
        loginButton.setDisable(disabled);
        registerButton.setDisable(disabled);
        emailField.setDisable(disabled);
        passwordField.setDisable(disabled);
    }

    public void setLoginAction(EventHandler<ActionEvent> handler) {
        loginButton.setOnAction(handler);
    }

    public void setRegisterAction(EventHandler<ActionEvent> handler) {
        registerButton.setOnAction(handler);
    }
}
