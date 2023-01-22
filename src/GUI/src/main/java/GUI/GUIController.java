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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.xml.sax.SAXException;


import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 *  Gui controller.
 */
public class GUIController implements Initializable {

    final FileChooser fileChooser = new FileChooser();
    private GUI gui = GUI.gui;
    public static GUIController guiController;
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

    @FXML CheckBox  showID;
    @FXML CheckBox  showIP;
    @FXML CheckBox  showCurrentTmp;
    @FXML CheckBox  showTargetTmp;
    @FXML CheckBox  showProject;
    @FXML CheckBox showBlowerStop;

    @FXML CheckBox  showName;
    @FXML CheckBox  showTime;
    @FXML CheckBox  showCurrentPhase;
    @FXML CheckBox  showProjectStop;

    @FXML TableView<Blower> blowersView;
    @FXML TableColumn<Blower,Hyperlink> blowerID;
    @FXML TableColumn<Blower,String> blowerIP;
    @FXML TableColumn<Blower,Float> blowerCurrentTmp;
    @FXML TableColumn<Blower,Float> blowerTargetTmp;
    @FXML TableColumn<Blower,String> blowerProject;
    @FXML TableColumn<Blower, Blower> blowerStop;
    @FXML TableColumn<Blower, String> blowerButtonStopped;

    @FXML TableView<Project> projectsView;
    @FXML TableColumn<Project,String> projectName;
    @FXML TableColumn<Project,Float> projectTime;
    @FXML TableColumn<Project,String> projectPhase;
    @FXML TableColumn<Project,Project> projectStop;

    public ArrayList<ImageView> cautionImages = new ArrayList<>();

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
        guiController = this;

        setTextFields();
        setBlowersTable();
        setProjectsTable();

        updateTable();

//        blowersView.getColumns().forEach(column -> column.setMinWidth(30));

//        todo debug
//        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//        scheduler.scheduleAtFixedRate(this::updateTable, 0, 10, TimeUnit.SECONDS);

    }

    private void setTextFields() {
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
            System.err.println("An error occurred when loading settings.");
            System.err.println(e);
        }
    }

    private void setBlowersTable() {
        blowerID.setCellValueFactory(
                new PropertyValueFactory<>("link"));
        blowerID.setCellFactory(new Callback<TableColumn<Blower, Hyperlink>, TableCell<Blower, Hyperlink>>() {
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
        blowerIP.setCellValueFactory(
                new PropertyValueFactory<>("IPAddress"));
        blowerCurrentTmp.setCellValueFactory(
                new PropertyValueFactory<>("currentTemp"));
        blowerTargetTmp.setCellValueFactory(
                new PropertyValueFactory<>("targetTemp"));
        blowerProject.setCellValueFactory(
                new PropertyValueFactory<>("project"));
//        blowerButtonStopped.setCellValueFactory(
//                new PropertyValueFactory<>("project"));
        blowerButtonStopped.setCellFactory(
                column -> new CautionImage() {
                }
        );
        blowerStop.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue()));
        blowerStop.setCellFactory(column -> new TableCell<Blower, Blower>() {
            private Button button = new Button("STOP");
            {
                button.setId("stopBtn");
                button.setOnAction(a -> {
                    Blower b = getItem();
                    System.out.println(blowerButtonStopped.getCellFactory().toString());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setResizable(true);
                    alert.setTitle("STOPPING BLOWER");
                    alert.setHeaderText("Do you really want to stop blower " + b.getId() + "?");
                    setAlertIcons(alert);

                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.get() == ButtonType.OK){
                        try {
                            System.out.println("blower " + b.getId() + " stopped");
                            gui.client.stopAController(b.getId());  // todo debug
                        } catch (Exception e) {
                            System.err.println("blower " + b.getId() + " could not be stopped");
                            gui.alert(e);
                        }
                    } else {
                        System.out.println("blower " + b.getId() + " will not be stopped");
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
                    setGraphic(button);
                }
            }
        });
    }

    private void setProjectsTable() {
        projectName.setCellValueFactory(
            new PropertyValueFactory<>("name"));
        projectTime.setCellValueFactory(
            new PropertyValueFactory<>("time"));
        projectPhase.setCellValueFactory(
            new PropertyValueFactory<>("currentPhase"));
        projectStop.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue()));
        projectStop.setCellFactory(column -> new TableCell<Project, Project>() {
            private final Button button = new Button("STOP");
            {
                button.setId("stopBtn");
                button.setOnAction(a -> {
                    Project p = getItem();

                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("STOPPING PROJECT");
                    alert.setHeaderText("Do you really want to stop project " + p.getName() + "?");
                    setAlertIcons(alert);

                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.get() == ButtonType.OK){
                        try {
                            System.out.println("project " + p.getName() + " stopped");
                            // todo funkcia na stopnutie projektu
                        } catch (Exception e) {
                            System.err.println("project " + p.getName() + " could not be stopped");
                            gui.alert(e);
                        }
                    } else {
                        System.out.println("project " + p.getName() + " will not be stopped");
                    }
                });
                button.setFont(Font.font("Arial", FontWeight.BOLD, 11.0));
                button.setMinWidth(75);
                button.setPrefWidth(75);
                button.setMaxWidth(USE_COMPUTED_SIZE);
            }

            @Override
            protected void updateItem(Project item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(button);
                }
            }
        });
    }

    public void updateTable() {
        if (!blowersView.getItems().isEmpty())
            blowersView.getItems().clear();
        if (!projectsView.getItems().isEmpty())
            projectsView.getItems().clear();

        blowersView.getItems().addAll(addBlowers());
        projectsView.getItems().addAll(addProjects());
    }

    private void setAlertIcons(Alert alert) {
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResource("boge_icon.jpg")).toString()));
        ImageView icon = new ImageView(String.valueOf(GUI.class.getResource("question.png")));
        icon.setFitHeight(48);
        icon.setFitWidth(48);
        alert.getDialogPane().setGraphic(icon);
    }

    private List<Blower> addBlowers() {
        System.out.println("add blowers");

        List<Blower> blowers = new ArrayList<Blower>();
//        todo debug
//        Random random = new Random();
//        for (int i = 0; i<numberOfBlowers; i++) {
//            String ip1 = String.valueOf(random.nextInt(255));
//            String ip2 = String.valueOf(random.nextInt(255));
//            int temp1= random.nextInt(70);
//            int temp2= random.nextInt(70);
//            Blower blower = new Blower("192.165." + ip1 + "." + ip2, ("id" + i), temp1, temp2, "project 1");
//            blowers.add(blower);
//        }

//        todo debug
        try {
            for (int i = 0; i < gui.client.getNumberOfControllers() ; i++) {
                RequestResult.Controller[] controllers = gui.client.getAllControllers();
                for (RequestResult.Controller c : controllers) {
                    String projectName = (c.getProjectName() == null) ? "" : c.getProjectName() ; // todo project name z tagu v xml nie cesty !!
                    if (projectName.contains("\\")) {
                        projectName = projectName.substring(projectName.lastIndexOf("\\")+1);
                    }
                    Blower blower = new Blower(c.getIP().getHostAddress(), c.getID(), c.getCurrentTemperature(), c.getTargetTemperature(), projectName);
                    blowers.add(blower);
                }
            }
        } catch (Exception e) {
            // todo log
            System.err.println("blowers were not loaded from server");
        }

        return blowers ;

    }

