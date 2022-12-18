package GUI;

import XML.XMLEditor;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import org.xml.sax.SAXException;


import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

/* todo
     - STOP buttony na duchadla
     - nejaky refresh btn na najdenie duchadiel
     -
 */

/**
 *  Gui controller.
 */
public class GUIController implements Initializable {

    private Desktop desktop = Desktop.getDesktop();
    final FileChooser fileChooser = new FileChooser();
    private GUI gui = new GUI();

    final WebView browser = new WebView();
    final WebEngine webEngine = browser.getEngine();
    final Hyperlink[] links = new Hyperlink[gui.numberOfBlowers];
    final static String url = "https://www.google.sk/";

    @FXML TextField filePath;
    @FXML TextField filePath2;
    @FXML TextField settingsPath;
    @FXML TextField settingsPort;

    @FXML BorderPane blowers;
    @FXML BorderPane projects;
    
    @FXML TableView<Blower> blowersView;
    @FXML TableColumn<Blower,String> blower_id;
    @FXML TableColumn<Blower,String> blower_ip_address;
    @FXML TableColumn<Blower,Float> blower_current_tmp;
    @FXML TableColumn<Blower,Float> blower_target_tmp;
    @FXML TableColumn<Blower,String> blower_project;
    @FXML TableColumn<Blower,Hyperlink> blower_config;
    @FXML TableColumn<Blower, Button> blower_stop; // todo, button nefunguje, posielat na server ptm ze sa zastavilo

    @FXML TableView<Project> projectsView;
    @FXML TableColumn<Project,String> project_name;
    @FXML TableColumn<Project,Float> project_time;
    @FXML TableColumn<Project,String> project_phase;

    
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        filePath.setTooltip(new Tooltip("Write path to xml file or search the file by clicking the search button."));
        filePath2.setTooltip(new Tooltip("Write path to xml file or search the file by clicking the search button."));
        settingsPath.setTooltip(new Tooltip("Enter default path to store exe file."));
        settingsPath.setPromptText("Enter default path to store exe file.");
        settingsPort.setTooltip(new Tooltip("Enter port to communicate with server."));
        settingsPort.setPromptText("Enter port to communicate with server.");

        UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            String input = change.getText();
            if (input.matches("[0-9]*")) {
                return change;
            }
            return null;
        };
        settingsPort.setTextFormatter(new TextFormatter<String>(integerFilter));


        blower_id.setCellValueFactory(
            new PropertyValueFactory<>("id"));
        blower_ip_address.setCellValueFactory(
            new PropertyValueFactory<>("IPAddress"));
        blower_current_tmp.setCellValueFactory(
            new PropertyValueFactory<>("currentTemp"));
        blower_target_tmp.setCellValueFactory(
            new PropertyValueFactory<>("targetTemp"));
        blower_project.setCellValueFactory(
            new PropertyValueFactory<>("project"));
        blower_config.setCellValueFactory(
            new PropertyValueFactory<>("link"));
        blower_stop.setCellValueFactory(
            new PropertyValueFactory<>("stop"));

        project_name.setCellValueFactory(
            new PropertyValueFactory<>("name"));
        project_time.setCellValueFactory(
            new PropertyValueFactory<>("time"));
        project_phase.setCellValueFactory(
            new PropertyValueFactory<>("currentPhase"));


        blowersView.getItems().addAll(addBlowers());
        projectsView.getItems().addAll(addProjects());

    }

    private List<Blower> addBlowers() {
        List<Blower> blowers = new ArrayList<Blower>();

        for (int i = 0; i<gui.numberOfBlowers; i++) {
            // todo, ziskat hodnoty zo servra
            final Hyperlink link = links[i] = new Hyperlink(("config " + i));
            link.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    webEngine.load(url);
                }
            });
            Blower blower = new Blower("1.2.3.4", ("id" + i), 0, 50, "project 1");
            blower.setLink(link);
            blowers.add(blower);
        }

        return blowers ;
    }

    private List<Project> addProjects() {
        List<Project> projects = new ArrayList<Project>();

        for (int i = 0; i<gui.numberOfProjects; i++) {
            // todo, ziskat hodnoty zo servra
            projects.add(new Project(("Project "+i), 0, "phase0"));
        }

        return projects ;
    }

    /**
     * Search XML file.
     *
     * @param actionEvent the action event
     */
    public void searchXML(ActionEvent actionEvent) {
        System.out.println("Search XML file button clicked");
        File file = fileChooser.showOpenDialog(gui.getStage());
        if (file != null) {
            filePath.setText(file.getPath());
            filePath2.setText(file.getPath());
        }
    }

    /**
     * Search path to EXE.
     *
     * @param actionEvent the action event
     */
    public void searchEXE(ActionEvent actionEvent) {
        System.out.println("Search EXE path button clicked");
        File file = fileChooser.showOpenDialog(gui.getStage());
        if (file != null) {
            settingsPath.setText(file.getPath());
        }
    }

    /**
     * Submit file.
     *
     * @param actionEvent the action event
     */
    public void submitFile(ActionEvent actionEvent) {
        try {
            System.out.println("Submit button clicked");
            // todo log mby
            XMLEditor.addPath(filePath.getText(), "A"); // todo path k .exe
            System.out.println("File successfully loaded");
            filePath.setText("");
            filePath2.setText("");

        } catch (IllegalArgumentException | ParserConfigurationException | IOException | SAXException | TransformerException e) {
            System.err.println("Error loading file");
            // todo log
//            Logger.getLogger( GUI.class.getName()).log( Level.SEVERE, null, e ); ?
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("ERROR");
            alert.setHeaderText("Error loading file");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
