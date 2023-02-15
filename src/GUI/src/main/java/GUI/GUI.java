package GUI;

import Communication.ClientHandler;
import Logs.GeneralLogger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


/**
 * Main class for GUI application
 */
public class GUI extends Application {

    public static GUI gui;
    private Stage stage;
    public static ClientHandler client;
    private File configFile;

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

            int port = 4002;
            try {
                BufferedReader br = new BufferedReader(new FileReader(configFile.getPath()));
                br.readLine();
                port = Integer.parseInt(br.readLine());
            } catch (IOException | NumberFormatException e) {
                GeneralLogger.writeExeption(e);
                System.err.println("GUI config load error");
            }

            client = new ClientHandler(port);

            Parent root = FXMLLoader.load(Objects.requireNonNull(GUI.class.getResource("gui.fxml")));

            Scene scene = new Scene(root, 910, 510);
            String css = Objects.requireNonNull(this.getClass().getResource("styles.css")).toExternalForm();
            scene.getStylesheets().add(css);
            stage.setOnCloseRequest(t -> {
                Platform.exit();
                System.exit(0);
            });
            stage.setScene(scene);
            stage.setTitle("BURNIEE");
            stage.getIcons().add(new Image(Objects.requireNonNull(GUI.class.getResourceAsStream("boge_icon.jpg"))));
            stage.setMinHeight(310);
            stage.setMinWidth(915);
            stage.show();
            this.stage = stage;

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

    /**
     * Show exception in GUI.
     *
     * @param e exception that occurred
     */
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
    }

    /**
     * Create config file for GUI.
     */
    private void createGUIConfigFile() {
        try {
            configFile = new File("GUIconfig.txt");
            if (configFile.createNewFile()) {
                GeneralLogger.writeMessage("Config file created");
            }
        } catch (IOException e) {
            GeneralLogger.writeExeption(e);
            System.err.println("An error occurred when creating GUI config file.");
            System.err.println(e);
        }
    }

    /**
     * Refresh GUI.
     */
    public void refresh() {
        if (GUIController.guiController != null){
            GUIController.guiController.updateTable();
        }
    }

}