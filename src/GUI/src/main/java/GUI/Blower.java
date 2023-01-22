package GUI;

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
    private String id;  // do 15 znakov, ascii,
    private String IPAddress;
    private float currentTemp;
    private Hyperlink graph;
    private float targetTemp;
    private String project;
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
     * @param project     the corresponding project
     */
    public Blower(String IPAddress, String id, float currentTemp, float targetTemp, String project) {
        this.IPAddress = IPAddress;
        this.id = id;
        this.currentTemp = currentTemp;
        this.graph = new Hyperlink("" + this.currentTemp);
        this.graph.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                LineChart<Number, Number> lineChart = new LineChart<>(new NumberAxis(), new NumberAxis());
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName("Blower " + getId());
                lineChart.getData().add(series);

                ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                executor.scheduleAtFixedRate(() -> {
                    series.getData().add(new XYChart.Data<>(count, Math.random()));
                    count++;
                }, 0, 2, TimeUnit.SECONDS);

                Scene scene = new Scene(lineChart, 400, 300);
                Stage newWindow = new Stage();
                newWindow.setTitle("GRAPH " + id);
                newWindow.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResource("boge_icon.jpg")).toString()));
                newWindow.setScene(scene);
                newWindow.setX(gui.getStage().getX() + 200);
                newWindow.setY(gui.getStage().getY() + 100);
                newWindow.show();
            }
        });
        this.targetTemp = targetTemp;
        this.project = project;
        this.link = new Hyperlink(this.id);
        this.link.setOnAction(event -> {
            try {
                String url = "http://" + IPAddress + "/control";
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
            alert.setHeaderText("Do you really want to resume blower " + getId() + "?");
            setAlertIcons(alert);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                try {
//                    gui.client.stopAController(getId());  // todo debug
                    hiddenButton.setVisible(false);
                    System.out.println("blower " + getId() + " was resumed");
                } catch (Exception e) {
                    System.err.println("blower " + getId() + " could not be resumed");
                    gui.alert(e);
                }
            } else {
                System.out.println("blower " + getId() + " will not be resumed");
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
            alert.setHeaderText("Do you really want to stop blower " + getId() + "?");
            setAlertIcons(alert);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                try {
//                    gui.client.stopAController(getId());  // todo debug
                    hiddenButton.setVisible(true);
                    System.out.println("blower " + getId() + " stopped");
                } catch (Exception e) {
                    System.err.println("blower " + getId() + " could not be stopped");
                    gui.alert(e);
                }
            } else {
                System.out.println("blower " + getId() + " will not be stopped");
            }
        });
    }

    /**
     * Gets id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets id.
     *
     * @param id the id
     */
    public void setId(String id) {
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
    public float getCurrentTemp() {
        return currentTemp;
    }

    /**
     * Sets current temp.
     *
     * @param currentTemp the current temp
     */
    public void setCurrentTemp(float currentTemp) {
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
     * @param graph the link to graph
     */
    public void setGraph(Hyperlink graph) {
        this.graph = graph;
    }

    /**
     * Gets target temp.
     *
     * @return the target temp
     */
    public float getTargetTemp() {
        return targetTemp;
    }

    /**
     * Sets target temp.
     *
     * @param targetTemp the target temp
     */
    public void setTargetTemp(float targetTemp) {
        this.targetTemp = targetTemp;
    }

    /**
     * Gets project.
     *
     * @return the project
     */
    public String getProject() {
        return project;
    }

    /**
     * Sets project.
     *
     * @param project the project
     */
    public void setProject(String project) {
        this.project = project;
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
        return Objects.equals(id, blower.id) && Objects.equals(IPAddress, blower.IPAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, IPAddress);
    }

}
