package GUI;

import Logs.GeneralLogger;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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
import static GUI.GUIController.setAlertIcons;
import static javafx.scene.layout.Region.USE_COMPUTED_SIZE;

/**
 * Class for projects.
 */
public class Project {
    private String name;
    private Hyperlink graph;
    private SimpleStringProperty currentPhase;
    private HashMap<String, List<Pair<String, String>>> temperatures;

    private final Button stopButton;

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

                    lineChart.setPrefSize(500, 200);
                    lineChart.setCreateSymbols(false);
//                    lineChart.setFocusTraversable(true);
//                    lineChart.setAnimated(true);
                    ObservableList<Blower> blowers = GUIController.getBlowersList();
                    ObservableList<Project> projects = GUIController.guiController.getProjectsList();

                    for (String key : temperatures.keySet()) {
                        System.out.println("key: " + key);
                        Blower blower = blowers.filtered(b -> b.idProperty().getValue().equals(key)).get(0);
                        System.out.println(blower);

                        blower.getCurrentSeries().setName("Blower " + key);
                        blower.getTargetSeries().setName("Target " + key);
                        if (!lineChart.getData().contains(blower.getCurrentSeries())) {
                            lineChart.getData().add(blower.getCurrentSeries());
                        }
                        if (!lineChart.getData().contains(blower.getTargetSeries())) {
                            lineChart.getData().add(blower.getTargetSeries());
                        }

                        List<Pair<String, String>> values = temperatures.get(key);
                        for (int i = 0; i <values.size(); i++) {
                            try {
                                blower.getCurrentSeries().getData().add(new XYChart.Data<>(i, Float.parseFloat(values.get(i).getKey())));
                                blower.getTargetSeries().getData().add(new XYChart.Data<>(i, Float.parseFloat(values.get(i).getValue())));

                            } catch (NumberFormatException e) {
                                GeneralLogger.writeExeption(e);
                                System.err.println(values.get(i));
                                System.err.println(e.getMessage());
                                e.printStackTrace();
                            }
                        }

                        NumberAxis xAxisLocal = ((NumberAxis) lineChart.getXAxis());
                        xAxisLocal.setUpperBound(values.size()+70);
                        xAxisLocal.setLowerBound(values.size()-30);

                        blower.getCurrentSeries().getNode().setStyle("-fx-stroke-width: 5px;");
                        blower.getTargetSeries().getNode().setStyle("-fx-stroke-width: 3px;");
                        blower.getTargetSeries().getNode().setStyle("-fx-opacity: 0.5");
                    }

                    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                    executor.scheduleAtFixedRate(() -> {
                        System.out.println("UPDATE GRAFU");
                        updateGraph();
                    }, 0, 1, TimeUnit.SECONDS);

                    ScrollPane scroll = new ScrollPane(lineChart);
                    scroll.viewportBoundsProperty().addListener(new ChangeListener<Bounds>() {
                        @Override
                        public void changed(ObservableValue<? extends Bounds> observableValue, Bounds oldBounds, Bounds newBounds) {
                            lineChart.setMinSize(Math.max(lineChart.getPrefWidth(), newBounds.getWidth()), Math.max(lineChart.getPrefHeight(), newBounds.getHeight()));
                            scroll.setPannable((lineChart.getPrefWidth() > newBounds.getWidth()) || (lineChart.getPrefHeight() > newBounds.getHeight()));
                        }
                    });

                    lineChart.setOnScroll(new EventHandler<ScrollEvent>() {
                        @Override
                        public void handle(ScrollEvent ev) {
                            double delta = 5;
                            double deltaY = ev.getDeltaY();

                            if (deltaY < 0) {
                                delta = -5;
                            }

                            NumberAxis xAxisLocal = ((NumberAxis) lineChart.getXAxis());
                            xAxisLocal.setUpperBound(xAxisLocal.getUpperBound() + delta);
                            xAxisLocal.setLowerBound(xAxisLocal.getLowerBound() + delta);

                            ev.consume();
                        }
                    });

                    Scene scene = new Scene(scroll, 500, 500);
                    Stage graphWindow = new Stage();
                    graphWindow.setTitle("GRAPH " + name);
                    graphWindow.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResource("boge_icon.jpg")).toString()));
                    graphWindow.setScene(scene);
                    graphWindow.setX(gui.getStage().getX() + 200);
                    graphWindow.setY(gui.getStage().getY() + 100);
                    graphWindow.show();
                    graphWindow.setOnCloseRequest(e -> {
                        executor.shutdown();
                    });

                } catch (IOException | InterruptedException | NullPointerException e) {
                    gui.alert(e);
                    e.printStackTrace();
                }

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
            alert.setHeaderText("Do you really want to stop project " + name + "?");
            setAlertIcons(alert);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                try {
                    gui.client.stopAProject(name);
                    GeneralLogger.writeMessage("project " + name + " stopped");
                    System.out.println("project " + name + " stopped");
                } catch (Exception e) {
                    GeneralLogger.writeExeption(e);
                    System.err.println("project " + name + " could not be stopped");
                    gui.alert(e);
                }
            } else {
                System.out.println("project " + name + " will not be stopped");
            }
        });
    }

    private void updateGraph() {
        ObservableList<Blower> blowers = GUIController.getBlowersList();
        for (String key : temperatures.keySet()) {
//            System.out.println("key: " + key);
            Blower blower = blowers.filtered(b -> b.idProperty().getValue().equals(key)).get(0);
//            System.out.println(blower);
            int i = blower.getCurrentSeries().getData().size();
            try {
                blower.getCurrentSeries().getData().add(new XYChart.Data<>(i + 1, blower.currentTempProperty().getValue()));
                blower.getTargetSeries().getData().add(new XYChart.Data<>(i + 1, blower.targetTempProperty().getValue()));
                if (i % 50 == 49) {
                    NumberAxis xAxisLocal = ((NumberAxis) lineChart.getXAxis());
                    xAxisLocal.setUpperBound(i + 70);
                    xAxisLocal.setLowerBound(i - 30);
                }
            } catch (NumberFormatException e) {
                GeneralLogger.writeExeption(e);
                System.err.println(e.getMessage());
                e.printStackTrace();
            }

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
     * Gets stop button.
     *
     * @return the stop button
     */
    public Button getStopButton() {
        return stopButton;
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
