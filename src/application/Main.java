package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private static Stage globalStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        globalStage = primaryStage;
        primaryStage.setTitle("Flashcard Study App");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginWindow.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 650, 500);
        applyThemeToScene(scene);

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(500);
        primaryStage.setMinHeight(400);
        primaryStage.show();
    }

    public static void switchScene(Parent root) {
        switchScene(root, 900, 650);
    }

    public static void switchScene(Parent root, double width, double height) {
        if (globalStage == null) return;
        width = Math.max(width, globalStage.getMinWidth());
        height = Math.max(height, globalStage.getMinHeight());

        Scene s = globalStage.getScene();
        if (s == null) {
            Scene scene = new Scene(root, width, height);
            applyThemeToScene(scene);
            globalStage.setScene(scene);
        } else {
            double currentWidth = globalStage.getWidth();
            double currentHeight = globalStage.getHeight();
            s.setRoot(root);
            applyThemeToMainStage();
            if (currentWidth != width || currentHeight != height) {
                globalStage.setWidth(width);
                globalStage.setHeight(height);
            }
        }
        globalStage.centerOnScreen();
    }

    public static void applyThemeToScene(Scene scene) {
        scene.getStylesheets().clear();
        scene.getStylesheets().add(Main.class.getResource("style.css").toExternalForm());
        if ("dark".equals(SessionData.getTheme())) {
            try { 
                scene.getStylesheets().add(Main.class.getResource("dark.css").toExternalForm()); 
            } catch (Exception ignore) {}
        }
    }

    public static void applyThemeToMainStage() {
        if (globalStage == null) return;
        Scene s = globalStage.getScene();
        if (s != null) applyThemeToScene(s);
    }

    public static Stage getStage() {
        return globalStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}