package GUI;

import Communication.RequestResult;
import Logs.GeneralLogger;
import XML.XMLEditor;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
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
    private final GUI gui = GUI.gui;
    public static GUIController guiController;

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
    @FXML CheckBox  showBlowerStop;

    @FXML CheckBox  showName;
    @FXML CheckBox  showCurrentPhase;

    @FXML TableView<Blower> blowersView;
    @FXML TableColumn<Blower,CheckBox> blowerMark;
    @FXML TableColumn<Blower,Hyperlink> blowerID;
    @FXML TableColumn<Blower,String> blowerIP;
    @FXML TableColumn<Blower, Float> blowerCurrentTmp;
    @FXML TableColumn<Blower,Float> blowerTargetTmp;
    @FXML TableColumn<Blower,String> blowerProject;
    @FXML TableColumn<Blower, Button> blowerStop;
    @FXML TableColumn<Blower, Button> blowerCaution;

    @FXML TableView<Project> projectsView;
    @FXML TableColumn<Project, Hyperlink> projectName;
    @FXML TableColumn<Project,String> projectPhase;
    @FXML TableColumn<Project,String> projectStatus;
    @FXML TableColumn<Project, Button> projectStop;

    public static final ObservableList<Blower> blowersList = FXCollections.observableArrayList();
    public ObservableList<Project> projectsList = FXCollections.observableArrayList();

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
            GeneralLogger.writeExeption(e);
            System.err.println("An error occurred when loading settings.");
            System.err.println(e);
        }
    }

    private void setBlowersTable() {
        blowerMark.setCellValueFactory(new PropertyValueFactory<>("marker"));
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
        blowerID.setComparator(Comparator.comparing(Labeled::getText));
        blowerIP.setCellValueFactory(new PropertyValueFactory<>("IPAddress"));
        blowerIP.setComparator((o1, o2) -> {
            String[] v1 = o1.split("\\.");
            String[] v2 = o2.split("\\.");
            for (int i=0; i<v1.length; i++) {
                if (Integer.parseInt(v1[i]) < Integer.parseInt(v2[i]))
                    return -1;
                if (Integer.parseInt(v1[i]) > Integer.parseInt(v2[i]))
                    return 1;
            }
            return 0;
        });
        blowerCurrentTmp.setCellValueFactory(new PropertyValueFactory<>("currentTemp"));
        blowerTargetTmp.setCellValueFactory(new PropertyValueFactory<>("targetTemp"));
        blowerProject.setCellValueFactory(new PropertyValueFactory<>("projectName"));
        blowerStop.setCellValueFactory(new PropertyValueFactory<>("stopButton"));
        blowerCaution.setCellValueFactory(new PropertyValueFactory<>("hiddenButton"));
    }

    private void setProjectsTable() {
        projectName.setCellValueFactory(new PropertyValueFactory<>("graph"));
        projectName.setCellFactory(new Callback<TableColumn<Project, Hyperlink>, TableCell<Project, Hyperlink>>() {
            @Override
            public TableCell<Project, Hyperlink> call(TableColumn<Project, Hyperlink> param) {
                return new TableCell<Project, Hyperlink>() {
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
        projectName.setComparator(Comparator.comparing(o -> Float.valueOf(o.getText())));
        projectStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        projectPhase.setCellValueFactory(new PropertyValueFactory<>("currentPhase"));
        projectStop.setCellValueFactory(new PropertyValueFactory<>("stopButton"));
    }

    public void updateTable() {
        updateBlowers();
        updateProjects();

        blowersView.refresh();
        projectsView.refresh();
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
        List<Blower> blowers = new ArrayList<>();
        try {
            RequestResult.Controller[] controllers = GUI.client.getAllControllers();
            for (RequestResult.Controller c : controllers) {
                String projectName = (c.getProjectName() == null) ? "" : c.getProjectName() ;
                if (projectName.contains("\\")) {
                    projectName = projectName.substring(projectName.lastIndexOf("\\")+1);
                }
                Blower blower = new Blower(c.getIP().getHostAddress(), c.getID(), c.getCurrentTemperature(), c.getTargetTemperature(), projectName, c.getStopped());
                blowers.add(blower);
            }
        } catch (Exception e) {
            GeneralLogger.writeExeption(e);
            System.err.println(e);
            e.printStackTrace();
        }

        return blowers ;
    }

    private Project[] addProjects() {
        try {
            return GUI.client.getAllProjects();
        } catch (Exception e) {
            GeneralLogger.writeExeption(e);
            System.err.println(e);
            e.printStackTrace();
            Project[] projects = {};
            return projects;
        }
    }

    private void updateBlowers() {
        try {
            List<Blower> blowers = new ArrayList<>();
            RequestResult.Controller[] controllers = GUI.client.getAllControllers();
            for (RequestResult.Controller c : controllers) {
                String projectName = (c.getProjectName() == null) ? "" : c.getProjectName();
                Blower blower = new Blower(c.getIP().getHostAddress(), c.getID(), c.getCurrentTemperature(), c.getTargetTemperature(), projectName, c.getStopped());
                blowers.add(blower);
            }

//            System.out.println("\nblowery v ObservableList= " + blowersList.size());
//            blowersList.forEach(i -> System.out.println(i.toString()));
            System.out.println("blowery zo servera= " + blowers.size());
            blowers.forEach(a -> System.out.println(a.toString()));
            for (Blower blower : blowers) {
                synchronized (blower) {
                    boolean gut = false;
                    for (Blower b : blowersList) {
                        if (b.equals(blower)) {
                            b.setId(blower.getId());
                            b.setLink();
                            b.setCurrentTemp(blower.getCurrentTemp());
                            b.setTargetTemp(blower.getTargetTemp());
                            b.setProjectName(blower.getProjectName());
                            b.setStopped(blower.isStopped());
                            b.setMarkedForProject(blower.isMarkedForProject());
                            gut = true;
                        }
                    }
                    if (!gut) {
                        blowersList.add(blower);
                    }
                }
            }

            List<Blower> blowersToRemove = new ArrayList<>();
            for (Blower b : blowersList) {
                boolean gut = false;
                for (Blower blower : blowers) {
                    if (b.equals(blower)) {
                        gut = true;
                    }
                }
                if (!gut) {
                    blowersToRemove.add(b);
                }
            }
            blowersToRemove.forEach(blowersList::remove);
        } catch (Exception e) {
            GeneralLogger.writeExeption(e);
            System.err.println(e);
            e.printStackTrace();
        }
    }

    private void updateProjects() {
        try {
//            System.out.println("\n projects v ObservableList= " + projectsList.size());
//            Arrays.asList(projectsList).forEach(i -> System.out.println(i.toString()));
            Project[] projects = GUI.client.getAllProjects();
            System.out.println("projects zo servera= " + projects.length);
            Arrays.asList(projects).forEach(a -> System.out.println(a.toString()));
            if (projects.length == 0) {
                projectsList.clear();
                return;
            }

            for (Project project : projects) {
                synchronized (project) {
                    boolean gut = false;
                    for (Project p : projectsList) {
                        if (p.equals(project)) {
                            p.setStatus(project.getStatus());
                            p.setCurrentPhase(project.getCurrentPhase());
                            gut = true;
                        }
                    }
                    if (!gut) {
                        projectsList.add(project);
                    }
                }
            }

//            List<Project> projectsToRemove = new ArrayList<>();
//            for (Project p : projectsList) {
//                boolean gut = false;
//                for (Project project : projects) {
//                    if (p.equals(project)) {
//                        gut = true;
//                    }
//                }
//                if (!gut) {
//                    projectsToRemove.add(p);
//                }
//            }
//            projectsToRemove.forEach(projectsList::remove);
        } catch (Exception e) {
            GeneralLogger.writeExeption(e);
            System.err.println(e);
            e.printStackTrace();
        }
    }

    /**
     * Search XML file.
     *
     */
    public void searchXML() {
        blowersInfo.setText("");
        projectsInfo.setText("");

        File file = fileChooser.showOpenDialog(gui.getStage());
        if (file != null) {
            String path = file.getPath();
            filePath.setText(path);
            filePath2.setText(path);
            checkType(path);
        }
    }

    public void checkTypeOfFile() {
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
     */
    public void searchEXE() {
        File file = fileChooser.showOpenDialog(gui.getStage());
        if (file != null) {
            pathToExe.setText(file.getPath());
        }
    }

    /**
     * Submit file.
     *
     */
    public void submitFile() {
        try {
            if (pathToExe.getText().isEmpty()) {
                throw new XMLLoadException("File can't be loaded. Set path to EXE in settings first!");
            }

            String originalPath = filePath.getText();
            String copiedPath = filePath.getText(0, originalPath.lastIndexOf(".")) + "_temp_control.xml";
            Path originalFile = Paths.get(originalPath);
            Path copiedFile = Paths.get(copiedPath);
            makeCopyOfXML(originalFile, copiedFile);

            XMLEditor.addPath(copiedPath, pathToExe.getText());
            GeneralLogger.writeMessage("XML file successfully loaded");

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
            blowersList.forEach(b -> b.setMarkedForProject(false));
            blowersList.forEach(b -> b.getMarker().setSelected(false));

        } catch (IllegalArgumentException | ParserConfigurationException | IOException | SAXException | TransformerException | XMLLoadException e) {
            GeneralLogger.writeExeption(e);
            System.err.println("Error loading file");
            System.err.println(e);
            e.printStackTrace();

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

    public void scanBlowers() {
        try {
            GUI.client.searchForNewControllers();
            updateTable();
            GeneralLogger.writeMessage("Search for new blowers was successful");
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
            gui.alert(e);
        }
    }

    public void stopAllBlowers() {
        try {
            GUI.client.stopAllControllers();
            for (Blower b: blowersList) {
                b.getHiddenButton().setVisible(true);
            }
            GeneralLogger.writeMessage("all blowers were stopped successfully (by GUI)");
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
            gui.alert(e);
        }
    }

    public void saveSettings() {
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

    public void showId() {
        blowerID.setVisible(showID.isSelected());
    }

    public void showIp() {
        blowerIP.setVisible(showIP.isSelected());
    }

    public void showCurrentTmp() {
        blowerCurrentTmp.setVisible(showCurrentTmp.isSelected());
    }

    public void showTargetTmp() {
        blowerTargetTmp.setVisible(showTargetTmp.isSelected());
    }

    public void showProject() {
        blowerProject.setVisible(showProject.isSelected());
    }

    public void showBlowerStop() {
        blowerStop.setVisible(showBlowerStop.isSelected());
    }

    public void showName() {
        projectName.setVisible(showName.isSelected());
    }

    public void showCurrentPhase() {
        projectPhase.setVisible(showCurrentPhase.isSelected());
    }

    public static ObservableList<Blower> getBlowersList() {
        return blowersList;
    }

    public ObservableList<Project> getProjectsList() {
        return projectsList;
    }
}
