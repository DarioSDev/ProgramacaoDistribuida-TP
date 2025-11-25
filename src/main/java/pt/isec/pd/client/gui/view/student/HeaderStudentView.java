package pt.isec.pd.client.gui.view.student;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import pt.isec.pd.client.StateManager;
import pt.isec.pd.common.User;

public class HeaderStudentView extends BorderPane {

    public static final int HEIGHT = 120;

    private static final String COLOR_PRIMARY = "#FF7A00";
    private static final String COLOR_BG = "#1A1A1A";

    private static final int DROPDOWN_WIDTH = 220;

    private final VBox dropdownMenu = new VBox();
    private boolean dropdownVisible = false;

    private final StateManager stateManager;
    private final User user;

    private static final String SVG_HOME =
            "M10 20V14H14V20H19V12H22L12 3 2 12H5V20H10Z";

    private static final String SVG_HISTORY =
            "M5.01113 11.5747L6.29289 10.2929C6.68342 9.90236 7.31658 9.90236 7.70711 10.2929C8.09763 10.6834 8.09763 11.3166 7.70711 11.7071L4.70711 14.7071C4.51957 14.8946 4.26522 15 4 15C3.73478 15 3.48043 14.8946 3.29289 14.7071L0.292893 11.7071C-0.0976312 11.3166 -0.0976312 10.6834 0.292893 10.2929C0.683417 9.90236 1.31658 9.90236 1.70711 10.2929L3.00811 11.5939C3.22118 6.25933 7.61318 2 13 2C18.5229 2 23 6.47715 23 12C23 17.5228 18.5229 22 13 22C9.85818 22 7.0543 20.5499 5.22264 18.2864C4.87523 17.8571 4.94164 17.2274 5.37097 16.88C5.80029 16.5326 6.42997 16.599 6.77738 17.0283C8.24563 18.8427 10.4873 20 13 20C17.4183 20 21 16.4183 21 12C21 7.58172 17.4183 4 13 4C8.72442 4 5.23222 7.35412 5.01113 11.5747ZM13 5C13.5523 5 14 5.44772 14 6V11.5858L16.7071 14.2929C17.0976 14.6834 17.0976 15.3166 16.7071 15.7071C16.3166 16.0976 15.6834 16.0976 15.2929 15.7071L12.2929 12.7071C12.1054 12.5196 12 12.2652 12 12V6C12 5.44772 12.4477 5 13 5Z";

    private static final String SVG_LOGOUT =
            "M14.7961 0H12.1961C8.99609 0 6.99609 2 6.99609 5.2V9.25H13.2461C13.6561 9.25 13.9961 9.59 "
                    + "13.9961 10C13.9961 10.41 13.6561 10.75 13.2461 10.75H6.99609V14.8C6.99609 18 8.99609 "
                    + "20 12.1961 20H14.7861C17.9861 20 19.9861 18 19.9861 14.8V5.2C19.9961 2 17.9961 0 "
                    + "14.7961 0Z M2.5575 9.2498L4.6275 7.17984C4.7775 7.02984 4.8475 6.83984 4.8475 6.64984C4.8475 "
                    + "6.45984 4.7775 6.25984 4.6275 6.11984C4.3375 5.82984 3.8575 5.82984 3.5675 6.11984L0.2175 "
                    + "9.4698C-0.0725 9.7598 -0.0725 10.2398 0.2175 10.5298L3.5675 13.8798C3.8575 14.1698 4.3375 "
                    + "14.1698 4.6275 13.8798C4.9175 13.5898 4.9175 13.1098 4.6275 12.8198L2.5575 10.7498H6.9975V9.2498H2.5575Z";


    public HeaderStudentView(StateManager stateManager, User user) {
        this.stateManager = stateManager;
        this.user = user;

        setPrefHeight(HEIGHT);
        setMinHeight(HEIGHT);
        setMaxHeight(HEIGHT);

        setStyle("-fx-background-color: " + COLOR_BG + ";");

        setupLayout();
        setupDropdown();
    }

    private void setupLayout() {
        Label title = new Label("Questia");
        title.setStyle("""
            -fx-font-size: 60px;
            -fx-font-weight: bold;
            -fx-text-fill: #FF7A00;
        """);

        BorderPane.setMargin(title, new Insets(25, 0, 0, 40));
        setLeft(title);

        StackPane menuBtn = createMenuButton();
        menuBtn.setOnMouseClicked(e -> toggleDropdown());

        HBox rightBox = new HBox(menuBtn);
        rightBox.setAlignment(Pos.TOP_RIGHT);
        rightBox.setPadding(new Insets(45, 40, 0, 0));
        setRight(rightBox);
    }

    private StackPane createMenuButton() {
        StackPane btn = new StackPane();
        btn.setCursor(Cursor.HAND);

        btn.setPrefSize(40, 40);
        btn.setMinSize(40, 40);
        btn.setMaxSize(40, 40);

        btn.setStyle("""
        -fx-background-color: #FF7A00;
        -fx-background-radius: 10;
    """);

        VBox lines = new VBox(3);
        lines.setAlignment(Pos.CENTER);

        Rectangle r1 = new Rectangle(14, 2.2, Color.BLACK);
        Rectangle r2 = new Rectangle(14, 2.2, Color.BLACK);
        Rectangle r3 = new Rectangle(14, 2.2, Color.BLACK);

        lines.getChildren().addAll(r1, r2, r3);
        btn.getChildren().add(lines);

        return btn;
    }

