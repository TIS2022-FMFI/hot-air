package GUI;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;


import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/* todo
     - STOP buttony na duchadla
     -
     -
 */
public class GUIController implements Initializable {

    private Desktop desktop = Desktop.getDesktop();
    final FileChooser fileChooser = new FileChooser();
    private GUI gui = new GUI();

    final WebView browser = new WebView();
    final WebEngine webEngine = browser.getEngine();
    final Hyperlink[] links = new Hyperlink[gui.numberOfBlowers];
    final static String url = "https://www.google.sk/";
//    final static String[] urls = new String[]{
//            "http://www.oracle.com/us/products/index.html",
//            "http://education.oracle.com/",
//            "http://www.oracle.com/partners/index.html",
//            "http://www.oracle.com/us/support/index.html"
//    };

    @FXML TextField filePath;
    @FXML TextField filePath2;

    @FXML BorderPane blowers;
    @FXML BorderPane projects;
    
    @FXML TableView<Blower> blowersView;
    @FXML TableColumn<Blower,String> blower_id;
    @FXML TableColumn<Blower,String> blower_ip_address;
    @FXML TableColumn<Blower,Float> blower_current_tmp;
    @FXML TableColumn<Blower,Float> blower_target_tmp;
    @FXML TableColumn<Blower,String> blower_project;
    @FXML TableColumn<Blower,Hyperlink> blower_config;

    @FXML TableView<Project> projectsView;
    @FXML TableColumn<Project,String> project_name;
    @FXML TableColumn<Project,Float> project_time;
    @FXML TableColumn<Project,String> project_phase;

    
    @Override
    public void initialize(URL location, ResourceBundle resources) {

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
            blowers.add(new Blower("1.2.3.4", ("id" + i), 0, 50, "project 1", link));
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

    public void chooseFile(ActionEvent actionEvent) {
        System.out.println("Button clicked");
        File file = fileChooser.showOpenDialog(gui.getStage());
        if (file != null) {
            filePath.setText(file.getPath());
            filePath2.setText(file.getPath());
        }
    }

    public void loadFile(ActionEvent actionEvent) {
        try {
            File file = new File(filePath.getText());
            desktop.open(file);
            System.out.println("File successfully opened");

            filePath.setText("");
            filePath2.setText("");
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
