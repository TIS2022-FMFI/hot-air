package GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;


public class GUI extends Application {

    private Stage stage;
    public int numberOfBlowers = 10;   // todo ziska sa zo servra?
    public int numberOfProjects = 1;   // todo ziska sa zo servra?


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(GUI.class.getResource("gui.fxml")));
            Scene scene = new Scene(root, 800, 800);
            String css = Objects.requireNonNull(this.getClass().getResource("styles.css")).toExternalForm();

            scene.getStylesheets().add(css);

            stage.setScene(scene);
            stage.setTitle("BURNIEE");
//          todo poriesit nejaku lepsiu ikonu
            stage.getIcons().add(new Image(Objects.requireNonNull(GUI.class.getResourceAsStream("icon.jpg"))));
            stage.setMinHeight(810);
            stage.setMinWidth(810);
            stage.show();
            this.stage = stage;


        } catch (Exception e) {
            System.err.println(e);
            // todo zapisat do logov, z gui?
        }

    }

    public Stage getStage() {
        return this.stage;
    }
}
