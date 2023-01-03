package GUI;

import Communication.ClientHandler;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Objects;


/**
 * Main class for GUI application
 */
public class GUI extends Application {

    public static GUI gui;
    private Stage stage;
    public ClientHandler client;

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        gui = this;
        try {
            createGUIConfigFile();

            client = new ClientHandler();
            Parent root = FXMLLoader.load(Objects.requireNonNull(GUI.class.getResource("gui.fxml")));
            Scene scene = new Scene(root, 810, 510);
            String css = Objects.requireNonNull(this.getClass().getResource("styles.css")).toExternalForm();

            scene.getStylesheets().add(css);

            stage.setScene(scene);
            stage.setTitle("BURNIEE");
            stage.getIcons().add(new Image(Objects.requireNonNull(GUI.class.getResourceAsStream("boge_icon.jpg"))));
            stage.setMinHeight(310);
            stage.setMinWidth(810);
            stage.show();

            this.stage = stage;

        } catch (Exception e) {
            System.err.println(e);
            alert(e);
            // todo zapisat do logov

        }

    }

    /**
     * Gets stage.
     *
     * @return the stage for GUI controller
     */
    public Stage getStage() {
        return this.stage;
    }

    public void alert(Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("ERROR");
        alert.setHeaderText(e.getMessage());
        alert.setContentText(e.toString());

        ImageView icon = new ImageView(String.valueOf(GUI.class.getResource("error.png")));
        icon.setFitHeight(48);
        icon.setFitWidth(48);

        alert.getDialogPane().setGraphic(icon);
        alert.show();
    }

    public void createGUIConfigFile() {
        try {
            File config = new File("GUIconfig.txt");
            if (config.createNewFile()) {
                System.out.println("File created: " + config.getName());
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.err.println("An error occurred.");
            e.printStackTrace();
        }
    }

}
