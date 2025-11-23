package pt.isec.pd.client.gui.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import pt.isec.pd.client.ClientAPI;
import pt.isec.pd.client.StateManager;

public class RegisterView extends BorderPane {

    private final ClientAPI clientService;
    private final StateManager stateManager;

    private final ComboBox<RoleItem> roleCombo = new ComboBox<>();
    private final TextField nameField = new TextField();
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmPasswordField = new PasswordField();
    private final TextField roleExtraField = new TextField();

    private final Button registerButton = new Button("Register");
    private final Button cancelButton = new Button("Cancel");

    private static final String COLOR_PRIMARY = "#FF7A00";
    private static final String COLOR_BG = "#1a1a1a";
    private static final String COLOR_ITEM = "#D9D9D9";
    private static final String COLOR_HOVER = "#F4B27C";

    private static class RoleItem {
        final String label;
        final String svg;
        RoleItem(String label, String svg) { this.label = label; this.svg = svg; }
        @Override public String toString() { return label; }
    }

    private static final String SVG_STUDENT =
            "M14.2491 13.6441C14.9191 13.2041 15.7991 13.6841 "
                    + "15.7991 14.4841V15.7741C15.7991 17.0441 14.8091 18.4041 "
                    + "13.6191 18.8041L10.4291 19.8641C9.86907 20.0541 8.95907 20.0541 "
                    + "8.40907 19.8641L5.21906 18.8041C4.01906 18.4041 3.03906 17.0441 "
                    + "3.03906 15.7741V14.4741C3.03906 13.6841 3.91906 13.2041 4.57906 13.6341L6.63906 14.9741 "
                    + "C7.42907 15.5041 8.42907 15.7641 9.42907 15.7641C10.4291 15.7641 11.4291 15.5041 12.2191 14.9741 "
                    + "L14.2491 13.6441Z M17.3975 4.4625L11.4075 0.5325C10.3275 -0.1775 8.54747 -0.1775 7.46747 0.5325L1.4475 4.4625 "
                    + "C-0.4825 5.7125 -0.4825 8.54251 1.4475 9.80251L3.0475 10.8425L7.46747 13.7225C8.54747 14.4325 10.3275 14.4325 "
                    + "11.4075 13.7225L15.7975 10.8425L17.1675 9.94251V13.0025C17.1675 13.4125 17.5075 13.7525 17.9175 13.7525 "
                    + "C18.3275 13.7525 18.6675 13.4125 18.6675 13.0025V8.08251C19.0675 6.7925 18.6575 5.2925 17.3975 4.4625Z";

    private static final String SVG_TEACHER =
            "M23.3792 3.50701H20.4692L17.1526 0.19038C16.8987 -0.0635414 16.4871 -0.0635414 16.2332 0.19038L12.9166 3.50692H10.0065C9.67322 3.50692 9.40308 3.77711 9.40308 4.11039V8.63893L10.0744 8.22211C10.555 7.92376 11.137 7.8856 11.6404 8.09105L13.8894 6.31768C14.2278 6.05087 14.7183 6.10899 14.985 6.44724C15.2518 6.78554 15.1937 7.27608 14.8554 7.5428L12.663 9.2715C12.8403 9.98058 12.552 10.7526 11.8978 11.1588L10.61 11.9583L9.40312 12.7078V14.021C9.40312 14.3543 9.67331 14.6244 10.0066 14.6244H23.3792C23.7125 14.6244 23.9827 14.3542 23.9827 14.021V4.11048C23.9827 3.7772 23.7125 3.50701 23.3792 3.50701ZM14.7553 3.50701L16.6929 1.56944L18.6305 3.50701H14.7553Z "
                    + "M11.7203 9.23469C11.4685 8.82917 10.9357 8.70458 10.5303 8.95634L8.78011 10.043C8.77284 8.59156 8.77669 9.36205 8.772 8.42708C8.76637 7.30513 7.84894 6.39233 6.72694 6.39233H5.87672C5.66054 6.953 5.11543 8.36675 4.95221 8.79008L6.68194 9.58409V8.43392C6.68194 8.33403 6.76289 8.25303 6.86283 8.25303C6.96277 8.25303 7.04372 8.33399 7.04372 8.43392L7.0628 11.6637C7.11666 12.311 7.83577 12.6637 8.37956 12.326L11.442 10.4246C11.8475 10.1729 11.9721 9.6402 11.7203 9.23469Z "
                    + "M2.10455 13.5109L2.10498 22.963C2.10498 23.5357 2.56922 24 3.14199 24C3.71475 24 4.179 23.5357 4.179 22.963V14.674H4.57289C4.48983 14.6467 2.10455 13.5109 2.10455 13.5109Z "
                    + "M6.68194 12.4288L5.92158 14.085C5.69827 14.5714 5.14815 14.8329 4.62671 14.6903V22.9629C4.62671 23.5357 5.09096 23.9999 5.66372 23.9999C6.23649 23.9999 6.70074 23.5357 6.70074 22.9629C6.6998 22.4403 6.68194 13.005 6.68194 12.4288Z "
                    + "M4.99039 7.79848L4.63901 7.15948L4.95073 6.59249C4.99718 6.50797 4.93596 6.40424 4.8394 6.40424H3.98295C3.88653 6.40424 3.82512 6.50788 3.87162 6.59249L4.18404 7.16061L3.83206 7.80078C3.79119 7.87517 3.77787 7.96147 3.79437 8.04467L3.91756 8.66511C4.28637 8.53109 4.56317 8.61162 4.87671 8.75553L5.02718 8.04692C5.04509 7.96241 5.03201 7.87419 4.99039 7.79848Z "
                    + "M6.47404 10.2327L4.47216 9.3137C4.25663 9.21479 4.002 9.30943 3.90319 9.52463L3.38531 10.6528L4.43972 11.2386C5.19295 11.6571 5.46431 12.6069 5.04581 13.3602C4.91597 13.5939 4.73452 13.7807 4.52395 13.9155L4.73831 14.0138C4.95384 14.1128 5.20847 14.0181 5.30728 13.8029L6.68498 10.8016C6.78384 10.5863 6.68939 10.3316 6.47404 10.2327Z "
                    + "M4.40287 5.82149C5.39201 5.82149 6.19387 5.01964 6.19387 4.0305C6.19387 3.04136 5.39201 2.2395 4.40287 2.2395C3.41373 2.2395 2.61188 3.04136 2.61188 4.0305C2.61188 5.01964 3.41373 5.82149 4.40287 5.82149Z "
                    + "M4.10164 11.847L1.74876 10.5397C1.75358 9.75295 1.75719 9.174 1.76169 8.43581C1.76221 8.34122 1.83913 8.26495 1.93363 8.26519C2.02813 8.26542 2.10458 8.34216 2.10458 8.4367V9.9412L2.79257 10.3235L3.28878 9.24258C3.40124 8.99756 3.59792 8.80692 3.83492 8.69855C3.73794 8.44697 3.04321 6.64524 2.94571 6.3924H2.07843C0.956382 6.3924 0.0390398 7.30524 0.0334148 8.42616C0.0196336 8.80458 0.0196336 10.6636 0.0172898 11.0411C0.0153211 11.3569 0.185758 11.6485 0.461758 11.8019L3.26225 13.3578C3.67939 13.5896 4.20556 13.4393 4.43736 13.0221C4.66911 12.605 4.51883 12.0788 4.10164 11.847Z";

