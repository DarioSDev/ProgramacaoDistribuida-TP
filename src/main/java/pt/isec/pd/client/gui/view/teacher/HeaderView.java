package pt.isec.pd.client.gui.view.teacher;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import pt.isec.pd.client.StateManager;
import pt.isec.pd.common.User;

public class HeaderView extends BorderPane {

    public static final int HEIGHT = 120;

    private static final String COLOR_PRIMARY = "#FF7A00";
    private static final String COLOR_BG = "#1A1A1A";
    private static final String COLOR_DROPDOWN_BG = "#D9D9D9";
    private static final String COLOR_DROPDOWN_TEXT = "#1E1E1E";
    private static final String COLOR_HOVER = "#F4B27C";

    private static final int DROPDOWN_WIDTH = 220;

    private final VBox dropdownMenu = new VBox();
    private boolean dropdownVisible = false;

    private final StateManager stateManager;
    private final User user;

    private static final String SVG_HOME =
            "M10 20V14H14V20H19V12H22L12 3 2 12H5V20H10Z";

    private static final String SVG_HISTORY =
            "M5.01113 9.5747L6.29289 8.2929C6.68342 7.90236 7.31658 7.90236 7.70711 8.2929C8.09763 8.6834 8.09763 9.3166 7.70711 9.7071L4.70711 12.7071C4.51957 12.8946 4.26522 13 4 13C3.73478 13 3.48043 12.8946 3.29289 12.7071L0.292893 9.7071C-0.0976312 9.3166 -0.0976312 8.6834 0.292893 8.2929C0.683417 7.90236 1.31658 7.90236 1.70711 8.2929L3.00811 9.5939C3.22118 4.25933 7.61318 0 13 0C18.5229 0 23 4.47715 23 10C23 15.5228 18.5229 20 13 20C9.85818 20 7.0543 18.5499 5.22264 16.2864C4.87523 15.8571 4.94164 15.2274 5.37097 14.88C5.80029 14.5326 6.42997 14.599 6.77738 15.0283C8.24563 16.8427 10.4873 18 13 18C17.4183 18 21 14.4183 21 10C21 5.58172 17.4183 2 13 2C8.72442 2 5.23222 5.35412 5.01113 9.5747ZM13 3C13.5523 3 14 3.44772 14 4V9.5858L16.7071 12.2929C17.0976 12.6834 17.0976 13.3166 16.7071 13.7071C16.3166 14.0976 15.6834 14.0976 15.2929 13.7071L12.2929 10.7071C12.1054 10.5196 12 10.2652 12 10V4C12 3.44772 12.4477 3 13 3Z";

    private static final String SVG_NEW_QUESTION =
            "M23.547 3.06604L20.913 0.429786C20.3302 -0.152214 19.3875 -0.152214 18.8055 0.429786L16.2367 3.00004H18.3442L19.332 2.01229C19.623 1.72054 20.0948 1.72054 20.3865 2.01229L21.9668 3.59329C22.2578 3.88504 22.2578 4.35679 21.9668 4.64779L21 5.61529V7.72353L23.547 5.17429C24.129 4.59304 24.129 3.64804 23.547 3.06604ZM8.26799 13.083C8.54774 13.3628 9.91501 14.7308 10.902 15.7193L19.332 7.28404L16.6793 4.66654L8.26799 13.083ZM5.03476 18.4283C4.86376 18.7718 5.19375 19.1258 5.5605 18.9548L9.58951 16.515L7.47377 14.3963L5.03476 18.4283ZM11.184 17.1555L4.65976 20.3828C3.92476 20.724 3.33675 20.0655 3.6075 19.3298L6.83249 12.801C6.88799 12.5198 6.99601 12.2468 7.21426 12.0285L16.2367 3.00004H2.09999C0.940491 3.00004 0 3.94054 0 5.10004V21.9C0 23.0595 0.940491 24 2.09999 24H18.9C20.0595 24 21 23.0595 21 21.9V7.72353L11.9557 16.7738C11.7382 16.9913 11.4652 17.1008 11.184 17.1555ZM20.3865 6.22954L21 5.61529V5.10004C21 3.94054 20.0595 3.00004 18.9 3.00004H18.3442L17.7892 3.55504L20.3865 6.22954Z";

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


    public HeaderView(StateManager stateManager, User user) {
        this.stateManager = stateManager;
        this.user = user;

        setPrefHeight(HEIGHT);
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
        home.setOnAction(e -> { toggleDropdown(); stateManager.showTeacherMenu(user); });

        Button history = createDropdownButton("History", SVG_HISTORY, false, false);
        history.setOnAction(e -> { toggleDropdown(); stateManager.showQuestionHistory(user); });

        Button newQ = createDropdownButton("New Question", SVG_NEW_QUESTION, false, false);
        newQ.setOnAction(e -> { toggleDropdown(); stateManager.showNewQuestionView(user); });

        Button profile = createDropdownButton("Profile", SVG_PROFILE, false, false);
        profile.setOnAction(e -> { toggleDropdown(); stateManager.showEditProfile(user); });

        Button logout = createDropdownButton("Logout", SVG_LOGOUT, false, true);
        logout.setOnAction(e -> { toggleDropdown(); stateManager.showLogin(); });

        dropdownMenu.getChildren().setAll(home, history, newQ, profile, logout);
    }



    private Button createDropdownButton(String text, String svgContent, boolean isTop, boolean isBottom) {

        Button btn = new Button(text);
        btn.setCursor(Cursor.HAND);
        btn.setGraphic(createIcon(svgContent));
        btn.setGraphicTextGap(14);
        btn.setAlignment(Pos.CENTER_LEFT);

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
        svg.setScaleX(1.0);
        svg.setScaleY(1.0);
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
