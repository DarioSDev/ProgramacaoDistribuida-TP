package pt.isec.pd.client;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import pt.isec.pd.client.gui.view.*;
import pt.isec.pd.common.User;

public class StateManager {

    private final Stage stage;
    private final ClientAPI client;

    private static final double WIDTH = 900;
    private static final double HEIGHT = 700;

    public StateManager(Stage stage, ClientAPI clientAPI) {
        this.stage = stage;
        this.client = clientAPI;

        stage.setResizable(true);
        stage.setMinWidth(WIDTH);
        stage.setMinHeight(HEIGHT);
    }

    private void setScene(javafx.scene.Parent root, String title) {
        StackPane wrapper = new StackPane(root);
        wrapper.setStyle("-fx-background-color: #1a1a1a;");
        Scene scene = new Scene(wrapper, WIDTH, HEIGHT);
        stage.setScene(scene);
        stage.setTitle(title);
        stage.centerOnScreen();
        stage.show();
    }

    public void showLogin() {
        setScene(new LoginView(client, this), "Questia - Login");
    }

    public void showRegister() {
        setScene(new RegisterView(client, this), "Questia - Register");
    }

    public void showMenu(User user) {
        if ("teacher".equalsIgnoreCase(user.getRole()))
            showTeacherMenu(user);
    }

    public void showTeacherMenu(User user) {
        setScene(new MenuTeacherView(client, this, user), "Questia - Teacher Menu");
    }

    public void showNewQuestionView(User user) {
        setScene(new NewQuestionView(client, this, user), "Questia - New Question");
    }

    public void showQuestionHistory(User user) {
        setScene(new QuestionHistoryView(client, this, user), "Questia - Question History");
    }

    public void showEditProfile(User user) {
        if ("teacher".equalsIgnoreCase(user.getRole()))
            setScene(new EditProfileTeacherView(client, this, user), "Questia - Edit Profile");
    }
}