    public RegisterView(ClientAPI clientService, StateManager stateManager) {
        this.clientService = clientService;
        this.stateManager = stateManager;

        setStyle("-fx-background-color: " + COLOR_BG + ";");

        Label title = new Label("Questia");
        title.setStyle("""
            -fx-font-size: 80px;
            -fx-text-fill: #FF7A00;
            -fx-font-weight: bold;
        """);
        BorderPane topBar = new BorderPane();

        BorderPane.setAlignment(title, Pos.TOP_LEFT);
        topBar.setLeft(title);
        topBar.setPadding(new Insets(20, 20, 0, 20));

        setTop(topBar);

        setCenter(createContent());

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
        cancelButton.setPrefWidth(100);
        cancelButton.setOnAction(e -> stateManager.showLogin());

        HBox cancelArea = new HBox(cancelButton);
        cancelArea.setAlignment(Pos.CENTER_RIGHT);
        cancelArea.setPadding(new Insets(0, 40, 50, 0));
        setBottom(cancelArea);
    }

    private VBox createContent() {

        setupRoleCombo();

        styleField(nameField, "Name");
        styleField(emailField, "Email");
        styleField(passwordField, "Password");
        styleField(confirmPasswordField, "Confirm Password");

        styleField(roleExtraField, "...");
        roleExtraField.setDisable(true);

        VBox fields = new VBox(18,
                roleCombo,
                nameField,
                roleExtraField,
                emailField,
                passwordField,
                confirmPasswordField
        );
        fields.setAlignment(Pos.CENTER);

        registerButton.setStyle("""
            -fx-background-color:#FF7A00;
            -fx-text-fill:#1E1E1E;
            -fx-font-size:16px;
            -fx-font-weight:bold;
            -fx-background-radius:10px;
            -fx-padding:10 30;
        """);
        registerButton.setPrefWidth(150);
        registerButton.setDisable(true);

        VBox root = new VBox(40, fields, new HBox(registerButton));
        ((HBox) root.getChildren().get(1)).setAlignment(Pos.CENTER);
        root.setAlignment(Pos.CENTER);

        disableAllFields();
        return root;
    }

    private void styleField(TextField field, String placeholder) {
        field.setPromptText(placeholder);
        field.setPrefWidth(260);
        field.setMaxWidth(260);
        field.setStyle("""
            -fx-background-color:#D9D9D9;
            -fx-text-fill:#1E1E1E;
            -fx-prompt-text-fill:#1E1E1E;
            -fx-padding:10;
            -fx-background-radius:10;
            -fx-border-radius:10;
            -fx-font-size:15px;
        """);
    }

