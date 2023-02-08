package GUI;

import Logs.GeneralLogger;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static GUI.GUI.gui;
import static GUI.GUIController.setAlertIcons;
import static javafx.scene.layout.Region.USE_COMPUTED_SIZE;

/**
 * Class for blowers.
 */
public class Blower {
    private String IPAddress;
    private final SimpleStringProperty id;
    private final Hyperlink link;
    private final SimpleFloatProperty currentTemp;
    private final SimpleFloatProperty targetTemp;
    private final SimpleStringProperty projectName;
    private final SimpleBooleanProperty stopped;
    private final SimpleBooleanProperty markedForProject;

    private final CheckBox marker;
    private final Button stopButton;
    private final Button hiddenButton;

    private ObservableList<XYChart.Data<Number, Number>> currentData;
    private ObservableList<XYChart.Data<Number, Number>> targetData;


    /**
     * Instantiates a new Blower.
     *
     * @param IPAddress   the ip address
     * @param id          the id
     * @param currentTemp the current temperature
     * @param targetTemp  the target temperature
     * @param projectName the corresponding project
     * @param stopped     if the blower is stopped
     */
    public Blower(String IPAddress, String id, float currentTemp, float targetTemp, String projectName, Boolean stopped) {
        this.IPAddress = IPAddress.trim();
        this.id = new SimpleStringProperty(id);
        this.link = new Hyperlink();
        setLink();

        this.currentTemp = new SimpleFloatProperty(currentTemp);
        this.targetTemp = new SimpleFloatProperty(targetTemp);
        this.projectName = new SimpleStringProperty(projectName);
        this.stopped = new SimpleBooleanProperty(stopped);
        this.markedForProject = new SimpleBooleanProperty(false);

        this.marker = new CheckBox();
        this.marker.selectedProperty().addListener((observable, oldValue, newValue) -> markedForProject.setValue(newValue));
        this.currentData = FXCollections.observableArrayList();
        this.targetData = FXCollections.observableArrayList();
        this.hiddenButton = new Button("");
        setHiddenButton();
        this.stopButton = new Button("STOP");
        setStopButton();
    }

    private void setHiddenButton() {
        ImageView imageView = new ImageView(Objects.requireNonNull(getClass().getResource("caution.png")).toExternalForm());
        imageView.setFitWidth(25);
        imageView.setFitHeight(20);
        hiddenButton.setId("cautionBtn");
        hiddenButton.setVisible(isStopped());
        hiddenButton.setGraphic(imageView);
        hiddenButton.setStyle("-fx-background-color: transparent;");
        hiddenButton.setPrefWidth(25);
        hiddenButton.setPrefHeight(Region.USE_COMPUTED_SIZE);
        hiddenButton.setMinWidth(25);
        hiddenButton.setMaxHeight(Region.USE_COMPUTED_SIZE);
        hiddenButton.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setResizable(true);
            alert.setTitle("RESUMING BLOWER");
            alert.setHeaderText("Do you really want to resume blower " + getId() + "?");
            setAlertIcons(alert);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                try {
                    GUI.client.unlockController(getId());
                    hiddenButton.setVisible(false);
                    System.out.println("blower " + getId() + " was resumed");
                } catch (Exception e) {
                    GeneralLogger.writeExeption(e);
                    System.err.println("blower " + getId() + " could not be resumed");
                    gui.alert(e);
                }
            } else {
                System.out.println("blower " + getId() + " will not be resumed");
            }
        });
    }

    private void setStopButton() {
        stopButton.setId("stopBtn");
        stopButton.setFont(Font.font("Arial", FontWeight.BOLD, 11.0));
        stopButton.setMinWidth(75);
        stopButton.setPrefWidth(75);
        stopButton.setMaxWidth(USE_COMPUTED_SIZE);
        stopButton.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setResizable(true);
            alert.setTitle("STOPPING BLOWER");
            alert.setHeaderText("Do you really want to stop blower " + getId() + "?");
            setAlertIcons(alert);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                try {
                    GUI.client.stopAController(getId());
                    hiddenButton.setVisible(isStopped());
                    System.out.println("blower " + getId() + " stopped");
                } catch (Exception e) {
                    GeneralLogger.writeExeption(e);
                    System.err.println("blower " + getId() + " could not be stopped");
                    gui.alert(e);
                }
            } else {
                System.out.println("blower " + getId()+ " will not be stopped");
            }
        });
    }