//    todo debug
//    private List<Project> addProjects() {
//        List<Project> projects = new ArrayList<Project>();
//
//        Random random = new Random();
//
//        for (int i = 0; i<numberOfProjects; i++) {
//            int time= random.nextInt(200);
//            int phase= random.nextInt(5) + 1;
//            projects.add(new Project(("Project "+i), time, "phase" + phase));
//        }
//
//        return projects ;
//    }

//    todo debug
    private Project[] addProjects() {
        System.out.println("add projects");
        try {
//            todo project name z tagu v xml nie cesty !!
            Project[] projects = gui.client.getAllProjects();
            return projects;
        }
        catch (Exception e) {
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
        File file = fileChooser.showOpenDialog(gui.getStage());
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

            String originalPath = filePath.getText();
            String copiedPath = filePath.getText(0, originalPath.lastIndexOf(".")) + "_temp_control.xml";
            Path originalFile = Paths.get(originalPath);
            Path copiedFile = Paths.get(copiedPath);
            makeCopyOfXML(originalFile, copiedFile);

            XMLEditor.addPath(copiedPath, pathToExe.getText());
            System.out.println("File successfully loaded");

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("SUCCESSFUL LOADING");
            alert.setHeaderText("File " + originalFile.getFileName() + " successfully loaded");
            alert.setContentText("Saved as " + copiedFile.getFileName());
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResource("boge_icon.jpg")).toString()));

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
            alert.setResizable(true);
            alert.setTitle("ERROR LOADING");
            alert.setHeaderText("Error loading file");
            alert.setContentText(e.getMessage());
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResource("boge_icon.jpg")).toString()));

            ImageView icon = new ImageView(String.valueOf(GUI.class.getResource("error.png")));
            icon.setFitHeight(48);
            icon.setFitWidth(48);

            alert.getDialogPane().setGraphic(icon);
            alert.show();
        }
    }

    private void makeCopyOfXML(Path originalFile, Path copiedFile) throws IOException {
        try {
            Files.copy(originalFile, copiedFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw e;
        }
    }

    public void scanBlowers(ActionEvent actionEvent) {
        try {
            System.out.println("Search for new blowers was successful");
            gui.client.searchForNewControllers();  // todo debug
        } catch (Exception e) {
            System.err.println("Search for new blowers was not successful");
            gui.alert(e);
        }
    }

    public void stopAllBlowers(ActionEvent actionEvent) {
        try {
            System.out.println("blowers were stopped successfully");
            gui.client.stopAllControllers();  // todo debug
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
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResource("boge_icon.jpg")).toString()));

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

    public void showId(ActionEvent actionEvent) {
        blowerID.setVisible(showID.isSelected());
    }

    public void showIp(ActionEvent actionEvent) {
        blowerIP.setVisible(showIP.isSelected());
    }

    public void showCurrentTmp(ActionEvent actionEvent) {
        blowerCurrentTmp.setVisible(showCurrentTmp.isSelected());
    }

    public void showTargetTmp(ActionEvent actionEvent) {
        blowerTargetTmp.setVisible(showTargetTmp.isSelected());
    }

    public void showProject(ActionEvent actionEvent) {
        blowerProject.setVisible(showProject.isSelected());
    }

    public void showBlowerStop(ActionEvent actionEvent) {
        blowerStop.setVisible(showBlowerStop.isSelected());
    }

    public void showName(ActionEvent actionEvent) {
        projectName.setVisible(showName.isSelected());
    }

    public void showTime(ActionEvent actionEvent) {
        projectTime.setVisible(showTime.isSelected());
    }

    public void showCurrentPhase(ActionEvent actionEvent) {
        projectPhase.setVisible(showCurrentPhase.isSelected());
    }

    public void showProjectStop(ActionEvent actionEvent) {
        projectStop.setVisible(showProjectStop.isSelected());
    }
}
