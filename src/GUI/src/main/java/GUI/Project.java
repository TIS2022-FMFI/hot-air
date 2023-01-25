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
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static GUI.GUI.gui;

/**
 * Class for projects.
 */
public class Project {
    private String name;
    private Hyperlink graph;
    private SimpleStringProperty currentPhase;
    private HashMap<String, List<Pair<String, String>>> temperatures;



//    private final Button stopButton;
//    private final Button hiddenButton;

    final NumberAxis xAxis = new NumberAxis();
    final NumberAxis yAxis = new NumberAxis();
    private final LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);


    /**
     * Instantiates a new Project.
     *
     * @param name         the name
     * @param currentPhase the current phase
     */
    public Project(String name, String currentPhase) {
        this.name = name;
        this.currentPhase = new SimpleStringProperty(currentPhase);
        xAxis.setLabel("time");
        xAxis.setAutoRanging(false);
        yAxis.setLabel("temperature");
        this.graph = new Hyperlink(this.name);
        this.graph.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    String pathToTempLog = GUI.client.getTempLogFile(name);
                    System.out.println(pathToTempLog);

                    temperatures = new HashMap<>();
                    try (BufferedReader br = new BufferedReader(new FileReader(pathToTempLog))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            System.out.println(line);
                            List<String> values = Arrays.asList(line.split(","));
                            for (int i = 2; i<values.size()-1; i++) {
                                String key = values.get(i).trim();
                                String value1 = values.get(i+1).trim();
                                String value2 = values.get(i+2).trim();
                                Pair<String, String> pair = new Pair<>(value1, value2);
                                if (temperatures.containsKey(key)) {
                                    temperatures.get(key).add(pair);
                                }

                                temperatures.putIfAbsent(key, new ArrayList<>(Arrays.asList(pair)));
                                i+=2;
                            }
                        }
                    }

                    lineChart.setCreateSymbols(false);
                    lineChart.setFocusTraversable(true);
                    lineChart.setAnimated(true);
                    ObservableList<Blower> blowers = GUIController.getBlowersList();
                    ObservableList<Project> projects = GUIController.guiController.getProjectsList();

                    for (String key : temperatures.keySet()) {
                        System.out.println("key: " + key);
                        Blower blower = blowers.filtered(b -> b.idProperty().getValue().equals(key)).get(0);
                        System.out.println(blower);

//                        blower.getCurrentSeries().setName("Blower " + key);
//                        blower.getTargetSeries().setName("Target " + key);
                        lineChart.getData().add(blower.getCurrentSeries());
                        lineChart.getData().add(blower.getTargetSeries());
                        List<Pair<String, String>> values = temperatures.get(key);
                        for (int i = 0; i <values.size(); i++) {
                            try {
                                blower.getCurrentSeries().getData().add(new XYChart.Data<>(i, Float.parseFloat(values.get(i).getKey())));
                                blower.getTargetSeries().getData().add(new XYChart.Data<>(i, Float.parseFloat(values.get(i).getValue())));

//                                blower.currentSeriesData.add(new XYChart.Data<>(i, Float.parseFloat(values.get(i).getKey())));
//                                blower.targetSeriesData.add(new XYChart.Data<>(i, Float.parseFloat(values.get(i).getValue())));
                            } catch (NumberFormatException e) {
                                GeneralLogger.writeExeption(e);
                                System.err.println(values.get(i));
                                System.err.println(e.getMessage());
                                e.printStackTrace();
                            }
                        }
//                        blower.getCurrentSeries().getNode().setStyle("-fx-stroke-width: 3px;");
//                        blower.getCurrentSeries().getNode().getStyleClass().add("my-node");

                        blower.getCurrentSeries().getNode().getStyleClass().add("my-node");

                        blower.getTargetSeries().getNode().setStyle("-fx-stroke-width: 2px;");
                        blower.getTargetSeries().getNode().setStyle("-fx-opacity: 0.5 ");


                    }

