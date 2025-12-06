package pt.isec.pd.client.gui.view;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AboutQuestia {

    private static final String COLOR_BG = "#1A1A1A";
    private static final String COLOR_PRIMARY = "#FF7A00";

    public static void show(Stage owner) {
        Stage popup = new Stage();
        popup.initOwner(owner);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setResizable(false);
        popup.setTitle("About Questia");

        VBox root = new VBox(35);
        root.setPadding(new Insets(35, 55, 45, 55));
        root.setAlignment(Pos.TOP_CENTER);

        root.setStyle("""
            -fx-background-color: #1A1A1A;
            -fx-border-color: #FF7A00 transparent transparent transparent;
            -fx-border-width: 4 0 0 0;
        """);

        root.setEffect(new DropShadow(25, Color.color(0, 0, 0, 0.45)));

        Label title = new Label("About Questia");
        title.setStyle("""
            -fx-font-size: 34px;
            -fx-font-weight: bold;
            -fx-text-fill: white;
        """);

        Region underline = new Region();
        underline.setPrefWidth(180);
        underline.setPrefHeight(3);
        underline.setStyle("""
            -fx-background-color: #FF7A00;
            -fx-background-radius: 2;
        """);

        Label subtitle = new Label("Developed by:");
        subtitle.setStyle("""
            -fx-font-size: 20px;
            -fx-font-weight: 500;
            -fx-text-fill: #CCCCCC;
        """);

        HBox row = new HBox(50);
        row.setAlignment(Pos.CENTER);

        row.getChildren().add(personCard("Dario Santos", "/images/dario.jpg", "2021110772"));
        row.getChildren().add(personCard("Evilania Lima", "/images/evilania.jpg", "2021125566"));
        row.getChildren().add(personCard("José Bastos", "/images/jose.jpg", "2021127160"));

        root.getChildren().addAll(title, underline, subtitle, row);

        Scene scene = new Scene(root);
        popup.setScene(scene);

        FadeTransition ft = new FadeTransition(Duration.millis(220), root);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        popup.showAndWait();
    }

    private static VBox personCard(String name, String imgPath, String number) {

        ImageView img = new ImageView(new Image(imgPath));
        img.setFitWidth(140);
        img.setFitHeight(140);

        Circle clip = new Circle(70, 70, 70);
        img.setClip(clip);

        StackPane imgContainer = new StackPane(img);
        imgContainer.setPadding(new Insets(4));

        imgContainer.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 80;
            -fx-border-color: #FF7A00;
            -fx-border-radius: 80;
            -fx-border-width: 3;
        """);

        imgContainer.setOnMouseEntered(e -> {
            imgContainer.setScaleX(1.07);
            imgContainer.setScaleY(1.07);
            imgContainer.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 80;
                -fx-border-color: #FF9A33;
                -fx-border-width: 3;
                -fx-border-radius: 80;
                -fx-effect: dropshadow(gaussian, #FF7A00, 18, 0.45, 0, 0);
            """);
        });

        imgContainer.setOnMouseExited(e -> {
            imgContainer.setScaleX(1);
            imgContainer.setScaleY(1);
            imgContainer.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 80;
                -fx-border-color: #FF7A00;
                -fx-border-width: 3;
                -fx-border-radius: 80;
                -fx-effect: none;
            """);
        });

        Label labelName = new Label(name);
        labelName.setStyle("""
            -fx-font-size: 18px;
            -fx-font-weight: 700;
            -fx-text-fill: white;
        """);

        Label labelNumber = new Label(number);
        labelNumber.setStyle("""
            -fx-font-size: 14px;
            -fx-text-fill: #BBBBBB;
        """);

        VBox card = new VBox(10, imgContainer, labelName, labelNumber);
        card.setAlignment(Pos.CENTER);

        return card;
    }
}
