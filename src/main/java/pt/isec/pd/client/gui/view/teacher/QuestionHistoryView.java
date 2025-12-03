package pt.isec.pd.client.gui.view.teacher;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import pt.isec.pd.client.*;
import pt.isec.pd.common.Question;
import pt.isec.pd.common.User;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class QuestionHistoryView extends BorderPane {

    private final ClientAPI client;
    private final StateManager stateManager;
    private final User user;

    private static final String COLOR_PRIMARY = "#FF7A00";
    private static final String COLOR_BG = "#1A1A1A";
    private static final double MAX_CONTENT_WIDTH = 950;

    private static final double TABLE_HEIGHT = 230;

    private static final double W_DATE = 100;
    private static final double W_QUESTION = 230;
    private static final double W_ACTIVE = 90;
    private static final double W_ANSWERS = 100;
    private static final double W_SCROLL = 50;
    private static final double W_TOTAL = W_DATE + W_QUESTION + W_ACTIVE + W_ANSWERS + W_SCROLL;

    private static final String SVG_ARROW_UP = "M8.48877 0C8.93408 0 9.34229 0.182454 9.71338 0.547363L16.5786 7.57031C16.7147 7.71257 16.8198 7.86719 16.894 8.03418C16.9621 8.20736 16.9961 8.3929 16.9961 8.59082C16.9961 8.86914 16.9312 9.11963 16.8013 9.34229C16.6652 9.57113 16.4858 9.75049 16.2632 9.88037C16.0405 10.0164 15.7931 10.0845 15.521 10.0845C15.1066 10.0845 14.7417 9.92676 14.4263 9.61133L8.1084 3.10791H8.85986L2.56055 9.61133C2.24512 9.92676 1.8833 10.0845 1.4751 10.0845C1.20296 10.0845 0.955566 10.0164 0.73291 9.88037C0.510254 9.75049 0.333984 9.57113 0.204102 9.34229C0.0680339 9.11963 0 8.86914 0 8.59082C0 8.1888 0.13916 7.84863 0.41748 7.57031L7.27344 0.547363C7.45898 0.361816 7.65072 0.225749 7.84863 0.13916C8.04655 0.0525716 8.25993 0.0061849 8.48877 0Z";
    private static final String SVG_ARROW_DOWN = "M8.48877 10.0845C8.04346 10.0845 7.63525 9.90204 7.26416 9.53713L0.408936 2.51418C0.272869 2.37192 0.167727 2.2173 0.0935078 2.05031C0.0254737 1.87713 -0.00854492 1.69159 -0.00854492 1.49367C-0.00854492 1.21535 0.0563819 0.964861 0.186264 0.742205C0.322332 0.513368 0.501694 0.334008 0.72435 0.204126C0.947006 0.0680589 1.19439 0 1.46654 0C1.88092 0 2.24582 0.157746 2.56125 0.473176L8.87889 6.97659H8.12742L15.4365 0.473176C15.7519 0.157746 16.1137 0 16.5219 0C16.7941 0 17.0415 0.0680589 17.2641 0.204126C17.4868 0.334008 17.6661 0.513368 17.796 0.742205C17.9321 0.964861 18 1.21535 18 1.49367C18 1.89569 17.8608 2.23586 17.5825 2.51418L10.7266 9.53713C10.541 9.72268 10.3493 9.85875 10.1513 9.94534C9.95341 10.0319 9.74003 10.0783 9.51121 10.0845Z";
    private static final String SVG_BACK = "M0 8.50732C0 8.06201 0.182454 7.65381 0.547363 7.28271L7.57031 0.41748C7.71257 0.281413 7.86719 0.17627 8.03418 0.102051C8.20736 0.0340169 8.3929 0 8.59082 0C8.86914 0 9.11963 0.0649414 9.34229 0.194824C9.57113 0.330892 9.75049 0.510254 9.88037 0.73291C10.0164 0.955566 10.0845 1.20296 10.0845 1.4751C10.0845 1.88949 9.92676 2.25439 9.61133 2.56982L3.10791 8.8877V8.13623L9.61133 14.4355C9.92676 14.751 10.0845 15.1128 10.0845 15.521C10.0845 15.7931 10.0164 16.0405 9.88037 16.2632C9.75049 16.4858 9.57113 16.6621 9.34229 16.792C9.11963 16.9281 8.86914 16.9961 8.59082 16.9961C8.1888 16.9961 7.84863 16.8569 7.57031 16.5786L0.547363 9.72266C0.361816 9.53711 0.225749 9.34538 0.13916 9.14746C0.0525716 8.94954 0.0061849 8.73617 0 8.50732Z";

    private final VBox rowsBox = new VBox(0);

    private final TextField startDateField = new TextField();
    private final TextField endDateField = new TextField();

    private Status currentStatusFilter = Status.ACTIVE;
    private final Circle dotActive = new Circle(6);
    private final Circle dotFuture = new Circle(6);
    private final Circle dotExpired = new Circle(6);
    private final Label lblActive = new Label("Active");
    private final Label lblFuture = new Label("Future");
    private final Label lblExpired = new Label("Expired");

    private final Button backButton = new Button("Back");
    private Label actH;

    private final List<QuestionItem> allItems = new ArrayList<>();

    public QuestionHistoryView(ClientAPI client, StateManager stateManager, User user) {
        this.client = client;
        this.stateManager = stateManager;
        this.user = user;

        setStyle("-fx-background-color: " + COLOR_BG + ";");

        BorderPane layout = new BorderPane();

        HeaderView header = new HeaderView(stateManager, user);
        layout.setTop(header);

        BorderPane centerContent = createCenterContent();
        centerContent.setMaxWidth(MAX_CONTENT_WIDTH);

        StackPane centerWrapper = new StackPane(centerContent);
        centerWrapper.setAlignment(Pos.TOP_CENTER);
        StackPane.setMargin(centerContent, new Insets(40, 0, 40, 0));

        layout.setCenter(centerWrapper);

        StackPane root = new StackPane(layout);
        header.attachToRoot(root);

        this.setCenter(root);

        loadData();
        applyFilters();
        updateFilterVisuals();
    }

    private BorderPane createCenterContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(0, 40, 40, 40));

        Label title = new Label("View History");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        VBox titleBox = new VBox(title);
        titleBox.setAlignment(Pos.TOP_LEFT);
        titleBox.setPadding(new Insets(0, 0, 20, 0));
        root.setTop(titleBox);

        Region table = createHistoryTable();
        VBox filters = createFiltersPanel();

        HBox center = new HBox(60, table, filters);
        center.setAlignment(Pos.TOP_LEFT);

        HBox.setHgrow(table, Priority.ALWAYS);
        root.setCenter(center);

        styleBackButton(backButton);
        backButton.setOnAction(e -> {
            if (stateManager != null)
                stateManager.showTeacherMenu(user);
        });

        HBox backBox = new HBox(backButton);
        backBox.setAlignment(Pos.CENTER);
        backBox.setPadding(new Insets(40, 0, 0, 0));
        root.setBottom(backBox);

        return root;
    }

    private Region createHistoryTable() {
        VBox wrapper = new VBox();
        wrapper.setPadding(new Insets(0));
        wrapper.setStyle("""
            -fx-border-color: #FF7A00;
            -fx-border-width: 1.5;
            -fx-border-radius: 6;
        """);

        HBox header = new HBox(0);
        header.setAlignment(Pos.CENTER_LEFT);

        Label dateH = createHeaderLabel("Date", W_DATE, Pos.CENTER_LEFT);
        Label questH = createHeaderLabel("Question", W_QUESTION, Pos.CENTER_LEFT);
        actH = createHeaderLabel("Active", W_ACTIVE, Pos.CENTER);
        Label ansH = createHeaderLabel("Answers", W_ANSWERS, Pos.CENTER);
        Label scrollH = createHeaderLabel("", W_SCROLL, Pos.CENTER);

        header.getChildren().addAll(
                addRightBorder(dateH, W_DATE),
                addRightBorder(questH, W_QUESTION),
                addRightBorder(actH, W_ACTIVE),
                addRightBorder(ansH, W_ANSWERS),
                scrollH
        );

        VBox titleHeader = new VBox(header);
        titleHeader.setStyle("-fx-background-color: transparent; -fx-border-color: #FF7A00; -fx-border-width: 0 0 1.5 0;");

        rowsBox.setFillWidth(true);
        rowsBox.setStyle("-fx-background-color: transparent;");

        ScrollPane scroll = new ScrollPane(rowsBox);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(TABLE_HEIGHT);
        scroll.setMinHeight(TABLE_HEIGHT);
        scroll.setStyle("""
            -fx-background: transparent;
            -fx-background-color: transparent;
            -fx-border-color: transparent;
        """);

        HBox gridOverlay = new HBox(0);
        gridOverlay.setAlignment(Pos.CENTER_LEFT);
        gridOverlay.setMaxWidth(Double.MAX_VALUE);
        gridOverlay.setMouseTransparent(true);
        gridOverlay.getChildren().addAll(
                createGridCol(W_DATE, true),
                createGridCol(W_QUESTION, true),
                createGridCol(W_ACTIVE, true),
                createGridCol(W_ANSWERS, false)
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
            double lineHeight = 30.0;
            double scrollableHeight = rowsBox.getHeight() - TABLE_HEIGHT;
            if (scrollableHeight > 0) {
                double increment = lineHeight / scrollableHeight;
                scroll.setVvalue(Math.max(0, scroll.getVvalue() - increment));
            }
        });

        StackPane btnDown = new StackPane(arrowDownIcon);
        btnDown.setCursor(Cursor.HAND);
        btnDown.setPadding(new Insets(0, 0, 8, 0));
        btnDown.setPrefHeight(40);
        btnDown.setOnMouseClicked(e -> {
            double lineHeight = 30.0;
            double scrollableHeight = rowsBox.getHeight() - TABLE_HEIGHT;
            if (scrollableHeight > 0) {
                double increment = lineHeight / scrollableHeight;
                scroll.setVvalue(Math.min(1.0, scroll.getVvalue() + increment));
            }
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        arrowsColumn.getChildren().addAll(btnUp, spacer, btnDown);
        arrowsColumn.setMinHeight(TABLE_HEIGHT);

        StackPane dataLayer = new StackPane();
        dataLayer.setAlignment(Pos.TOP_LEFT);
        dataLayer.getChildren().addAll(scroll, gridOverlay);
        dataLayer.setMaxWidth(Double.MAX_VALUE);
        dataLayer.setMinHeight(TABLE_HEIGHT);
        HBox.setHgrow(dataLayer, Priority.ALWAYS);

        HBox contentRow = new HBox(0, dataLayer, arrowsColumn);
        contentRow.setAlignment(Pos.TOP_LEFT);
        contentRow.setMaxWidth(Double.MAX_VALUE);
        contentRow.setMinHeight(TABLE_HEIGHT);
        HBox.setHgrow(contentRow, Priority.ALWAYS);

        VBox.setVgrow(contentRow, Priority.ALWAYS);
        wrapper.getChildren().addAll(titleHeader, contentRow);

        wrapper.setMaxWidth(W_TOTAL);

        return wrapper;
    }

    private void renderRows(List<QuestionItem> items) {
        rowsBox.getChildren().clear();

       if (items.isEmpty()) {
            Label emptyMsg = new Label("No questions found matching your filters.");
            emptyMsg.setStyle("-fx-text-fill: #D9D9D9; -fx-font-size: 16px; -fx-font-weight: bold;");
            emptyMsg.setAlignment(Pos.CENTER);
            emptyMsg.setMinHeight(TABLE_HEIGHT - 30);
            emptyMsg.setPrefWidth(W_TOTAL - W_SCROLL);
            rowsBox.getChildren().add(emptyMsg);
            return;
        }

        for (QuestionItem item : items) {
            Question q = item.question;

            HBox row = new HBox(0);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMinHeight(30);
            row.setMaxWidth(Double.MAX_VALUE);

            Label date = new Label(q.getStartTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            date.setStyle("-fx-text-fill:white; -fx-font-size:12px;");
            row.getChildren().add(createCell(date, W_DATE, Pos.CENTER_LEFT));

            Label text = new Label(truncate(q.getQuestion(), 25));
            text.setStyle("-fx-text-fill:white; -fx-font-size:12px;");
            text.setCursor(Cursor.HAND);

            text.setOnMouseClicked(e -> {
                e.consume();
                stateManager.showEditQuestionView(user, item.question);
            });

            row.getChildren().add(createCell(text, W_QUESTION, Pos.CENTER_LEFT));

            Circle dot = new Circle(5);
            dot.setFill(getStatusColor(q));
            row.getChildren().add(createCell(dot, W_ACTIVE, Pos.CENTER));

            Label answers = new Label(Integer.toString(item.question.getTotalAnswers()));
            answers.setStyle("-fx-text-fill:white; -fx-font-size:12px;");
            answers.setCursor(Cursor.HAND);

            answers.setOnMouseClicked(e -> {
                e.consume();
                stateManager.showCheckQuestionDataView(user, new ClientServiceMock().getQuestionResults(UserManager.getInstance().getUser(), q.getQuestion())); // TODO
            });

            StackPane answersCell = createCell(answers, W_ANSWERS, Pos.CENTER);

            answersCell.setOnMouseClicked(e -> {
                e.consume();
                stateManager.showCheckQuestionDataView(user, new ClientServiceMock().getQuestionResults(UserManager.getInstance().getUser(), q.getQuestion())); // TODO
            });

            row.getChildren().add(answersCell);

            rowsBox.getChildren().add(row);
        }
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

    private StackPane addRightBorder(Node node, double width) {
        StackPane p = new StackPane(node);
        p.setPrefWidth(width);
        p.setMinWidth(width);
        p.setMaxWidth(width);
        p.setStyle("-fx-border-color: #FF7A00; -fx-border-width: 0 1.5 0 0;");
        return p;
    }

    private Label createHeaderLabel(String text, double width, Pos alignment) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(width);
        lbl.setMaxWidth(width);
        lbl.setAlignment(alignment);
        if (alignment == Pos.CENTER_LEFT) lbl.setPadding(new Insets(0, 0, 0, 10));
        lbl.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
        return lbl;
    }

    private VBox createFiltersPanel() {
        VBox panel = new VBox(22);
        panel.setAlignment(Pos.TOP_LEFT);

        Label filtersTitle = new Label("Filters");
        filtersTitle.setStyle("-fx-text-fill: #FF7A00; -fx-font-size: 18px; -fx-font-weight: bold;");

        VBox startDateBox = createDateFilterDropdown(startDateField, "Start Date");
        VBox endDateBox = createDateFilterDropdown(endDateField, "End Date");

        VBox dateBox = new VBox(12, startDateBox, endDateBox);

        Button applyBtn = new Button("Apply Filters");
        applyBtn.setCursor(Cursor.HAND);
        applyBtn.setPrefWidth(200);
        applyBtn.setPrefHeight(35);
        applyBtn.setStyle("""
        -fx-background-color: #FF7A00;
        -fx-text-fill: black;
        -fx-font-size: 14px;
        -fx-font-weight: bold;
        -fx-background-radius: 10;
    """);

        applyBtn.setOnAction(e -> applyFilters());

        VBox applyBox = new VBox(applyBtn);
        applyBox.setPadding(new Insets(5, 0, 0, 0));

        HBox rowActive = createFilterRow(Status.ACTIVE, dotActive, lblActive);
        HBox rowFuture = createFilterRow(Status.FUTURE, dotFuture, lblFuture);
        HBox rowExpired = createFilterRow(Status.EXPIRED, dotExpired, lblExpired);

        VBox radiosBox = new VBox(8, rowActive, rowFuture, rowExpired);
        radiosBox.setAlignment(Pos.TOP_LEFT);

        panel.getChildren().addAll(filtersTitle, dateBox, radiosBox, applyBox);
        return panel;
    }


    private HBox createFilterRow(Status status, Circle dot, Label label) {
        HBox row = new HBox(10, dot, label);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setCursor(Cursor.HAND);
        row.setOnMouseClicked(e -> {
            currentStatusFilter = status;
            updateFilterVisuals();
            applyFilters();
        });
        return row;
    }

    private void updateFilterVisuals() {
        updateSingleFilterVisual(Status.ACTIVE, dotActive, lblActive);
        updateSingleFilterVisual(Status.FUTURE, dotFuture, lblFuture);
        updateSingleFilterVisual(Status.EXPIRED, dotExpired, lblExpired);

        switch (currentStatusFilter) {
            case ACTIVE -> actH.setText("Active");
            case FUTURE -> actH.setText("Future");
            case EXPIRED -> actH.setText("Expired");
        }
    }

    private void updateSingleFilterVisual(Status status, Circle dot, Label label) {
        if (currentStatusFilter == status) {
            label.setStyle("-fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-size: 14px;");
            dot.setFill(Color.web(COLOR_PRIMARY));
            dot.setStroke(Color.WHITE);
            dot.setStrokeWidth(2);
            dot.setRadius(5);
        } else {
            label.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
            dot.setFill(Color.web("#D9D9D9"));
            dot.setStrokeWidth(0);
            dot.setRadius(6);
        }
    }

    private VBox createDateFilterDropdown(TextField field, String labelText) {
        StackPane fieldWrapper = new StackPane(field);
        VBox filterBox = new VBox(3);
        styleDateField(field, labelText);
        filterBox.getChildren().add(fieldWrapper);
        return filterBox;
    }

    private void styleDateField(TextField field, String placeholder) {
        field.setFocusTraversable(false);
        field.setPromptText(placeholder);
        field.setPrefWidth(200);
        field.setMinHeight(30);
        field.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-size: 13px; -fx-background-radius: 8;");

        field.textProperty().addListener((obs, oldV, newV) -> {
            newV = newV.replaceAll("[^0-9/]", "");
            if (newV.length() == 2 && !oldV.endsWith("/")) newV += "/";
            if (newV.length() == 5 && oldV.length() < 5) newV += "/";
            if (newV.length() > 10) newV = oldV;
            field.setText(newV);
        });

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                if (field.getText().trim().isEmpty()) {
                    field.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-size: 13px; -fx-background-radius: 8;");
                } else if (!isValidDate(field.getText())) {
                    field.setStyle("-fx-background-color: #ffcccc; -fx-text-fill: black; -fx-font-size: 13px; -fx-background-radius: 8; -fx-border-color: red;");
                }
            } else {
                field.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-size: 13px; -fx-background-radius: 8;");
            }
        });
    }


    private String truncate(String t, int max) {
        return t.length() <= max ? t : t.substring(0, max - 3) + "...";
    }

    private SVGPath createArrowIcon(String svgContent) {
        SVGPath svg = new SVGPath();
        svg.setContent(svgContent);
        svg.setFill(Color.web(COLOR_PRIMARY));
        svg.setScaleX(1.2);
        svg.setScaleY(1.2);
        return svg;
    }

    private void loadData() {
        allItems.clear();

        // Chamar o servidor (numa thread separada para não bloquear UI)
        new Thread(() -> {
            try {
                List<Question> questions = client.getTeacherQuestions(user, null);

                Platform.runLater(() -> {
                    for (Question q : questions) {
                        allItems.add(new QuestionItem(q));
                    }
                    applyFilters(); // Atualiza a tabela com os dados carregados
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }



    private void applyFilters() {
        LocalDate startFilter = parseDate(startDateField.getText());
        LocalDate endFilter = parseDate(endDateField.getText());
        LocalDateTime now = LocalDateTime.now();

        List<QuestionItem> filtered = allItems.stream().filter(item -> {
            Question q = item.question;
            if (startFilter != null && q.getStartTime().toLocalDate().isBefore(startFilter)) return false;
            if (endFilter != null && q.getEndTime().toLocalDate().isAfter(endFilter)) return false;

            if (currentStatusFilter != null) {
                boolean isActive = now.isAfter(q.getStartTime()) && now.isBefore(q.getEndTime());
                boolean isFuture = now.isBefore(q.getStartTime());
                boolean isExpired = now.isAfter(q.getEndTime());
                return switch (currentStatusFilter) {
                    case ACTIVE -> isActive;
                    case FUTURE -> isFuture;
                    case EXPIRED -> isExpired;
                };
            }
            return true;
        }).toList();

        renderRows(filtered);
    }

    private LocalDate parseDate(String text) {
        try {
            if (!isValidDate(text)) return null;
            String[] p = text.split("/");
            return LocalDate.of(Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0]));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isValidDate(String text) {
        if (text == null || !text.matches("\\d{2}/\\d{2}/\\d{4}")) return false;
        try {
            String[] p = text.split("/");
            LocalDate.of(Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0]));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Color getStatusColor(Question q) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(q.getStartTime()) && now.isBefore(q.getEndTime())) return Color.web(COLOR_PRIMARY);
        if (now.isBefore(q.getStartTime())) return Color.web("#FFFFFF");
        return Color.web("#808080");
    }

    private void styleBackButton(Button btn) {
        btn.setCursor(Cursor.HAND);
        btn.setPrefWidth(260);
        btn.setPrefHeight(40);

        SVGPath arrow = new SVGPath();
        arrow.setContent(SVG_BACK);
        arrow.setFill(Color.BLACK);
        arrow.setScaleX(0.8);
        arrow.setScaleY(0.8);

        btn.setGraphic(arrow);
        btn.setGraphicTextGap(60);

        btn.setStyle("""
            -fx-background-color: #FF7A00;
            -fx-text-fill: black;
            -fx-font-size: 16px;
            -fx-font-weight: bold;
            -fx-background-radius: 12;
            -fx-alignment: CENTER_LEFT;
            -fx-padding: 0 0 0 35;
        """);
    }

//    private static class QuestionItem {
//        final Question question;
//        final ClientAPI.TeacherResultsData results;
//
//        QuestionItem(Question question, ClientAPI.TeacherResultsData results) {
//            this.question = question;
//            this.results = results;
//        }
//    }

    private static class QuestionItem {
        final Question question;
        QuestionItem(Question question) {
            this.question = question;
        }
    }


    private enum Status { ACTIVE, FUTURE, EXPIRED }
}