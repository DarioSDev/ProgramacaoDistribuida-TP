package pt.isec.pd.client;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.isec.pd.client.gui.view.*;
import pt.isec.pd.client.gui.view.student.EditProfileStudentView;
import pt.isec.pd.client.gui.view.student.MenuStudentView;
import pt.isec.pd.client.gui.view.student.QuestionView;
import pt.isec.pd.client.gui.view.student.StudentQuestionHistoryView;
import pt.isec.pd.client.gui.view.teacher.*;
import pt.isec.pd.common.entities.Question;
import pt.isec.pd.common.dto.TeacherResultsData;
import pt.isec.pd.common.entities.User;

public class StateManager {

    private static final Logger log = LoggerFactory.getLogger(StateManager.class);
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
        if ("teacher".equalsIgnoreCase(user.getRole())) {
            setScene(new EditProfileTeacherView(client, this, user),
                    "Questia - Edit Profile");
        } else if ("student".equalsIgnoreCase(user.getRole())) {
            setScene(new EditProfileStudentView(client, this, user),
                    "Questia - Edit Profile");
        }
    }

    public void showEditQuestionView(User user, Question question) {
        setScene(new EditQuestionView(client, this, user, question), "Edit Question");
    }

    public void showCheckQuestionDataView(User user, TeacherResultsData results) {
        setScene(new CheckQuestionDataView(client, this, user, results),
                "Questia - Question Results");
    }

    public void showStudentMenu(User user) {
        setScene(new MenuStudentView(client, this, user),
                "Questia - Student Menu");
    }

    public void showMenu(User user) {
        UserManager.getInstance()
                .setUser(user)
                .setRole(user.getRole())
                .setLoggedIn(true);
        if ("teacher".equalsIgnoreCase(user.getRole()))
            showTeacherMenu(user);
        else if ("student".equalsIgnoreCase(user.getRole()))
            showStudentMenu(user);
    }

    public void showStudentHistory(User user) {
        setScene(new StudentQuestionHistoryView(client, this, user),
                "Questia - History");
    }

    public void showQuestionView(User user, String code) {
        setScene(new QuestionView(client, this, user, code),
                "Questia - Question");
    }

}
