package GUI;

import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static GUI.GUI.gui;
import static GUI.GUIController.setAlertIcons;
import static javafx.scene.layout.Region.USE_COMPUTED_SIZE;

/**
 * Class for blowers.
 */
public class Blower {
    private SimpleStringProperty id;  // do 15 znakov, ascii,
    private String IPAddress;
    private SimpleFloatProperty currentTemp;
    private Hyperlink graph;
    private SimpleFloatProperty targetTemp;
    private SimpleStringProperty projectName;
    private Hyperlink link;

    private final Button stopButton;
    private final Button hiddenButton;

    private int count = 0;

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
        this.IPAddress = IPAddress;
        this.id = new SimpleStringProperty(id);
        this.currentTemp = new SimpleFloatProperty(currentTemp);
        setGraph();
        this.targetTemp = new SimpleFloatProperty(targetTemp);;
        this.projectName = new SimpleStringProperty(projectName);
        this.link = new Hyperlink(idProperty().getValue());
        this.link.setOnAction(event -> {
            try {
                String url = "http://" + IPAddress;
                System.out.println(url);
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                // todo zapisat do logov
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
                    System.err.println("blower " + idProperty().getValue() + " could not be stopped");
                    gui.alert(e);
                }
            } else {
                System.out.println("blower " + idProperty().getValue() + " will not be stopped");
            }
        });
    }

    /**
     * Gets id.
     *
     * @return the id
     */
    public SimpleStringProperty idProperty() {
        return id;
    }

    /**
     * Sets id.
     *
     * @param id the id
     */
    public void setIdProperty(SimpleStringProperty id) {
        this.id = id;
    }

    /**
     * Gets ip address.
     *
     * @return the ip address
     */
    public String getIPAddress() {
        return IPAddress;
    }

    /**
     * Sets ip address.
     *
     * @param IPAddress the ip address
     */
    public void setIPAddress(String IPAddress) {
        this.IPAddress = IPAddress;
    }

    /**
     * Gets current temp.
     *
     * @return the current temp
     */
    public SimpleFloatProperty currentTempProperty() {
        return currentTemp;
    }

    /**
     * Sets current temp.
     *
     * @param currentTemp the current temp
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
     */
    public void setGraph() {
        this.graph = new Hyperlink("" + currentTempProperty().getValue());
        this.graph.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                LineChart<Number, Number> lineChart = new LineChart<>(new NumberAxis(), new NumberAxis());
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName("Blower " + idProperty().getValue());
                lineChart.getData().add(series);

                ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                executor.scheduleAtFixedRate(() -> {
                    series.getData().add(new XYChart.Data<>(count, Math.random()));
                    count++;
                }, 0, 2, TimeUnit.SECONDS);

                Scene scene = new Scene(lineChart, 400, 300);
                Stage newWindow = new Stage();
                newWindow.setTitle("GRAPH " + idProperty().getValue());
                newWindow.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResource("boge_icon.jpg")).toString()));
                newWindow.setScene(scene);
                newWindow.setX(gui.getStage().getX() + 200);
                newWindow.setY(gui.getStage().getY() + 100);
                newWindow.show();
            }
        });
    }

    /**
     * Gets target temp.
     *
     * @return the target temp
     */
    public SimpleFloatProperty targetTempProperty() {
        return targetTemp;
    }

    /**
     * Sets target temp.
     *
     * @param targetTemp the target temp
     */
    public void setTargetTempProperty(SimpleFloatProperty targetTemp) {
        this.targetTemp = targetTemp;
    }

    /**
     * Gets project.
     *
     * @return the project
     */
    public SimpleStringProperty projectNameProperty() {
        return projectName;
    }

    /**
     * Sets project.
     *
     * @param projectName the project
     */
    public void setProjectNameProperty(SimpleStringProperty projectName) {
        this.projectName = projectName;
    }

    /**
     * Gets link.
     *
     * @return the link
     */
    public Hyperlink getLink() {
        return link;
    }

    /**
     * Sets link.
     *
     * @param link the link
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
                "id=" + id +
                ", IPAddress='" + IPAddress + '\'' +
                ", currentTemp=" + currentTemp +
                ", targetTemp=" + targetTemp +
                '}';
    }
}
