package GUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class GUIController implements Initializable {

    private Desktop desktop = Desktop.getDesktop();
    final FileChooser fileChooser = new FileChooser();
    private GUI gui = new GUI();

    @FXML
    TextField filePath;
    @FXML
    TextField filePath2;

    @FXML
    BorderPane blowers;
    @FXML
    BorderPane projects;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void chooseFile(ActionEvent actionEvent) {
        System.out.println("Button clicked");
        File file = fileChooser.showOpenDialog(gui.getStage());
        // todo podla toho v kt okne som
        if (file != null) {
            filePath.setText(file.getPath());
        }
    }

    public void loadFile(ActionEvent actionEvent) {
        try {
            File file = new File(filePath.getText());
            desktop.open(file);
            System.out.println("File successfully opened");
        } catch (IllegalArgumentException | IOException e) {
            System.err.println("Error opening file");
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("ERROR");
            alert.setHeaderText("Error loading file");
            alert.setContentText(e.getMessage());
            alert.showAndWait();

//            Logger.getLogger( GUI.class.getName()).log( Level.SEVERE, null, e );
        }
    }
}
