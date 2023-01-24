package GUI;

import Logs.GeneralLogger;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
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

//    private final Button stopButton;
//    private final Button hiddenButton;

    final NumberAxis xAxis = new NumberAxis();
    final NumberAxis yAxis = new NumberAxis();

    /**
     * Instantiates a new Project.
     *
     * @param name         the name
     * @param currentPhase the current phase
     */
    public Project(String name, String currentPhase) {
        this.name = name;
        this.currentPhase = new SimpleStringProperty(currentPhase);
        this.graph = new Hyperlink(this.name);
        this.graph.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    String pathToTempLog = GUI.client.getTempLogFile(name);
                    System.out.println(pathToTempLog);

                    HashMap<String, List<String>> temperatures = new HashMap<>();
                    try (BufferedReader br = new BufferedReader(new FileReader(pathToTempLog))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            System.out.println(line);
                            List<String> values = Arrays.asList(line.split(","));
                            for (int i = 2; i<values.size()-1; i++) {
                                if (temperatures.containsKey(values.get(i))) {
                                    temperatures.get(values.get(i)).add(values.get(i+1).trim());
                                }

                                temperatures.putIfAbsent(values.get(i), new ArrayList<>(Arrays.asList(values.get(i+1))));
                                i++;
                            }
                        }
                    }
//                    System.out.println("temperatures");
//                    for (String s : temperatures.keySet()) {
//                        System.out.println(s + " = " + temperatures.get(s));
//                    }

                    LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
                    lineChart.setCreateSymbols(false);

                    ObservableList<Blower> blowers = GUIController.getBlowersList();

                    for (String key : temperatures.keySet()) {
                        System.out.println("key: " + key);
                        Blower blower = blowers.filtered(b -> b.idProperty().getValue().trim().equals(key.trim())).get(0);
                        System.out.println(blower);
                        blower.getSeries().setName("Blower " + key);
                        lineChart.getData().add(blower.getSeries());
                        for (int i = 0; i <temperatures.get(key).size(); i++) {
                            try {
                                blower.getSeries().getData().add(new XYChart.Data<>(i, Float.parseFloat(temperatures.get(key).get(i))));
                            } catch (NumberFormatException e) {
                                GeneralLogger.writeExeption(e);
                                System.out.println(temperatures.get(key).get(i));
                                System.out.println(e.getMessage());
                            }
                        }

//                        XYChart.Series<Number, Number> series = new XYChart.Series<>();
//                        series.setName("Blower " + key);
//                        lineChart.getData().add(series);
//                        int i;
//                        for (i = 0; i <temperatures.get(key).size(); i++) {
//                            try {
//                                series.getData().add(new XYChart.Data<>(i, Float.parseFloat(temperatures.get(key).get(i))));
//                            } catch (NumberFormatException e) {
//                                GeneralLogger.writeExeption(e);
//                                System.out.println(temperatures.get(key).get(i));
//                                System.out.println(e.getMessage());
//                            }
//                        }
                    }

                    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                    executor.scheduleAtFixedRate(() -> {
                        System.out.println("UPDATE GRAFU");
//                        ObservableList<Blower> blowers = GUIController.getBlowersList();
//                        List<Blower> blowersToProject = new ArrayList<Blower>();
//
//                        blowers.filtered(b-> b.projectNameProperty().getValue().equals(name));
//                        series.getData().add(new XYChart.Data<>(i[0], blower.currentTempProperty().getValue()));
//                        i++;
                    }, 0, 1, TimeUnit.SECONDS);

                    Scene scene = new Scene(lineChart, 500, 500);
                    Stage newWindow = new Stage();
                    newWindow.setTitle("GRAPH " + name);
                    newWindow.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResource("boge_icon.jpg")).toString()));
                    newWindow.setScene(scene);
                    newWindow.setX(gui.getStage().getX() + 200);
                    newWindow.setY(gui.getStage().getY() + 100);
                    newWindow.show();

                } catch (IOException | InterruptedException | NullPointerException e) {
                    gui.alert(e);
                    e.printStackTrace();
                }

//                LineChart<Number, Number> lineChart = new LineChart<>(new NumberAxis(), new NumberAxis());
//                XYChart.Series<Number, Number> series = new XYChart.Series<>();
//                series.setName("Blower " + idProperty().getValue());
//                lineChart.getData().add(series);
//
//                ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
//                executor.scheduleAtFixedRate(() -> {
//                    series.getData().add(new XYChart.Data<>(count, Math.random()));
//                    count++;
//                }, 0, 2, TimeUnit.SECONDS);
//
//                Scene scene = new Scene(lineChart, 400, 300);
//                Stage newWindow = new Stage();
//                newWindow.setTitle("GRAPH " + idProperty().getValue());
//                newWindow.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResource("boge_icon.jpg")).toString()));
//                newWindow.setScene(scene);
//                newWindow.setX(gui.getStage().getX() + 200);
//                newWindow.setY(gui.getStage().getY() + 100);
//                newWindow.show();
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
    public void setGraph(Hyperlink graph) {
        this.graph.setText(graph.getText());
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

    @Override
    public String toString() {
        return "Project{" +
                "name='" + name + '\'' +
                ", currentPhase=" + currentPhase +
                '}';
    }
}
