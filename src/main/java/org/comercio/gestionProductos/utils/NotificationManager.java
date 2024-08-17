package org.comercio.gestionProductos.utils;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.LinkedList;
import java.util.Queue;

public class NotificationManager {

    private static final Queue<Popup> notifications = new LinkedList<>();

    public static void showNotification(String message, Stage primaryStage) {
        Platform.runLater(() -> {
            Label label = new Label(message);
            label.setStyle("-fx-background-color: #2a9d8f; -fx-text-fill: white; -fx-padding: 10px; -fx-border-radius: 5px; -fx-background-radius: 5px;");

            StackPane pane = new StackPane(label);
            pane.setStyle("-fx-padding: 10px;");
            pane.setAlignment(Pos.BOTTOM_RIGHT);

            Popup popup = new Popup();
            popup.getContent().add(pane);

            double stageX = primaryStage.getX();
            double stageY = primaryStage.getY();
            double stageWidth = primaryStage.getWidth();
            double stageHeight = primaryStage.getHeight();

            popup.setX(stageX + stageWidth - 250); // Ajusta el valor para posicionar correctamente
            popup.setY(stageY + stageHeight - (50 + notifications.size() * 60)); // Ajusta para las notificaciones en cascada

            notifications.add(popup);
            popup.show(primaryStage);

            Timeline timeline = new Timeline(new KeyFrame(
                    Duration.millis(5000),
                    ae -> {
                        popup.hide();
                        notifications.remove(popup);
                    }));
            timeline.setCycleCount(1);
            timeline.play();
        });
    }
}
