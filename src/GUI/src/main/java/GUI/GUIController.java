package GUI;

import XML.XMLEditor;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.util.Callback;
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


/**
 *  Gui controller.
 */
public class GUIController implements Initializable {

    private Desktop desktop = Desktop.getDesktop();
    final FileChooser fileChooser = new FileChooser();
    private GUI gui = GUI.gui;
    private int numberOfBlowers = 0;
    private int numberOfProjects = 0;

    final WebView browser = new WebView();
    final WebEngine webEngine = browser.getEngine();
    final Hyperlink[] links;
    final static String url = "https://www.google.sk/";

    @FXML TextField filePath;
    @FXML TextField filePath2;
    @FXML TextField settingsPath;
    @FXML TextField settingsPort;
    @FXML Text blowersInfo;
    @FXML Text projectsInfo;
    @FXML Text portInfo;

    @FXML BorderPane blowers;
    @FXML BorderPane projects;
    
    @FXML TableView<Blower> blowersView;
    @FXML TableColumn<Blower,String> blower_id;
    @FXML TableColumn<Blower,String> blower_ip_address;
    @FXML TableColumn<Blower,Float> blower_current_tmp;
    @FXML TableColumn<Blower,Float> blower_target_tmp;
    @FXML TableColumn<Blower,String> blower_project;
    @FXML TableColumn<Blower,Hyperlink> blower_config;
    @FXML TableColumn<Blower, Blower> blower_stop;

    @FXML TableView<Project> projectsView;
    @FXML TableColumn<Project,String> project_name;
    @FXML TableColumn<Project,Float> project_time;
    @FXML TableColumn<Project,String> project_phase;

    public GUIController() {
        // todo debug
//        try {
//            numberOfBlowers = gui.client.getNumberOfControllers();
//            numberOfProjects = gui.client.getNumberOfProjects();
            numberOfBlowers = 10;
            numberOfProjects = 2;
            links = new Hyperlink[numberOfBlowers];
//        } catch (IOException | InterruptedException e) {
//            gui.alert(e);
//        }

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        filePath.setTooltip(new Tooltip("Enter path to xml file or search the file by clicking the search button."));
        filePath2.setTooltip(new Tooltip("Enter path to xml file or search the file by clicking the search button."));
        settingsPath.setTooltip(new Tooltip("Enter default path to store exe file."));
        settingsPath.setPromptText("Enter default path to store exe file.");
        settingsPort.setTooltip(new Tooltip("Enter port to communicate with server. [default 4002]"));
        settingsPort.setPromptText("Enter port to communicate with server. [default 4002]");

        settingsPort.getProperties().put("vkType", "numeric");
        settingsPort.setTextFormatter(new TextFormatter<>(c -> {
            portInfo.setText("");
            if (c.isContentChange()) {
                if (c.getControlNewText().length() == 0) {
                    return c;
                }
                try {
                    Integer.parseInt(c.getControlNewText());
                    if (c.getControlNewText().length() > 5) {
                        portInfo.setText("wrong format: wrong port");
                        return null;
                    }
                    return c;
                } catch (NumberFormatException e) {
                    portInfo.setText("wrong format: enter numbers only");
                }
                return null;

            }
            return c;
        }));

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
        blower_config.setCellFactory(new Callback<TableColumn<Blower, Hyperlink>, TableCell<Blower, Hyperlink>>() {
            @Override
            public TableCell<Blower, Hyperlink> call(TableColumn<Blower, Hyperlink> param) {
                return new TableCell<Blower, Hyperlink>() {
                    @Override
                    protected void updateItem(Hyperlink item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(item);
                        }
                    }
                };
            }
        });
        blower_config.setCellValueFactory(
            new PropertyValueFactory<>("link"));
        blower_stop.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue()));
        blower_stop.setCellFactory(column -> new TableCell<Blower, Blower>() {
            private final Button button = new Button("STOP");
            {
                button.setId("stopBtn");
                button.setOnAction(a -> {
                    Blower b = getItem();
                    try {
                        gui.client.stopAController(b.getId());
                        System.out.println("blower " + b.getId() + " stopped");
                    } catch (Exception e) {
                        gui.alert(e);
                        System.err.println("blower " + b.getId() + " could not be stopped");
                    }
                });
                button.setFont(Font.font("Arial", FontWeight.BOLD, 11.0));
                button.setMinWidth(75);
                button.setPrefWidth(75);
                button.setMaxWidth(USE_COMPUTED_SIZE);
            }

            @Override
            protected void updateItem(Blower item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(button);
                }
            }
        });

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

        for (int i = 0; i<numberOfBlowers; i++) {
            Blower blower = new Blower("1.2.3.4", ("id" + i), 0, 50, "project 1");
            blowers.add(blower);
        }

        return blowers ;
    }

    private List<Project> addProjects() {
        List<Project> projects = new ArrayList<Project>();

        for (int i = 0; i<numberOfProjects; i++) {
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
        blowersInfo.setText("");
        projectsInfo.setText("");


        System.out.println("Search XML file button clicked");
        File file = fileChooser.showOpenDialog(gui.getStage());
        if (file != null) {
            String path = file.getPath();

            filePath.setText(path);
            filePath2.setText(path);
            checkType(path);
        }
    }

    public void checkTypeOfFile(ActionEvent actionEvent) {
        String path = filePath.getText();
        checkType(path);
    }

    private void checkType(String path) {
//        System.out.println(path.substring(path.lastIndexOf('.')));
        blowersInfo.setText("");
        projectsInfo.setText("");
        if (!path.substring(path.lastIndexOf('.')).equals(".xml")) {
            blowersInfo.setText("type of chosen file is not .xml");
            projectsInfo.setText("type of chosen file is not .xml");
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
            // todo zapisat do logov

            XMLEditor.addPath(filePath.getText(), "A"); // todo path k .exe
            System.out.println("File successfully loaded");

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("SUCCESSFUL LOADING");
            alert.setHeaderText("File successfully loaded");

            ImageView icon = new ImageView(String.valueOf(GUI.class.getResource("success.png")));
            icon.setFitHeight(48);
            icon.setFitWidth(48);

            alert.getDialogPane().setGraphic(icon);
            alert.show();

            filePath.setText("");
            filePath2.setText("");

        } catch (IllegalArgumentException | ParserConfigurationException | IOException | SAXException | TransformerException e) {
            System.err.println("Error loading file");
            // todo zapisat do logov
//            Logger.getLogger( GUI.class.getName()).log( Level.SEVERE, null, e ); ?
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("ERROR LOADING");
            alert.setHeaderText("Error loading file");
            alert.setContentText(e.getMessage());

            ImageView icon = new ImageView(String.valueOf(GUI.class.getResource("error.png")));
            icon.setFitHeight(48);
            icon.setFitWidth(48);

            alert.getDialogPane().setGraphic(icon);
            alert.show();
        }
    }

    public void scanBlowers(ActionEvent actionEvent) {
        try {
            gui.client.searchForNewControllers();
            System.out.println("Search for new blowers was successful");
        } catch (Exception e) {
            gui.alert(e);
            System.err.println("Search for new blowers was not successful");
        }
    }

    public void stopAllBlowers(ActionEvent actionEvent) {
        try {
            gui.client.stopAllControllers();
            System.out.println("blowers were stopped successfully");
        } catch (Exception e) {
            gui.alert(e);
            System.err.println("blowers could not be stopped");
        }
    }

    public void saveSettings(ActionEvent actionEvent) { // todo, asi sa to posle na server a pri startovani gui sa zisti ci neni na serveri nieco ulozene uz?
        System.out.println("Save settings button clicked");
    }
}