//                    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
//                    executor.scheduleAtFixedRate(() -> {
//                        System.out.println("UPDATE GRAFU");
//                        updateGraph();
//                    }, 0, 1, TimeUnit.SECONDS);
//                    if (projects.filtered(a-> a.getName().equals(name)).size() == 0) executor.shutdownNow();

                    ScrollPane scroll = new ScrollPane(lineChart);
                    Scene scene = new Scene(scroll, 500, 500);
                    Stage graphWindow = new Stage();
                    graphWindow.setTitle("GRAPH " + name);
                    graphWindow.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResource("boge_icon.jpg")).toString()));
                    graphWindow.setScene(scene);
                    graphWindow.setX(gui.getStage().getX() + 200);
                    graphWindow.setY(gui.getStage().getY() + 100);
                    graphWindow.show();

//                    for (XYChart.Series<Number, Number> s : lineChart.getData()) {
//                        for (XYChart.Data<Number, Number> d : s.getData()) {
//                            Tooltip t = new Tooltip(d.getYValue().toString());
//                            Tooltip.install(d.getNode(), t);
//
////                            Tooltip.install(d.getNode(), new Tooltip(
////                                    d.getXValue()+ "\n" + d.getYValue()));
//
////                            //Adding class on hover
////                            d.getNode().setOnMouseEntered(e -> d.getNode().getStyleClass().add("onHover"));
////
////                            //Removing class on exit
////                            d.getNode().setOnMouseExited(e -> d.getNode().getStyleClass().remove("onHover"));
//                        }
//                    }

                } catch (IOException | InterruptedException | NullPointerException e) {
                    gui.alert(e);
                    e.printStackTrace();
                }

            }
        });
    }

    private void updateGraph() {
        ObservableList<Blower> blowers = GUIController.getBlowersList();
        for (String key : temperatures.keySet()) {
            System.out.println("key: " + key);
            Blower blower = blowers.filtered(b -> b.idProperty().getValue().equals(key)).get(0);
            System.out.println(blower);
            int i = blower.getCurrentSeries().getData().size();
            try {

                blower.getCurrentSeries().getData().add(new XYChart.Data<>(i+1, blower.currentTempProperty().getValue()));
                blower.getTargetSeries().getData().add(new XYChart.Data<>(i+1, blower.targetTempProperty().getValue()));

//                blower.currentSeriesData.add(new XYChart.Data<>(i+1, blower.currentTempProperty().getValue()));
//                blower.targetSeriesData.add(new XYChart.Data<>(i+1, blower.targetTempProperty().getValue()));
            } catch (NumberFormatException e) {
                GeneralLogger.writeExeption(e);
                System.err.println(e.getMessage());
                e.printStackTrace();
            }


//            for (XYChart.Data<Number, Number> d : blower.getCurrentSeries().getData()) {
//                Tooltip.install(d.getNode(), new Tooltip(
//                        d.getXValue().toString() + "\n" +
//                                "Number Of Events : " + d.getYValue()));
//
//                //Adding class on hover
//                d.getNode().setOnMouseEntered(e -> d.getNode().getStyleClass().add("onHover"));
//
//                //Removing class on exit
//                d.getNode().setOnMouseExited(e -> d.getNode().getStyleClass().remove("onHover"));
//            }

//            for (XYChart.Data<Number, Number> entry : blower.getCurrentSeries().getData()) {
//                System.out.println("Entered!");
//                Tooltip t = new Tooltip(entry.getYValue().toString());
//                Tooltip.install(entry.getNode(), t);
//            }
//            for (XYChart.Data<Number, Number> entry : blower.getTargetSeries().getData()) {
//                Tooltip t = new Tooltip(entry.getYValue().toString());
//                Tooltip.install(entry.getNode(), t);
//            }
        }
    }

    /**
     * Gets project name.
     *
     * @return project name, from NAME TAG in XML
     */
    public String getName() {
        return name;
    }

    /**
     * Sets project name.
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