//    TODO delete co netreba

    /**
     * Gets ip address of blower.
     *
     * @return ip address
     */
    public String getIPAddress() {
        return IPAddress;
    }

    /**
     * Sets ip address of blower.
     *
     * @param IPAddress the ip address
     */
    public void setIPAddress(String IPAddress) {
        this.IPAddress = IPAddress;
    }

    /**
     * Gets blower id.
     *
     * @return blower id
     */
    public String getId() {
        return id.get();
    }

    /**
     * Sets blower id.
     *
     * @param id blower id
     */
    public void setId(String id) {
        this.id.set(id);
    }

    /**
     * Gets link to blower's website.
     *
     * @return the link to blower's website
     */
    public Hyperlink getLink() {
        return link;
    }

    /**
     * Sets link to blower's website.
     *
     */
    public void setLink() {
        this.link.setText(getId());
        this.link.setOnAction(event -> {
            try {
                String url = "http://" + this.IPAddress;
                Desktop.getDesktop().browse(new URI(url));
                GeneralLogger.writeMessage(url);
            } catch (IOException | URISyntaxException e) {
                GeneralLogger.writeExeption(e);
                e.printStackTrace();
            }
        });
    }

    /**
     * Gets current temperature of blower.
     *
     * @return the current temperature of blower
     */
    public float getCurrentTemp() {
        return currentTemp.get();
    }

    /**
     * Sets current temperature of blower.
     *
     * @param currentTemp the current temperature of blower
     */
    public void setCurrentTemp(float currentTemp) {
        this.currentTemp.set(currentTemp);
    }

    /**
     * Gets target temperature of blower.
     *
     * @return the target temperature of blower
     */
    public float getTargetTemp() {
        return targetTemp.get();
    }

    /**
     * Sets target temperature of blower.
     *
     * @param targetTemp the target temperature of blower
     */
    public void setTargetTemp(float targetTemp) {
        this.targetTemp.set(targetTemp);
    }

    /**
     * Gets project name for blower.
     *
     * @return the project name for blower
     */
    public String getProjectName() {
        return projectName.get();
    }

    /**
     * Sets project name for blower.
     *
     * @param projectName the project name for blower
     */
    public void setProjectName(String projectName) {
        this.projectName.set(projectName);
    }

    /**
     * Gets if blower is stopped.
     *
     * @return if blower is stopped
     */
    public boolean isStopped() {
        return stopped.get();
    }

    /**
     * Sets if blower is stopped.
     *
     * @param stopped if blower is stopped
     */
    public void setStopped(boolean stopped) {
        this.stopped.set(stopped);
    }

    /**
     * Gets if blower is marked for project.
     *
     * @return if blower is marked for project
     */
    public boolean isMarkedForProject() {
        return markedForProject.get();
    }

    /**
     * Sets if blower is marked for project.
     *
     * @param markedForProject if blower is marked for project
     */
    public void setMarkedForProject(boolean markedForProject) {
        this.markedForProject.set(markedForProject);
    }

    /**
     * Gets checkbox for marking blower for project.
     *
     * @return the checkbox for marking blower for project
     */
    public CheckBox getMarker() {
        return marker;
    }

    public ObservableList<XYChart.Data<Number, Number>> getCurrentData() {
        return currentData;
    }

    public void setCurrentData(ObservableList<XYChart.Data<Number, Number>> currentData) {
        this.currentData = currentData;
    }

    public ObservableList<XYChart.Data<Number, Number>> getTargetData() {
        return targetData;
    }

    public void setTargetData(ObservableList<XYChart.Data<Number, Number>> targetData) {
        this.targetData = targetData;
    }

    /**
     * Gets stop button.
     *
     * @return the stop button
     */
    public Button getStopButton() {
        return stopButton;
    }

    /**
     * Gets caution button.
     *
     * @return the caution button, that is (by default) hidden
     */
    public Button getHiddenButton() {
        return hiddenButton;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Blower blower = (Blower) o;
        return Objects.equals(IPAddress, blower.IPAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(IPAddress);
    }

    @Override
    public String toString() {
        return "Blower{" +
                "id=" + id.getValue() +
                ", IPAddress='" + IPAddress + '\'' +
                ", currentTemp=" + currentTemp.getValue() +
                ", targetTemp=" + targetTemp.getValue() +
                ", projectName=" + projectName.getValue() +
                ", stopped=" + stopped.getValue() +
                '}';
    }

}
