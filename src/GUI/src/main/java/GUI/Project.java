package GUI;

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

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static GUI.GUI.gui;
import static GUI.GUIController.setAlertIcons;
import static javafx.scene.layout.Region.USE_COMPUTED_SIZE;

/**
 * Class for projects.
 */
public class Project {
    private String name;
    private Hyperlink graph;
    private SimpleStringProperty currentPhase;

    private final Button stopButton;
    private final Button hiddenButton;

    private int count = 0;

    /**
     * Instantiates a new Project.
     *
     * @param name         the name
     * @param currentPhase the current phase
     */
    public Project(String name, String currentPhase) {
        this.name = name;
        setGraph();
        this.currentPhase = new SimpleStringProperty(currentPhase);

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
            alert.setTitle("RESUMING PROJECT");
            alert.setHeaderText("Do you really want to resume project " + this.name + "?");
            setAlertIcons(alert);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                try {
                    // todo funkcia na resume projektu    // todo debug
                    hiddenButton.setVisible(false);
                    System.out.println("project " + this.name + " was resumed");
                } catch (Exception e) {
                    System.err.println("project " + this.name + " could not be resumed");
                    gui.alert(e);
                }
            } else {
                System.out.println("project " + this.name + " will not be resumed");
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
            alert.setTitle("STOPPING PROJECT");
            alert.setHeaderText("Do you really want to stop project " + this.name + "?");
            setAlertIcons(alert);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                try {
                    // todo funkcia na stopnutie projektu  // todo debug
                    hiddenButton.setVisible(true);
                    System.out.println("project " + this.name + " stopped");
                } catch (Exception e) {
                    System.err.println("project " + this.name + " could not be stopped");
                    gui.alert(e);
                }
            } else {
                System.out.println("project " + this.name + " will not be stopped");
            }
        });
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
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
        this.graph = new Hyperlink("" + getName());
        this.graph.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                LineChart<Number, Number> lineChart = new LineChart<>(new NumberAxis(), new NumberAxis());
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                // loger
//                series.setName("Blower " + );  //
                lineChart.getData().add(series);

                ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                executor.scheduleAtFixedRate(() -> {
                    series.getData().add(new XYChart.Data<>(count, Math.random()));
                    count++;
                }, 0, 2, TimeUnit.SECONDS);

                Scene scene = new Scene(lineChart, 400, 300);
                Stage newWindow = new Stage();
                newWindow.setTitle("GRAPH FOR " + getName());
                newWindow.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResource("boge_icon.jpg")).toString()));
                newWindow.setScene(scene);
                newWindow.setX(gui.getStage().getX() + 200);
                newWindow.setY(gui.getStage().getY() + 100);
                newWindow.show();
            }
        });
    }

    /**
     * Gets current phase.
     *
     * @return the current phase
     */
    public SimpleStringProperty currentPhaseProperty() {
        return currentPhase;
    }

    /**
     * Sets current phase.
     *
     * @param currentPhase the current phase
     */
    public void setCurrentPhaseProperty(SimpleStringProperty currentPhase) {
        this.currentPhase = currentPhase;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return Objects.equals(name, project.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
