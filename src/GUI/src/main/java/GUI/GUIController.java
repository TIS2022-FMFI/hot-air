package GUI;

import Communication.RequestResult;
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
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.xml.sax.SAXException;


import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;


/**
 *  Gui controller.
 */
public class GUIController implements Initializable {

    final FileChooser fileChooser = new FileChooser();
    final DirectoryChooser directoryChooser = new DirectoryChooser();
    private GUI gui = GUI.gui;
    private int numberOfBlowers = 0;
    private int numberOfProjects = 0;

    @FXML TextField filePath;
    @FXML TextField filePath2;
    @FXML TextField pathToExe;
    @FXML TextField portToServer;
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
        try {
            numberOfBlowers = gui.client.getNumberOfControllers();
            numberOfProjects = gui.client.getNumberOfProjects();
//            // todo na debug
//            numberOfBlowers = 10;
//            numberOfProjects = 2;
        } catch (IOException | InterruptedException e) {
            gui.alert(e);
        }

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        filePath.setTooltip(new Tooltip("Enter path to xml file or search the file by clicking the search button."));
        filePath2.setTooltip(new Tooltip("Enter path to xml file or search the file by clicking the search button."));
        pathToExe.setTooltip(new Tooltip("Enter default path to store exe file."));
        pathToExe.setPromptText("Enter default path to store exe file.");
        portToServer.setTooltip(new Tooltip("Enter port to communicate with server. [default 4002]"));
        portToServer.setPromptText("Enter port to communicate with server. [default 4002]");

        portToServer.getProperties().put("vkType", "numeric");
        portToServer.setTextFormatter(new TextFormatter<>(c -> {
            portInfo.setText("");
            if (c.isContentChange()) {
                if (c.getControlNewText().length() == 0) {
                    return c;
                }
                try {
                    Integer.parseInt(c.getControlNewText());
                    if (c.getControlNewText().length() > 5 || c.getControlNewText().length() == 5 && c.getControlNewText().compareTo("65535") > 0 ) {
                        portInfo.setText("wrong format: wrong port, choose from <1, 65535>");
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

        try {
            File config = new File("GUIconfig.txt");
            BufferedReader reader = new BufferedReader(new FileReader(config));
            String path = reader.readLine();
            String port = reader.readLine();
            if (path != null && !path.isEmpty()) pathToExe.setText(path);
            if (port != null && !port.isEmpty()) portToServer.setText(port);
            reader.close();

        } catch (IOException e) {
            System.err.println("An error occurred.");
            e.printStackTrace();
        }

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
                        System.out.println("blower " + b.getId() + " stopped");
                        gui.client.stopAController(b.getId());
                    } catch (Exception e) {
                        System.err.println("blower " + b.getId() + " could not be stopped");
                        gui.alert(e);
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
            try {
                RequestResult.Controller[] controllers = gui.client.getAllControllers();
                for (RequestResult.Controller c : controllers) {
                    String projectName = c.getProjectName();
                    if (projectName.contains("\\")) {
                        projectName = projectName.substring(projectName.lastIndexOf("\\")+1);
                    }
                    Blower blower = new Blower(c.getIP().getHostAddress(), c.getID(), c.getCurrentTemperature(), c.getTargetTemperature(), projectName);
                    blowers.add(blower);
                }
            }
            catch (IOException | InterruptedException e) {
                // todo log
                System.err.println("blowers were not loaded from server");
                return blowers ;
            }
        }

        return blowers ;
    }

    private Project[] addProjects() {
        try {
            Project[] projects = gui.client.getAllProjects();
            return projects;
        }
        catch (IOException | InterruptedException e) {
            // todo log
            System.err.println("projects were not loaded from server");
            Project[] projects = {};
            return projects;
        }
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
        blowersInfo.setText("");
        projectsInfo.setText("");
        if (!path.substring(path.lastIndexOf('.')).equals(".xml")) {
            blowersInfo.setText("type of chosen file is not .xml");
            projectsInfo.setText("type of chosen file is not .xml");
        }
    }

    /**
     * Search / Select directory for EXE.
     *
     * @param actionEvent the action event
     */
    public void searchEXE(ActionEvent actionEvent) {
        System.out.println("Search EXE path button clicked");
        File file = directoryChooser.showDialog(gui.getStage());
        if (file != null) {
            pathToExe.setText(file.getPath());
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

            if (pathToExe.getText().isEmpty()) {
                XMLLoadException exception = new XMLLoadException("File can't be loaded. Set path to EXE in settings first!");
                throw exception;
            }

            XMLEditor.addPath(filePath.getText(), pathToExe.getText());
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

        } catch (IllegalArgumentException | ParserConfigurationException | IOException | SAXException | TransformerException | XMLLoadException e) {
            System.err.println("Error loading file");
            // todo zapisat do logov
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
            System.out.println("Search for new blowers was successful");
            gui.client.searchForNewControllers();
        } catch (Exception e) {
            System.err.println("Search for new blowers was not successful");
            gui.alert(e);
        }
    }

    public void stopAllBlowers(ActionEvent actionEvent) {
        try {
            System.out.println("blowers were stopped successfully");
            gui.client.stopAllControllers();
        } catch (Exception e) {
            System.err.println("blowers could not be stopped");
            gui.alert(e);
        }
    }

    public void saveSettings(ActionEvent actionEvent) {
        try {
            FileWriter writer = new FileWriter("GUIconfig.txt");
            writer.write(pathToExe.getText());
            writer.write("\n");
            writer.write(portToServer.getText());
            writer.close();

            System.out.println("settings were successfully saved");
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("SUCCESSFULLY SAVED");
            alert.setHeaderText("Settings were successfully saved");

            ImageView icon = new ImageView(String.valueOf(GUI.class.getResource("success.png")));
            icon.setFitHeight(48);
            icon.setFitWidth(48);

            alert.getDialogPane().setGraphic(icon);
            alert.show();

        } catch (IOException e) {
            System.err.println("settings were not saved");
            e.printStackTrace();
            gui.alert(e);
        }
    }
}
