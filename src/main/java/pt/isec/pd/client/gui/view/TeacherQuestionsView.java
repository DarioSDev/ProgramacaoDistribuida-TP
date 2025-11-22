package pt.isec.pd.client.gui.view;

import javafx.scene.layout.BorderPane;
import pt.isec.pd.client.ClientAPI;
import pt.isec.pd.client.StateManager;
import pt.isec.pd.common.User;

public class TeacherQuestionsView extends BorderPane {

    private final ClientAPI client;
    private final StateManager stateManager;
    private final User user;


    public TeacherQuestionsView(ClientAPI client, StateManager stateManager, User user) {
        this.client = client;
        this.stateManager = stateManager;
        this.user = user;

    }

}