    private Group createProfileIcon() {
        SVGPath p1 = new SVGPath();
        p1.setContent(
                "M14.2491 13.6441C14.9191 13.2041 15.7991 13.6841 15.7991 14.4841V15.7741C15.7991 17.0441 "
                        + "14.8091 18.4041 13.6191 18.8041L10.4291 19.8641C9.86907 20.0541 8.95907 20.0541 "
                        + "8.40907 19.8641L5.21906 18.8041C4.01906 18.4041 3.03906 17.0441 3.03906 15.7741V14.4741C3.03906 "
                        + "13.6841 3.91906 13.2041 4.57906 13.6341L6.63906 14.9741C7.42907 15.5041 8.42907 15.7641 "
                        + "9.42907 15.7641C10.4291 15.7641 11.4291 15.5041 12.2191 14.9741L14.2491 13.6441Z"
        );
        p1.setFill(Color.web(COLOR_PRIMARY));

        SVGPath p2 = new SVGPath();
        p2.setContent(
                "M17.3975 4.4625L11.4075 0.5325C10.3275 -0.1775 8.54747 -0.1775 7.46747 0.5325L1.4475 4.4625C-0.4825 "
                        + "5.7125 -0.4825 8.54251 1.4475 9.80251L3.0475 10.8425L7.46747 13.7225C8.54747 14.4325 10.3275 "
                        + "14.4325 11.4075 13.7225L15.7975 10.8425L17.1675 9.94251V13.0025C17.1675 13.4125 17.5075 13.7525 "
                        + "17.9175 13.7525C18.3275 13.7525 18.6675 13.4125 18.6675 13.0025V8.08251C19.0675 6.7925 18.6575 "
                        + "5.2925 17.3975 4.4625Z"
        );
        p2.setFill(Color.web(COLOR_PRIMARY));

        Group g = new Group(p1, p2);
        g.setScaleX(0.9);
        g.setScaleY(0.9);
        g.setTranslateY(1);

        return g;
    }

    private void setupDropdown() {

        dropdownMenu.setVisible(false);
        dropdownMenu.setManaged(false);
        dropdownMenu.setSpacing(0);
        dropdownMenu.setPadding(Insets.EMPTY);
        dropdownMenu.setFillWidth(true);

        dropdownMenu.setPrefWidth(DROPDOWN_WIDTH);
        dropdownMenu.setMinWidth(DROPDOWN_WIDTH);
        dropdownMenu.setMaxWidth(DROPDOWN_WIDTH);

        dropdownMenu.setStyle("""
        -fx-background-color: transparent;
        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 12, 0.1, 0, 3);
    """);

        dropdownMenu.setTranslateX(-20);
        dropdownMenu.setTranslateY(90);
        StackPane.setAlignment(dropdownMenu, Pos.TOP_RIGHT);

        Button home = createDropdownButton("Home", SVG_HOME, true, false);
        home.setOnAction(e -> { toggleDropdown(); stateManager.showStudentMenu(user); });

        Button history = createDropdownButton("History", SVG_HISTORY, false, false);
        history.setOnAction(e -> { toggleDropdown(); stateManager.showStudentHistory(user); });

        Button profile = createDropdownButton("Profile", null, false, false);
        profile.setGraphic(createProfileIcon());
        profile.setOnAction(e -> {
            toggleDropdown();
            stateManager.showEditProfile(user);
        });


        Button logout = createDropdownButton("Logout", SVG_LOGOUT, false, true);
        logout.setOnAction(e -> { toggleDropdown(); stateManager.showLogin(); });

        dropdownMenu.getChildren().setAll(home, history, profile, logout);
    }

    private Button createDropdownButton(String text, String svgContent, boolean isTop, boolean isBottom) {

        Button btn = new Button(text);
        btn.setCursor(Cursor.HAND);
        btn.setGraphicTextGap(14);
        btn.setAlignment(Pos.CENTER_LEFT);

        if (svgContent != null)
            btn.setGraphic(createIcon(svgContent));

        btn.setPrefWidth(DROPDOWN_WIDTH);
        btn.setMinWidth(DROPDOWN_WIDTH);
        btn.setMaxWidth(DROPDOWN_WIDTH);

        String radius = "0 0 0 0";
        if (isTop) radius = "12 12 0 0";
        if (isBottom) radius = "0 0 12 12";

        String normalStyle = """
        -fx-background-color: #D9D9D9;
        -fx-font-size: 15px;
        -fx-text-fill: #1E1E1E;
        -fx-padding: 10 18;
        -fx-background-radius: %s;
    """.formatted(radius);

        String hoverStyle = """
        -fx-background-color: #F4B27C;
        -fx-font-size: 15px;
        -fx-text-fill: #1E1E1E;
        -fx-padding: 10 18;
        -fx-background-radius: %s;
    """.formatted(radius);

        btn.setStyle(normalStyle);

        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(normalStyle));

        return btn;
    }

    private SVGPath createIcon(String svgContent) {
        SVGPath svg = new SVGPath();
        svg.setContent(svgContent);
        svg.setFill(Color.web(COLOR_PRIMARY));
        return svg;
    }

    private void toggleDropdown() {
        dropdownVisible = !dropdownVisible;
        dropdownMenu.setVisible(dropdownVisible);
        dropdownMenu.setManaged(dropdownVisible);
    }

    public void attachToRoot(StackPane root) {
        root.getChildren().add(dropdownMenu);
        StackPane.setAlignment(dropdownMenu, Pos.TOP_RIGHT);

        root.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!dropdownVisible) return;

            Bounds b = dropdownMenu.localToScene(dropdownMenu.getBoundsInLocal());
            double x = e.getSceneX();
            double y = e.getSceneY();

            boolean inside =
                    x >= b.getMinX() && x <= b.getMaxX() &&
                            y >= b.getMinY() && y <= b.getMaxY();

            if (!inside) {
                toggleDropdown();
            }
        });
    }
}