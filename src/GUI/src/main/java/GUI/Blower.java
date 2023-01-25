package GUI;

import Logs.GeneralLogger;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
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
    private SimpleStringProperty id;
    private Hyperlink link;
    private String IPAddress;
    private SimpleFloatProperty currentTemp;
    private Hyperlink graph;
    private SimpleFloatProperty targetTemp;
    private SimpleStringProperty projectName;

    private final Button stopButton;
    private final Button hiddenButton;

    private XYChart.Series<Number, Number> currentSeries = new XYChart.Series<>();
    private XYChart.Series<Number, Number> targetSeries = new XYChart.Series<>();

    final NumberAxis xAxis = new NumberAxis();
    final NumberAxis yAxis = new NumberAxis();

    /**
     * Instantiates a new Blower.
     *
     * @param IPAddress   the ip address
     * @param id          the id
     * @param currentTemp the current temperature
     * @param targetTemp  the target temperature
     * @param projectName     the corresponding project
     */
    public Blower(String IPAddress, String id, float currentTemp, float targetTemp, String projectName) {
        this.IPAddress = IPAddress.trim();
        this.id = new SimpleStringProperty(id);
        this.currentTemp = new SimpleFloatProperty(currentTemp);
        this.targetTemp = new SimpleFloatProperty(targetTemp);
        this.projectName = new SimpleStringProperty(projectName);
        this.link = new Hyperlink(idProperty().getValue());
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
        ImageView imageView = new ImageView(Objects.requireNonNull(getClass().getResource("caution.png")).toExternalForm());
        imageView.setFitWidth(25);
        imageView.setFitHeight(20);
        this.hiddenButton = new Button("");
        hiddenButton.setId("cautionBtn");
        hiddenButton.setVisible(false);
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
            alert.setHeaderText("Do you really want to resume blower " + idProperty().getValue() + "?");
            setAlertIcons(alert);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                try {
                    GUI.gui.client.unlockController(idProperty().getValue());    // todo debug
                    hiddenButton.setVisible(false);
                    System.out.println("blower " + idProperty().getValue() + " was resumed");
                } catch (Exception e) {
                    GeneralLogger.writeExeption(e);
                    System.err.println("blower " + idProperty().getValue() + " could not be resumed");
                    gui.alert(e);
                }
            } else {
                System.out.println("blower " + idProperty().getValue() + " will not be resumed");
            }
        });
        this.stopButton = new Button("STOP");
        stopButton.setId("stopBtn");
        stopButton.setFont(Font.font("Arial", FontWeight.BOLD, 11.0));
        stopButton.setMinWidth(75);
        stopButton.setPrefWidth(75);
        stopButton.setMaxWidth(USE_COMPUTED_SIZE);
        stopButton.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setResizable(true);
            alert.setTitle("STOPPING BLOWER");
            alert.setHeaderText("Do you really want to stop blower " + idProperty().getValue() + "?");
            setAlertIcons(alert);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                try {
                    gui.client.stopAController(idProperty().getValue());  // todo debug
                    hiddenButton.setVisible(true);
                    System.out.println("blower " + idProperty().getValue() + " stopped");
                } catch (Exception e) {
                    GeneralLogger.writeExeption(e);
                    System.err.println("blower " + idProperty().getValue() + " could not be stopped");
                    gui.alert(e);
                }
            } else {
                System.out.println("blower " + idProperty().getValue() + " will not be stopped");
            }
        });

    }

    /**
     * Gets blower id.
     *
     * @return blower id
     */
    public SimpleStringProperty idProperty() {
        return id;
    }

    /**
     * Sets blower id.
     *
     * @param id blower id
     */
    public void setIdProperty(SimpleStringProperty id) {
        this.id = id;
    }

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
     * Gets current temperature of blower.
     *
     * @return the current temperature of blower
     */
    public SimpleFloatProperty currentTempProperty() {
        return currentTemp;
    }

    /**
     * Sets current temperature of blower.
     *
     * @param currentTemp the current temperature of blower
     */
    public void setCurrentTempProperty(SimpleFloatProperty currentTemp) {
        this.currentTemp = currentTemp;
    }

    /**
     * Gets current link to graph.
     *
     * @return the link to graph
     */
    public Hyperlink getGraph() {
        return graph;
    }

    /**
     * Sets link to open the graph.
     *
     * @param graph the graph
     */
    public void setGraph(Hyperlink graph) {
        this.graph.setText(graph.getText());
    }

    /**
     * Gets target temperature of blower.
     *
     * @return the target temperature of blower
     */
    public SimpleFloatProperty targetTempProperty() {
        return targetTemp;
    }

    /**
     * Sets target temperature of blower.
     *
     * @param targetTemp the target temperature of blower
     */
    public void setTargetTempProperty(SimpleFloatProperty targetTemp) {
        this.targetTemp = targetTemp;
    }

    /**
     * Gets project name for blower.
     *
     * @return the project name for blower
     */
    public SimpleStringProperty projectNameProperty() {
        return projectName;
    }

    /**
     * Sets project name for blower.
     *
     * @param projectName the project name for blower
     */
    public void setProjectNameProperty(SimpleStringProperty projectName) {
        this.projectName = projectName;
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
     * @param link the link to blower's website
     */
    public void setLink(Hyperlink link) {
        this.link = link;
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

    public XYChart.Series<Number, Number> getCurrentSeries() {
        return currentSeries;
    }

    public void setCurrentSeries(XYChart.Series<Number, Number> currentSeries) {
        this.currentSeries = currentSeries;
    }

    public XYChart.Series<Number, Number> getTargetSeries() {
        return targetSeries;
    }

    public void setTargetSeries(XYChart.Series<Number, Number> targetSeries) {
        this.targetSeries = targetSeries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Blower blower = (Blower) o;
        return Objects.equals(IPAddress, blower.IPAddress);
    }

    public boolean equalsEverything(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Blower blower = (Blower) o;
        return Objects.equals(idProperty().getValue(), blower.idProperty().getValue())
                && Objects.equals(IPAddress, blower.IPAddress)
                && Objects.equals(currentTempProperty().getValue(), blower.currentTempProperty().getValue())
                && Objects.equals(targetTempProperty().getValue(), blower.targetTempProperty().getValue())
                && Objects.equals(projectNameProperty().getValue(), blower.projectNameProperty().getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(IPAddress);
    }

    @Override
    public String toString() {
        return "Blower{" +
                "id=" + id +
                ", IPAddress='" + IPAddress + '\'' +
                ", currentTemp=" + currentTemp +
                ", targetTemp=" + targetTemp +
                ", projectName=" + projectName +
                '}';
    }

}
