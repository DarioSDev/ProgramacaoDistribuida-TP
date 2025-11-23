package pt.isec.pd.client.gui.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class HeaderView extends BorderPane {

    public static final int HEIGHT = 120;
    private static final String COLOR_PRIMARY = "#FF7A00";

    public HeaderView(Runnable onMenuClick) {

        setPrefHeight(HEIGHT);
        setMinHeight(HEIGHT);
        setMaxHeight(HEIGHT);
        setStyle("-fx-background-color: #1A1A1A;");

        Label title = new Label("Questia");
        title.setStyle("""
            -fx-font-size: 60px;
            -fx-font-weight: bold;
            -fx-text-fill: #FF7A00;
            """);

        BorderPane.setMargin(title, new Insets(25, 0, 0, 40));
        setLeft(title);

        HBox rightBox = new HBox();
        rightBox.setAlignment(Pos.TOP_RIGHT);
        rightBox.setPadding(new Insets(50, 40, 0, 0));

        StackPane menu = createMenuButton();
        menu.setOnMouseClicked(e -> onMenuClick.run());

        rightBox.getChildren().add(menu);
        setRight(rightBox);
    }

    private StackPane createMenuButton() {

        StackPane btn = new StackPane();
        btn.setCursor(Cursor.HAND);

        btn.setPrefSize(36, 36);
        btn.setMinSize(36, 36);
        btn.setMaxSize(36, 36);

        btn.setStyle("""
            -fx-background-color: #FF7A00;
            -fx-background-radius: 8;
            """);

        Rectangle line1 = new Rectangle(14, 2, Color.BLACK);
        Rectangle line2 = new Rectangle(14, 2, Color.BLACK);
        Rectangle line3 = new Rectangle(14, 2, Color.BLACK);

        StackPane.setMargin(line1, new Insets(0, 0, 10, 0));
        StackPane.setMargin(line3, new Insets(10, 0, 0, 0));

        StackPane icon = new StackPane(line1, line2, line3);

        btn.getChildren().add(icon);

        return btn;
    }
}
