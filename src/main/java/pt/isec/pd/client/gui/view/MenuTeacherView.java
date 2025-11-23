package pt.isec.pd.client.gui.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import pt.isec.pd.client.ClientAPI;
import pt.isec.pd.client.StateManager;
import pt.isec.pd.common.User;

public class MenuTeacherView extends BorderPane {

    private final ClientAPI client;
    private final StateManager stateManager;
    private final User user;

    private static final String COLOR_PRIMARY = "#FF7A00";
    private static final String COLOR_BG = "#1A1A1A";
    private static final String COLOR_BUTTON_TEXT = "#000000";

    private static final String SVG_NEW_QUESTION =
            "M23.547 3.06604L20.913 0.429786C20.3302 -0.152214 19.3875 -0.152214 18.8055 0.429786L16.2367 3.00004H18.3442L19.332 2.01229C19.623 1.72054 20.0948 1.72054 20.3865 2.01229L21.9668 3.59329C22.2578 3.88504 22.2578 4.35679 21.9668 4.64779L21 5.61529V7.72353L23.547 5.17429C24.129 4.59304 24.129 3.64804 23.547 3.06604ZM8.26799 13.083C8.54774 13.3628 9.91501 14.7308 10.902 15.7193L19.332 7.28404L16.6793 4.66654L8.26799 13.083ZM5.03476 18.4283C4.86376 18.7718 5.19375 19.1258 5.5605 18.9548L9.58951 16.515L7.47377 14.3963L5.03476 18.4283ZM11.184 17.1555L4.65976 20.3828C3.92476 20.724 3.33675 20.0655 3.6075 19.3298L6.83249 12.801C6.88799 12.5198 6.99601 12.2468 7.21426 12.0285L16.2367 3.00004H2.09999C0.940491 3.00004 0 3.94054 0 5.10004V21.9C0 23.0595 0.940491 24 2.09999 24H18.9C20.0595 24 21 23.0595 21 21.9V7.72353L11.9557 16.7738C11.7382 16.9913 11.4652 17.1008 11.184 17.1555ZM20.3865 6.22954L21 5.61529V5.10004C21 3.94054 20.0595 3.00004 18.9 3.00004H18.3442L17.7892 3.55504L20.3865 6.22954Z";

    private static final String SVG_VIEW_QUESTIONS =
            "M5.01113 9.5747L6.29289 8.2929C6.68342 7.90236 7.31658 7.90236 7.70711 8.2929C8.09763 8.6834 8.09763 9.3166 7.70711 9.7071L4.70711 12.7071C4.51957 12.8946 4.26522 13 4 13C3.73478 13 3.48043 12.8946 3.29289 12.7071L0.292893 9.7071C-0.0976312 9.3166 -0.0976312 8.6834 0.292893 8.2929C0.683417 7.90236 1.31658 7.90236 1.70711 8.2929L3.00811 9.5939C3.22118 4.25933 7.61318 0 13 0C18.5229 0 23 4.47715 23 10C23 15.5228 18.5229 20 13 20C9.85818 20 7.0543 18.5499 5.22264 16.2864C4.87523 15.8571 4.94164 15.2274 5.37097 14.88C5.80029 14.5326 6.42997 14.599 6.77738 15.0283C8.24563 16.8427 10.4873 18 13 18C17.4183 18 21 14.4183 21 10C21 5.58172 17.4183 2 13 2C8.72442 2 5.23222 5.35412 5.01113 9.5747ZM13 3C13.5523 3 14 3.44772 14 4V9.5858L16.7071 12.2929C17.0976 12.6834 17.0976 13.3166 16.7071 13.7071C16.3166 14.0976 15.6834 14.0976 15.2929 13.7071L12.2929 10.7071C12.1054 10.5196 12 10.2652 12 10V4C12 3.44772 12.4477 3 13 3Z";

    public MenuTeacherView(ClientAPI client, StateManager stateManager, User user) {
        this.client = client;
        this.stateManager = stateManager;
        this.user = user;

        setStyle("-fx-background-color: " + COLOR_BG + ";");

        BorderPane mainLayout = new BorderPane();

        HeaderView header = new HeaderView(stateManager, user);
        mainLayout.setTop(header);
        mainLayout.setCenter(createButtons());

        StackPane root = new StackPane(mainLayout);
        header.attachToRoot(root);
        this.setCenter(root);
    }

    private VBox createButtons() {

        Button newQuestionBtn = new Button("New Question");
        Button viewQuestionsBtn = new Button("View Questions");

        styleMainButton(newQuestionBtn);
        newQuestionBtn.setGraphic(createMainIcon(SVG_NEW_QUESTION));
        newQuestionBtn.setGraphicTextGap(40);
        newQuestionBtn.setOnAction(e -> stateManager.showNewQuestionView(user));

        styleMainButton(viewQuestionsBtn);
        viewQuestionsBtn.setGraphic(createMainIcon(SVG_VIEW_QUESTIONS));
        viewQuestionsBtn.setGraphicTextGap(20);
        viewQuestionsBtn.setOnAction(e -> stateManager.showQuestionHistory(user));

        VBox box = new VBox(20, newQuestionBtn, viewQuestionsBtn);
        box.setAlignment(Pos.CENTER);

        return box;
    }

    private void styleMainButton(Button btn) {
        btn.setStyle(String.format("""
            -fx-font-size: 22px;
            -fx-font-weight: bold;
            -fx-background-color: %s;
            -fx-text-fill: %s;
            -fx-background-radius: 12;
            -fx-padding: 10 40;
        """, COLOR_PRIMARY, COLOR_BUTTON_TEXT));
        btn.setPrefWidth(360);
    }

    private SVGPath createMainIcon(String svgContent) {
        SVGPath svg = new SVGPath();
        svg.setContent(svgContent);
        svg.setFill(Color.BLACK);
        svg.setScaleX(1.0);
        svg.setScaleY(1.0);
        return svg;
    }
}
