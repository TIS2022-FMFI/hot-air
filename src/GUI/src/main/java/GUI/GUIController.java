package GUI;

import Communication.RequestResult;
import XML.XMLEditor;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    @FXML CheckBox  showCurrentPhase;
    @FXML CheckBox  showProjectStop;

    @FXML TableView<Blower> blowersView;
    @FXML TableColumn<Blower,Hyperlink> blowerID;
    @FXML TableColumn<Blower,String> blowerIP;
    @FXML TableColumn<Blower, Hyperlink> blowerCurrentTmp;
    @FXML TableColumn<Blower,Float> blowerTargetTmp;
    @FXML TableColumn<Blower,String> blowerProject;
    @FXML TableColumn<Blower, Button> blowerStop;
    @FXML TableColumn<Blower, Button> blowerCaution;

    @FXML TableView<Project> projectsView;
    @FXML TableColumn<Project,String> projectName;
    @FXML TableColumn<Project,String> projectPhase;
    @FXML TableColumn<Project,Button> projectStop;
    @FXML TableColumn<Project,Button> projectCaution;

    ObservableList<Blower> blowersList = FXCollections.observableArrayList();
    ObservableList<Project> projectsList = FXCollections.observableArrayList();

    public GUIController() {
        // todo na debug
//        numberOfBlowers = 10;
//        numberOfProjects = 2;
        try {
            numberOfBlowers = gui.client.getNumberOfControllers();
            numberOfProjects = gui.client.getNumberOfProjects();
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

        blowersView.setItems(blowersList);
        projectsView.setItems(projectsList);
        blowersList.addAll(addBlowers());
        projectsList.addAll(addProjects());

//        todo debug
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::updateTable, 0, 1, TimeUnit.SECONDS);

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
        blowerID.setCellValueFactory(new PropertyValueFactory<>("link"));
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
        blowerID.setComparator((o1, o2) -> o1.getText().compareTo(o2.getText()));
        blowerID.setSortType(TableColumn.SortType.ASCENDING);
        blowerID.setComparator((o2, o1) -> o2.getText().compareTo(o1.getText()));
        blowerID.setSortType(TableColumn.SortType.DESCENDING);
        blowerIP.setCellValueFactory(
                new PropertyValueFactory<>("IPAddress"));
        blowerIP.setComparator(new Comparator<String>() {
            @Override
            public int compare(String o1 , String o2) {
                String[] v1 = o1.split("\\.");
                String[] v2 = o2.split("\\.");
                for (int i=0; i<v1.length; i++) {
                    if (Integer.parseInt(v1[i]) < Integer.parseInt(v2[i]))
                        return -1;
                    if (Integer.parseInt(v1[i]) > Integer.parseInt(v2[i]))
                        return 1;
                }
                return 0;
            }
        });
        blowerCurrentTmp.setCellValueFactory(
                new PropertyValueFactory<>("graph"));
        blowerCurrentTmp.setCellFactory(new Callback<TableColumn<Blower, Hyperlink>, TableCell<Blower, Hyperlink>>() {
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
        blowerCurrentTmp.setComparator((o1, o2) -> Float.valueOf(o1.getText()).compareTo(Float.valueOf(o2.getText())));
        blowerCurrentTmp.setSortType(TableColumn.SortType.ASCENDING);
        blowerCurrentTmp.setComparator((o2, o1) -> Float.valueOf(o2.getText()).compareTo(Float.valueOf(o1.getText())));
        blowerCurrentTmp.setSortType(TableColumn.SortType.DESCENDING);

        blowerTargetTmp.setCellValueFactory(new PropertyValueFactory<>("targetTemp"));
        blowerProject.setCellValueFactory(new PropertyValueFactory<>("project"));
        blowerStop.setCellValueFactory(new PropertyValueFactory<>("stopButton"));
        blowerCaution.setCellValueFactory(new PropertyValueFactory<>("hiddenButton"));
    }

    private void setProjectsTable() {
        projectName.setCellValueFactory(new PropertyValueFactory<>("name"));
        projectPhase.setCellValueFactory(new PropertyValueFactory<>("currentPhase"));
        projectStop.setCellValueFactory(new PropertyValueFactory<>("stopButton"));
        projectCaution.setCellValueFactory(new PropertyValueFactory<>("hiddenButton"));
    }

    public void updateTable() {
        updateBlowers();
        updateProjects();

        blowersView.refresh();
        projectsView.refresh();

        System.out.println("tables updated");
    }

    public static void setAlertIcons(Alert alert) {
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(Objects.requireNonNull(GUIController.class.getResource("boge_icon.jpg")).toString()));
        ImageView icon = new ImageView(String.valueOf(GUI.class.getResource("question.png")));
        icon.setFitHeight(48);
        icon.setFitWidth(48);
        alert.getDialogPane().setGraphic(icon);
    }

    private List<Blower> addBlowers() {
        System.out.println("adding blowers");

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
            RequestResult.Controller[] controllers = gui.client.getAllControllers();
            for (RequestResult.Controller c : controllers) {
                String projectName = (c.getProjectName() == null) ? "" : c.getProjectName() ;
                if (projectName.contains("\\")) {
                    projectName = projectName.substring(projectName.lastIndexOf("\\")+1);
                }
                Blower blower = new Blower(c.getIP().getHostAddress(), c.getID(), c.getCurrentTemperature(), c.getTargetTemperature(), projectName);
                blowers.add(blower);
            }
            System.out.println("blowers were loaded successfully from server");
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
//            int phase= random.nextInt(5) + 1;
//            projects.add(new Project(("Project "+i), "phase" + phase));
//        }
//
//        return projects;
//    }

//    todo debug
    private Project[] addProjects() {
        System.out.println("adding projects");
        try {
            Project[] projects = gui.client.getAllProjects();
            System.out.println("projects were loaded successfully from server");
            return projects;
        }
        catch (Exception e) {
            // todo log
            System.err.println("projects were not loaded from server");
            Project[] projects = {};
            return projects;
        }
    }

    private List<Blower> updateBlowers() {
        System.out.println("updating blowers");

        List<Blower> blowers = new ArrayList<Blower>();

        try {
            RequestResult.Controller[] controllers = gui.client.getAllControllers();
            for (RequestResult.Controller c : controllers) {
                String projectName = (c.getProjectName() == null) ? "" : c.getProjectName() ;
                if (projectName.contains("\\")) {
                    projectName = projectName.substring(projectName.lastIndexOf("\\")+1);
                }
                Blower blower = new Blower(c.getIP().getHostAddress(), c.getID(), c.getCurrentTemperature(), c.getTargetTemperature(), projectName);
                blowers.add(blower);
            }
            System.out.println("blowery v ObservableList");
            blowersList.forEach(i -> System.out.println(i.toString()));
            System.out.println("blowery zo servera");
            for (Blower blower : blowers) {
                System.out.println(blower.toString());
                if (blowersList.contains(blower)) {
                    Blower b = blowersList.filtered(o -> o.equals(blower)).get(0);
                    b.setIdProperty(blower.idProperty());
                    b.setCurrentTempProperty(blower.currentTempProperty());
                    b.setTargetTempProperty(blower.targetTempProperty());
                    b.setProjectNameProperty(blower.projectNameProperty());
                    b.setGraph();
                } else {
                    blowersList.add(blower);
                }
            }
            System.out.println("blowers were updated successfully from server");
        } catch (Exception e) {
            // todo log
            System.err.println("blowers were not updated from server");
        }
        return blowers ;
    }

    private Project[] updateProjects() {
        System.out.println("updating projects");
        try {
            Project[] projects = gui.client.getAllProjects();
            for (Project project : projects) {
                if (projectsList.contains(project)) {
                    Project p = projectsList.filtered(o -> o.equals(project)).get(0);
                    p.setCurrentPhaseProperty(project.currentPhaseProperty());
                } else {
                    projectsList.add(project);
                }
            }
            System.out.println("projects were updated successfully from server");
            return projects;
        }
        catch (Exception e) {
            // todo log
            System.err.println("projects were not updated from server");
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
            gui.client.searchForNewControllers();  // todo debug
            updateTable();
            System.out.println("Search for new blowers was successful");
        } catch (Exception e) {
            System.err.println("Search for new blowers was not successful");
            gui.alert(e);
        }
    }

    public void stopAllBlowers(ActionEvent actionEvent) {
        try {
            gui.client.stopAllControllers();  // todo debug
            for (Blower b: blowersList) {
                b.getHiddenButton().setVisible(true);
            }
            System.out.println("blowers were stopped successfully");
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

    public void showCurrentPhase(ActionEvent actionEvent) {
        projectPhase.setVisible(showCurrentPhase.isSelected());
    }

    public void showProjectStop(ActionEvent actionEvent) {
        projectStop.setVisible(showProjectStop.isSelected());
    }
}
