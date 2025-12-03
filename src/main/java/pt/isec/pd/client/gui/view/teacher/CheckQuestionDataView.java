package pt.isec.pd.client.gui.view.teacher;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import pt.isec.pd.client.ClientAPI;
import pt.isec.pd.client.StateManager;
import pt.isec.pd.common.StudentAnswerInfo;
import pt.isec.pd.common.TeacherResultsData;
import pt.isec.pd.common.User;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CheckQuestionDataView extends BorderPane {

    private final ClientAPI client;
    private final StateManager stateManager;
    private final User user;

    private TeacherResultsData results;

    private static final String COLOR_PRIMARY = "#FF7A00";
    private static final String COLOR_BG = "#1A1A1A";

    private static final double W_ID = 90;
    private static final double W_NAME = 140;
    private static final double W_EMAIL = 200;
    private static final double W_ANSWER = 70;
    private static final double W_SCROLL = 35;
    private static final double W_TOTAL = W_ID + W_NAME + W_EMAIL + W_ANSWER + W_SCROLL;

    private static final String SVG_ARROW_UP = "M8.48877 0C8.93408 0 9.34229 0.182454 9.71338 0.547363L16.5786 7.57031C16.7147 7.71257 16.8198 7.86719 16.894 8.03418C16.9621 8.20736 16.9961 8.3929 16.9961 8.59082C16.9961 8.86914 16.9312 9.11963 16.8013 9.34229C16.6652 9.57113 16.4858 9.75049 16.2632 9.88037C16.0405 10.0164 15.7931 10.0845 15.521 10.0845C15.1066 10.0845 14.7417 9.92676 14.4263 9.61133L8.1084 3.10791H8.85986L2.56055 9.61133C2.24512 9.92676 1.8833 10.0845 1.4751 10.0845C1.20296 10.0845 0.955566 10.0164 0.73291 9.88037C0.510254 9.75049 0.333984 9.57113 0.204102 9.34229C0.0680339 9.11963 0 8.86914 0 8.59082C0 8.1888 0.13916 7.84863 0.41748 7.57031L7.27344 0.547363C7.45898 0.361816 7.65072 0.225749 7.84863 0.13916C8.04655 0.0525716 8.25993 0.0061849 8.48877 0Z";
    private static final String SVG_ARROW_DOWN = "M8.48877 10.0845C8.04346 10.0845 7.63525 9.90204 7.26416 9.53713L0.408936 2.51418C0.272869 2.37192 0.167727 2.2173 0.0935078 2.05031C0.0254737 1.87713 -0.00854492 1.69159 -0.00854492 1.49367C-0.00854492 1.21535 0.0563819 0.964861 0.186264 0.742205C0.322332 0.513368 0.501694 0.334008 0.72435 0.204126C0.947006 0.0680589 1.19439 0 1.46654 0C1.88092 0 2.24582 0.157746 2.56125 0.473176L8.87889 6.97659H8.12742L15.4365 0.473176C15.7519 0.157746 16.1137 0 16.5219 0C16.7941 0 17.0415 0.0680589 17.2641 0.204126C17.4868 0.334008 17.6661 0.513368 17.796 0.742205C17.9321 0.964861 18 1.21535 18 1.49367C18 1.89569 17.8608 2.23586 17.5825 2.51418L10.7266 9.53713C10.541 9.72268 10.3493 9.85875 10.1513 9.94534C9.95341 10.0319 9.74003 10.0783 9.51121 10.0845Z";
    private static final String SVG_BACK = "M0 8.50732C0 8.06201 0.182454 7.65381 0.547363 7.28271L7.57031 0.41748C7.71257 0.281413 7.86719 0.17627 8.03418 0.102051C8.20736 0.0340169 8.3929 0 8.59082 0C8.86914 0 9.11963 0.0649414 9.34229 0.194824C9.57113 0.330892 9.75049 0.510254 9.88037 0.73291C10.0164 0.955566 10.0845 1.20296 10.0845 1.4751C10.0845 1.88949 9.92676 2.25439 9.61133 2.56982L3.10791 8.8877V8.13623L9.61133 14.4355C9.92676 14.751 10.0845 15.1128 10.0845 15.521C10.0845 15.7931 10.0164 16.0405 9.88037 16.2632C9.75049 16.4858 9.57113 16.6621 9.34229 16.792C9.11963 16.9281 8.86914 16.9961 8.59082 16.9961C8.1888 16.9961 7.84863 16.8569 7.57031 16.5786L0.547363 9.72266C0.361816 9.53711 0.225749 9.34538 0.13916 9.14746C0.0525716 8.94954 0.0061849 8.73617 0 8.50732Z";

    private static final String SVG_EXPORT =
            "M17.8622 6.1375L12.7717 1.04688C12.7461 1.02133 12.7193 0.997285 12.6914 0.974363C12.6792 0.964379 12.6661 0.95552 12.6535 0.946051C12.6376 0.934145 12.622 0.921816 12.6057 0.910848C12.5904 0.900676 12.5743 0.891723 12.5586 0.882254C12.5438 0.873395 12.5293 0.864066 12.5142 0.856004C12.4981 0.847426 12.4815 0.840066 12.4649 0.832238C12.4491 0.824832 12.4335 0.81691 12.4175 0.810254C12.4015 0.803645 12.385 0.798254 12.3686 0.792348C12.3513 0.786113 12.3342 0.779457 12.3166 0.77416C12.3004 0.769191 12.2837 0.765582 12.2671 0.76141C12.2491 0.75691 12.2313 0.751895 12.2132 0.748238C12.194 0.744395 12.1743 0.742051 12.1548 0.739285C12.139 0.737035 12.1234 0.734035 12.1077 0.732441C12.0722 0.72902 12.0363 0.727051 12 0.727051C11.9636 0.727051 11.9277 0.72902 11.8922 0.732488C11.8769 0.734035 11.8616 0.736941 11.8462 0.739098C11.8263 0.741957 11.8062 0.744348 11.7866 0.748191C11.7691 0.75166 11.7517 0.756629 11.7343 0.760988C11.7172 0.765301 11.6998 0.769051 11.6831 0.77416C11.6661 0.779316 11.6495 0.785785 11.6328 0.791738C11.616 0.797785 11.5988 0.803363 11.5824 0.810207C11.567 0.816629 11.5519 0.824316 11.5366 0.831441C11.5195 0.839504 11.5022 0.847145 11.4856 0.856004C11.4713 0.863645 11.4574 0.872645 11.4434 0.881035C11.4268 0.890832 11.4101 0.900301 11.3941 0.910941C11.3789 0.921207 11.3643 0.932738 11.3494 0.943895C11.3357 0.954066 11.3217 0.963676 11.3084 0.974457C11.2823 0.995926 11.2569 1.01889 11.2324 1.04318C11.2311 1.04435 11.2299 1.04538 11.2286 1.0465L6.13758 6.13746C5.71163 6.5635 5.71154 7.25421 6.13758 7.68021C6.56354 8.10625 7.25433 8.10616 7.68033 7.6803L10.9091 4.45174V17.818C10.9091 18.4205 11.3975 18.9089 12 18.9089C12.6025 18.9089 13.0909 18.4205 13.0909 17.818V4.45179L16.3194 7.6803C16.5324 7.8933 16.8117 7.99985 17.0908 7.99985C17.37 7.99985 17.6492 7.8933 17.8622 7.6803C18.2882 7.25425 18.2882 6.5635 17.8622 6.1375Z " +
                    "M22.9091 11.6362C22.3066 11.6362 21.8182 12.1247 21.8182 12.7272V21.0908H2.1818V12.7272C2.1818 12.1247 1.69336 11.6362 1.09087 11.6362C0.488391 11.6362 0 12.1247 0 12.7272V22.1817C0 22.7842 0.488438 23.2726 1.09092 23.2726H22.9091C23.5116 23.2726 24 22.7842 24 22.1817V12.7272C24 12.1247 23.5116 11.6362 22.9091 11.6362Z";

    private final VBox rowsBox = new VBox(0);
    private final Button backButton = new Button("Back");
    private final Button exportButton = new Button("Export");

    public CheckQuestionDataView(ClientAPI client, StateManager stateManager, User user, TeacherResultsData results) {
        this.client = client;
        this.stateManager = stateManager;
        this.user = user;
        this.results = results;

        setStyle("-fx-background-color: " + COLOR_BG + ";");

        BorderPane layout = new BorderPane();

        HeaderView header = new HeaderView(stateManager, user);
        layout.setTop(header);

        BorderPane centerContent = createCenterContent();
        StackPane centerWrapper = new StackPane(centerContent);
        centerWrapper.setAlignment(Pos.TOP_CENTER);
        StackPane.setMargin(centerContent, new Insets(-20, 0, 0, 0));
        layout.setCenter(centerWrapper);

        StackPane root = new StackPane(layout);
        header.attachToRoot(root);
        this.setCenter(root);
    }

    private BorderPane createCenterContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(0, 40, 40, 10));

        VBox topBox = new VBox(0);
        root.setTop(topBox);

        BorderPane centerPane = new BorderPane();
        centerPane.setPadding(new Insets(20, 0, 0, 0));

        HBox questionHeader = new HBox();
        questionHeader.setAlignment(Pos.CENTER_LEFT);

        Label qTitle = new Label("Question");
        qTitle.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        Region spacerQ = new Region();
        HBox.setHgrow(spacerQ, Priority.ALWAYS);

        Label dateLabelTop = new Label(results.getCreationDate());
        dateLabelTop.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        questionHeader.getChildren().addAll(qTitle, spacerQ, dateLabelTop);

        Label qText = new Label(results.getQuestionText());
        qText.setWrapText(true);
        qText.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        VBox questionBox = new VBox(6, questionHeader, qText);
        centerPane.setTop(questionBox);

        HBox middleRow = new HBox(60);
        middleRow.setAlignment(Pos.TOP_LEFT);
        middleRow.setPadding(new Insets(20, 0, 0, 0));

        VBox leftPanel = createLeftPanel();
        Region tableRegion = createAnswersTable();

        middleRow.getChildren().addAll(leftPanel, tableRegion);
        centerPane.setCenter(middleRow);

        VBox responsiveCenter = new VBox(50);
        responsiveCenter.setAlignment(Pos.TOP_CENTER);

        responsiveCenter.setFillWidth(false);
        responsiveCenter.getChildren().add(centerPane);

        root.setCenter(responsiveCenter);

        styleButton(backButton, SVG_BACK);
        backButton.setOnAction(e -> stateManager.showQuestionHistory(user));

        styleButton(exportButton, SVG_EXPORT);
        exportButton.setGraphicTextGap(45);
        exportButton.setOnAction(e -> showExportDialog());

        VBox buttonsColumn = new VBox(15);
        buttonsColumn.setAlignment(Pos.CENTER);

        buttonsColumn.getChildren().add(exportButton);
        buttonsColumn.getChildren().add(backButton);

        buttonsColumn.setPadding(new Insets(40, 0, 0, 0));

        root.setBottom(buttonsColumn);

        return root;
    }

    private VBox createLeftPanel() {
        VBox box = new VBox(30);
        box.setAlignment(Pos.TOP_LEFT);

        VBox correctBox = new VBox(10);
        Label lblCorrect = new Label("Correct Option");
        lblCorrect.setStyle("-fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        String correctLetter = results.getCorrectOptionLetter().toLowerCase();
        List<String> options = results.getOptions();

        correctBox.getChildren().add(lblCorrect);

        for (int i = 0; i < options.size() && i < 4; i++) {
            char c = (char) ('a' + i);
            String optionText = options.get(i);

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            Circle circle = new Circle(6);
            if (String.valueOf(c).equals(correctLetter)) {
                circle.setFill(Color.web(COLOR_PRIMARY));
            } else {
                circle.setFill(Color.web("#808080"));
            }

            Label lbl = new Label(c + ") " + optionText);
            lbl.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
            lbl.setWrapText(true);
            lbl.setMaxWidth(180);
            lbl.setPadding(new Insets(0));

            row.getChildren().addAll(circle, lbl);
            correctBox.getChildren().add(row);
        }

        VBox totalBox = new VBox(4);
        Label lblTotalTitle = new Label("Total Answers");
        lblTotalTitle.setStyle("-fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label lblTotal = new Label(Integer.toString(results.getTotalAnswers()));
        lblTotal.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        totalBox.getChildren().addAll(lblTotalTitle, lblTotal);

        box.getChildren().addAll(correctBox, totalBox);
        return box;
    }

    private Region createAnswersTable() {

        final double TABLE_HEIGHT = 230;
        final double ROW_HEIGHT = 28;

        VBox wrapper = new VBox();
        wrapper.setPadding(new Insets(0));
        wrapper.setSpacing(0);

        wrapper.setStyle("""
        -fx-border-color: #FF7A00;
        -fx-border-width: 1.5;
        -fx-border-radius: 6;
    """);

        HBox header = new HBox(0);
        header.setAlignment(Pos.CENTER_LEFT);

        Label idH = createHeaderLabel("ID", W_ID, Pos.CENTER_LEFT);
        Label nameH = createHeaderLabel("Name", W_NAME, Pos.CENTER_LEFT);
        Label emailH = createHeaderLabel("Email", W_EMAIL, Pos.CENTER_LEFT);
        Label ansH = createHeaderLabel("Answer", W_ANSWER, Pos.CENTER);
        Label scrollH = createHeaderLabel("", W_SCROLL, Pos.CENTER);

        header.getChildren().addAll(
                addRightBorder(idH, W_ID),
                addRightBorder(nameH, W_NAME),
                addRightBorder(emailH, W_EMAIL),
                addRightBorder(ansH, W_ANSWER),
                scrollH
        );

        VBox titleHeader = new VBox(header);
        titleHeader.setStyle("-fx-background-color: transparent; -fx-border-color: #FF7A00; -fx-border-width: 0 0 1.5 0;");
        titleHeader.setSpacing(0);
        titleHeader.setPadding(Insets.EMPTY);

        rowsBox.getChildren().clear();
        rowsBox.setSpacing(0);
        rowsBox.setPadding(Insets.EMPTY);
        rowsBox.setStyle("-fx-padding: 0;");

        renderRows(results.getAnswers());

        ScrollPane scroll = new ScrollPane(rowsBox);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setFitToWidth(true);
        scroll.setPannable(false);

        scroll.setPrefViewportHeight(TABLE_HEIGHT);
        scroll.setMinHeight(TABLE_HEIGHT);
        scroll.setMaxHeight(TABLE_HEIGHT);

        scroll.setStyle("""
        -fx-background: transparent;
        -fx-background-color: transparent;
        -fx-border-color: transparent;
    """);

        HBox gridOverlay = new HBox(0);
        gridOverlay.setAlignment(Pos.CENTER_LEFT);
        gridOverlay.setMouseTransparent(true);

        gridOverlay.getChildren().addAll(
                createGridCol(W_ID, true),
                createGridCol(W_NAME, true),
                createGridCol(W_EMAIL, true),
                createGridCol(W_ANSWER, false)
        );

        VBox arrowsColumn = new VBox();
        arrowsColumn.setPrefWidth(W_SCROLL);
        arrowsColumn.setMinWidth(W_SCROLL);
        arrowsColumn.setMaxWidth(W_SCROLL);

        arrowsColumn.setStyle("-fx-border-color: #FF7A00; -fx-border-width: 0 0 0 1.5;");
        arrowsColumn.setAlignment(Pos.TOP_CENTER);

        SVGPath arrowUpIcon = createArrowIcon(SVG_ARROW_UP);
        SVGPath arrowDownIcon = createArrowIcon(SVG_ARROW_DOWN);

        StackPane btnUp = new StackPane(arrowUpIcon);
        btnUp.setCursor(Cursor.HAND);
        btnUp.setPadding(new Insets(8, 0, 0, 0));
        btnUp.setPrefHeight(40);
        btnUp.setOnMouseClicked(e -> {
            double scrollable = rowsBox.getHeight() - TABLE_HEIGHT;
            if (scrollable > 0) {
                double increment = ROW_HEIGHT / scrollable;
                scroll.setVvalue(Math.max(0, scroll.getVvalue() - increment));
            }
        });

        StackPane btnDown = new StackPane(arrowDownIcon);
        btnDown.setCursor(Cursor.HAND);
        btnDown.setPadding(new Insets(0, 0, 8, 0));
        btnDown.setPrefHeight(40);
        btnDown.setOnMouseClicked(e -> {
            double scrollable = rowsBox.getHeight() - TABLE_HEIGHT;
            if (scrollable > 0) {
                double increment = ROW_HEIGHT / scrollable;
                scroll.setVvalue(Math.min(1.0, scroll.getVvalue() + increment));
            }
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        arrowsColumn.getChildren().addAll(btnUp, spacer, btnDown);
        arrowsColumn.setMinHeight(TABLE_HEIGHT);

        StackPane dataLayer = new StackPane(scroll, gridOverlay);
        dataLayer.setAlignment(Pos.TOP_LEFT);
        dataLayer.setMinHeight(TABLE_HEIGHT);
        dataLayer.setMaxHeight(TABLE_HEIGHT);

        HBox contentRow = new HBox(0);
        contentRow.setAlignment(Pos.TOP_LEFT);
        contentRow.getChildren().addAll(dataLayer, arrowsColumn);

        contentRow.setMinHeight(TABLE_HEIGHT);
        contentRow.setMaxHeight(TABLE_HEIGHT);

        wrapper.getChildren().addAll(titleHeader, contentRow);

        double totalHeight = 28 + TABLE_HEIGHT;
        wrapper.setMinHeight(totalHeight);
        wrapper.setPrefHeight(totalHeight);
        wrapper.setMaxHeight(totalHeight);

        wrapper.setMaxWidth(W_TOTAL);

        return wrapper;
    }


    private void renderRows(List<StudentAnswerInfo> answers) {
        rowsBox.getChildren().clear();

        if (answers == null || answers.isEmpty()) {
            Label emptyMsg = new Label("No answers yet.");
            emptyMsg.setStyle("-fx-text-fill: #D9D9D9; -fx-font-size: 16px; -fx-font-weight: bold;");
            emptyMsg.setAlignment(Pos.CENTER);
            rowsBox.getChildren().add(emptyMsg);
            return;
        }

        int idx = 1;
        for (StudentAnswerInfo info : answers) {
            HBox row = new HBox(0);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMinHeight(28);

            String idText = String.format("%09d", idx++);
            Label id = new Label(idText);
            id.setStyle("-fx-text-fill:white; -fx-font-size:12px;");
            row.getChildren().add(createCell(id, W_ID, Pos.CENTER_LEFT));

            Label name = new Label(info.getStudentName());
            name.setStyle("-fx-text-fill:white; -fx-font-size:12px;");
            row.getChildren().add(createCell(name, W_NAME, Pos.CENTER_LEFT));

            Label email = new Label(info.getStudentEmail());
            email.setStyle("-fx-text-fill:white; -fx-font-size:12px;");
            row.getChildren().add(createCell(email, W_EMAIL, Pos.CENTER_LEFT));

            Label ans = new Label(info.getAnswerLetter());
            ans.setStyle("-fx-text-fill:white; -fx-font-size:12px;");
            row.getChildren().add(createCell(ans, W_ANSWER, Pos.CENTER));

            rowsBox.getChildren().add(row);
        }
    }

    private Label createHeaderLabel(String text, double width, Pos alignment) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(width);
        lbl.setMaxWidth(width);
        lbl.setAlignment(alignment);
        if (alignment == Pos.CENTER_LEFT)
            lbl.setPadding(new Insets(0, 0, 0, 10));
        lbl.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
        return lbl;
    }

    private StackPane addRightBorder(Node node, double width) {
        StackPane p = new StackPane(node);
        p.setPrefWidth(width);
        p.setMinWidth(width);
        p.setMaxWidth(width);
        p.setStyle("-fx-border-color: #FF7A00; -fx-border-width: 0 1.5 0 0;");
        return p;
    }

    private StackPane createCell(Node node, double width, Pos alignment) {
        StackPane p = new StackPane(node);
        p.setPrefWidth(width);
        p.setMinWidth(width);
        p.setMaxWidth(width);
        p.setAlignment(alignment);

        if (alignment == Pos.CENTER_LEFT) {
            p.setPadding(new Insets(0, 0, 0, 10));
        } else {
            p.setPadding(Insets.EMPTY);
        }
        return p;
    }

    private Region createGridCol(double width, boolean borderRight) {
        Region r = new Region();
        r.setPrefWidth(width);
        r.setMinWidth(width);
        if (borderRight) {
            r.setStyle("-fx-border-color: #FF7A00; -fx-border-width: 0 1.5 0 0;");
        }
        return r;
    }

    private SVGPath createArrowIcon(String svgContent) {
        SVGPath svg = new SVGPath();
        svg.setContent(svgContent);
        svg.setFill(Color.web(COLOR_PRIMARY));
        svg.setScaleX(1.2);
        svg.setScaleY(1.2);
        return svg;
    }

    private void styleButton(Button btn, String svgContent) {
        btn.setCursor(Cursor.HAND);
        btn.setPrefWidth(260);
        btn.setPrefHeight(40);

        SVGPath icon = new SVGPath();
        icon.setContent(svgContent);
        icon.setFill(Color.BLACK);
        icon.setScaleX(1.0);
        icon.setScaleY(1.0);
        btn.setGraphic(icon);

        btn.setGraphicTextGap(60);

        btn.setStyle("""
        -fx-background-color: #FF7A00;
        -fx-text-fill: black;
        -fx-font-size: 16px;
        -fx-font-weight: bold;
        -fx-background-radius: 20;
        -fx-alignment: CENTER;
             -fx-alignment: CENTER_LEFT;
            -fx-padding: 0 0 0 35;
    """);
    }

    private void showExportDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Export answers");

        dialog.setGraphic(null);

        dialog.getDialogPane().setStyle("""
        -fx-background-color: #1A1A1A;
    """);

        Label label = new Label("Choose the format to export");
        label.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");

        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll("CSV", "Excel (.xlsx)");
        combo.getSelectionModel().select(0);

        combo.setStyle("""
        -fx-background-color: #1A1A1A;
        -fx-border-color: #FF7A00;
        -fx-border-width: 2;
        -fx-border-radius: 6;
        -fx-background-radius: 6;
        -fx-text-fill: white;
    """);

        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                }
                setStyle("-fx-text-fill: white; -fx-background-color: #1A1A1A;");
            }
        });

        combo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: #1A1A1A;");
                } else {
                    setText(item);
                    setStyle("-fx-background-color: #1A1A1A; -fx-text-fill: white;");
                }

                this.hoverProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal) {
                        setStyle("-fx-background-color: #FF7A00; -fx-text-fill: black;");
                    } else if (!empty && item != null) {
                        setStyle("-fx-background-color: #1A1A1A; -fx-text-fill: white;");
                    }
                });
            }
        });

        VBox content = new VBox(20, label, combo);
        content.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(content);

        ButtonType okButtonType = new ButtonType("Export", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(okButtonType);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(okButtonType);
        okButton.setStyle("""
        -fx-background-color: #FF7A00;
        -fx-text-fill: black;
        -fx-font-size: 14px;
        -fx-font-weight: bold;
        -fx-background-radius: 8;
        -fx-cursor: hand;
        -fx-padding: 8 20 8 20;
    """);

        ButtonType cancelButtonType = ButtonType.CANCEL;
        dialog.getDialogPane().getButtonTypes().add(cancelButtonType);

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelButtonType);
        cancelButton.setStyle("""
        -fx-background-color: #2E2E2E;
        -fx-text-fill: white;
        -fx-font-size: 14px;
        -fx-background-radius: 8;
        -fx-cursor: hand;
        -fx-padding: 8 20 8 20;
    """);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == okButtonType) {
                return combo.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(choice -> {
            if (choice.startsWith("CSV")) {
                exportAsDelimited(false);
            } else {
                exportAsDelimited(true);
            }
        });
    }


    private void exportAsDelimited(boolean excelLike) {
        FileChooser fc = new FileChooser();
        fc.setTitle(excelLike ? "Export to Excel" : "Export to CSV");
        if (excelLike) {
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Excel Files (*.xlsx)", "*.xlsx"));
        } else {
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        }

        File file = fc.showSaveDialog(this.getScene() != null ? this.getScene().getWindow() : null);
        if (file == null) return;

        String sep = ";";
        try (PrintWriter pw = new PrintWriter(file, StandardCharsets.UTF_8)) {
            pw.println("ID" + sep + "Name" + sep + "Email" + sep + "Answer" + sep + "Correct");

            int idx = 1;
            for (StudentAnswerInfo info : results.getAnswers()) {
                String id = String.format("%09d", idx++);
                String correct = info.isCorrect() ? "YES" : "NO";
                pw.println(id + sep
                        + safe(info.getStudentName()) + sep
                        + safe(info.getStudentEmail()) + sep
                        + safe(info.getAnswerLetter()) + sep
                        + correct);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error exporting file: " + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}