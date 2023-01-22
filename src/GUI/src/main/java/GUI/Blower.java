package GUI;

import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.swing.text.html.ImageView;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private Boolean stopped;

    private int count = 0;

//    private ImageView stopped;

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
                newWindow.setX(GUI.gui.getStage().getX() + 200);
                newWindow.setY(GUI.gui.getStage().getY() + 100);
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
        this.stopped = false;

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
     * Gets if blower is stopped.
     *
     * @return boolean if blower is stopped
     */
    public Boolean getStopped() {
        return stopped;
    }

    /**
     * Sets if blower is stopped.
     *
     * @param stopped boolean if blower is stopped
     */
    public void setStopped(Boolean stopped) {
        this.stopped = stopped;
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
