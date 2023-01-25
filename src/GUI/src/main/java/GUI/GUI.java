package GUI;

import Communication.ClientHandler;
import Logs.GeneralLogger;
import Logs.TemperatureLogsDeleter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static GUI.GUIController.setAlertIcons;


/**
 * Main class for GUI application
 */
public class GUI extends Application {

    public static GUI gui;
    private Stage stage;
    public static ClientHandler client;

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

            client = new ClientHandler();   // todo debug

            Parent root = FXMLLoader.load(Objects.requireNonNull(GUI.class.getResource("gui.fxml")));
            Scene scene = new Scene(root, 850, 510);
            String css = Objects.requireNonNull(this.getClass().getResource("styles.css")).toExternalForm();
            scene.getStylesheets().add(css);
            stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent t) {
                    Platform.exit();
                    System.exit(0);
                }
            });
            stage.setScene(scene);
            stage.setTitle("BURNIEE");
            stage.getIcons().add(new Image(Objects.requireNonNull(GUI.class.getResourceAsStream("boge_icon.jpg"))));
            stage.setMinHeight(310);
            stage.setMinWidth(900);
            stage.show();
            this.stage = stage;

            System.out.println("GUI successfully started");
        } catch (Exception e) {
            GeneralLogger.writeExeption(e);
            System.err.println(e.getMessage());
            e.printStackTrace();
            alert(e);
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
        alert.setResizable(true);
        alert.setTitle("ERROR");
        alert.setHeaderText(e.getMessage());
        alert.setContentText(e.toString());
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResource("boge_icon.jpg")).toString()));
        ImageView icon = new ImageView(String.valueOf(GUI.class.getResource("error.png")));
        icon.setFitHeight(48);
        icon.setFitWidth(48);
        alert.getDialogPane().setGraphic(icon);
        alert.show();

        GeneralLogger.writeExeption(e);
        System.err.println(e.getMessage());
        e.printStackTrace();
    }

    public void createGUIConfigFile() {
        try {
            File config = new File("GUIconfig.txt");
            if (config.createNewFile()) {
                System.out.println("Config file created");
            }
        } catch (IOException e) {
            System.err.println("An error occurred.");
            System.err.println(e);
            e.printStackTrace();
            GeneralLogger.writeExeption(e);
        }
    }

    public void refresh() {
        if (GUIController.guiController != null){
            System.out.println("refresh was successful");
            GUIController.guiController.updateTable();
        } else {
            System.err.println("refresh was not successful");
        }
    }

    public boolean deleteLogFiles() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setResizable(true);
        alert.setTitle("DELETE TEMPERATURE LOG FILES");
        alert.setHeaderText("Do you want to delete old temperature log files?");

        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(Objects.requireNonNull(GUIController.class.getResource("boge_icon.jpg")).toString()));
        ImageView icon = new ImageView(String.valueOf(GUI.class.getResource("question.png")));
        icon.setFitHeight(48);
        icon.setFitWidth(48);
        alert.getDialogPane().setGraphic(icon);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK){
            System.out.println("temperature log files will be deleted");
            GeneralLogger.writeMessage("temperature log files will be deleted");
            TemperatureLogsDeleter.deleteFiles();
            return true;
        } else {
            System.out.println("temperature log files will not be deleted");
            return false;
        }
    }

}