    private void setFieldWhite(TextField field) {
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

    private void disableAllFields() {
        nameField.setDisable(true);
        emailField.setDisable(true);
        passwordField.setDisable(true);
        confirmPasswordField.setDisable(true);
        roleExtraField.setDisable(true);
        registerButton.setDisable(true);
    }

    private void enableAllBaseFields() {
        nameField.setDisable(false);
        emailField.setDisable(false);
        passwordField.setDisable(false);
        confirmPasswordField.setDisable(false);
        registerButton.setDisable(false);
    }

    private void updateFieldsByRole() {
        RoleItem selected = roleCombo.getValue();

        if (selected == null) {
            disableAllFields();
            return;
        }

        enableAllBaseFields();

        setFieldWhite(nameField);
        setFieldWhite(emailField);
        setFieldWhite(passwordField);
        setFieldWhite(confirmPasswordField);
        setFieldWhite(roleExtraField);

        if (selected.label.equals("Student"))
            roleExtraField.setPromptText("Id Number");
        else
            roleExtraField.setPromptText("Teacher Code");

        roleExtraField.setDisable(false);
        roleExtraField.setVisible(true);
        roleExtraField.setManaged(true);
    }

    private void setupRoleCombo() {

        roleCombo.getItems().setAll(
                new RoleItem("Student", SVG_STUDENT),
                new RoleItem("Teacher", SVG_TEACHER)
        );
        roleCombo.setPromptText("Role");

        roleCombo.setPrefWidth(260);
        roleCombo.setMinWidth(260);
        roleCombo.setMaxWidth(260);
        roleCombo.setPrefHeight(40);
        roleCombo.setMinHeight(40);
        roleCombo.setMaxHeight(40);

        roleCombo.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 10;
            -fx-border-radius: 10;
            -fx-border-color: transparent;
            -fx-padding: 0 10;
            -fx-font-size: 14px;
            -fx-focus-color: transparent;
            -fx-faint-focus-color: transparent;
        """);

        roleCombo.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin instanceof ComboBoxListViewSkin<?> cbSkin) {

                Node arrowBtn = roleCombo.lookup(".arrow-button");
                if (arrowBtn != null) {
                    arrowBtn.setStyle("""
                        -fx-background-color: white;
                        -fx-background-radius: 10;
                        -fx-padding: 0 12;
                    """);
                }

                Node arrow = roleCombo.lookup(".arrow");
                if (arrow != null)
                    arrow.setStyle("-fx-background-color: black;");

                Node popupContent = cbSkin.getPopupContent();
                if (popupContent instanceof Region region) {
                    region.setStyle("""
                        -fx-background-color: transparent;
                        -fx-padding: 0;
                    """);
                }

                Platform.runLater(() -> {
                    Node listView = cbSkin.getPopupContent().lookup(".list-view");
                    if (listView instanceof Region r) {
                        r.setStyle("""
                            -fx-background-color: transparent;
                            -fx-padding: 0;
                            -fx-pref-width: 200;
                        """);
                    }
                });
            }
        });

        roleCombo.setOnShown(e ->
                Platform.runLater(() -> {
                    Skin<?> skin = roleCombo.getSkin();
                    if (skin instanceof ComboBoxListViewSkin<?> cbSkin) {
                        Region popup = (Region) cbSkin.getPopupContent().getParent();
                        popup.setTranslateX(60);
                    }
                })
        );

        roleCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(RoleItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setBackground(Background.EMPTY);
                    return;
                }

                setGraphic(buildOption(item));

                setPrefHeight(38);
                setMinHeight(38);
                setMaxHeight(38);

                Color normal = Color.web(COLOR_ITEM);
                Color selected = Color.web("#D9D9D9");
                Color hover = Color.web(COLOR_HOVER);
                Color base = isSelected() ? selected : normal;

                setBackground(new Background(
                        new BackgroundFill(base, new CornerRadii(10), Insets.EMPTY)
                ));

                hoverProperty().addListener((o, oldV, h) -> {
                    Color bg = h ? hover : (isSelected() ? selected : normal);
                    setBackground(new Background(
                            new BackgroundFill(bg, new CornerRadii(10), Insets.EMPTY)
                    ));
                });
            }
        });

        roleCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(RoleItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText("Role");
                    setGraphic(null);
                } else {
                    setText(null);
                    setGraphic(buildOption(item));
                }

                setPrefHeight(40);
                setMinHeight(40);
                setMaxHeight(40);

                setPrefWidth(260);
                setMinWidth(260);
                setMaxWidth(260);

                setStyle("-fx-padding: 10; -fx-text-fill: #1E1E1E;");

                setBackground(new Background(
                        new BackgroundFill(Color.WHITE, new CornerRadii(10), Insets.EMPTY)
                ));
            }
        });

        roleCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateFieldsByRole());
    }

    private HBox buildOption(RoleItem item) {
        SVGPath icon = new SVGPath();
        icon.setFill(Color.web(COLOR_PRIMARY));
        icon.setScaleX(0.75);
        icon.setScaleY(0.75);
        icon.setContent(item.svg);

        Label text = new Label(item.label);
        text.setStyle("-fx-text-fill:black; -fx-font-size:15px;");

        HBox box = new HBox(8, icon, text);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(4, 8, 4, 8));

        return box;
    }
}
