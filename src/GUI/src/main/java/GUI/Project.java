package GUI;

import Logs.GeneralLogger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
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
    private SimpleStringProperty status;
    private SimpleStringProperty currentPhase;
    private Hyperlink graph;
    private HashMap<String, List<Pair<String, String>>> tempLogFile;

    private final Button stopButton;
    private NumberAxis xAxis;
    private NumberAxis yAxis;
    private LineChart<Number, Number> lineChart;

    /**
     * Instantiates a new Project.
     *
     * @param name         the name
     * @param currentPhase the current phase
     */
    public Project(String name, String currentPhase) {
        this.name = name;
        this.status = new SimpleStringProperty("running");
        this.currentPhase = new SimpleStringProperty(currentPhase);
        this.graph = new Hyperlink(this.name);
        this.graph.setOnAction(event -> openGraph());
        this.stopButton = new Button("STOP");
        setStopButton();
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
            alert.setTitle("STOPPING PROJECT");
            alert.setHeaderText("Do you really want to stop project " + name + "?");
            setAlertIcons(alert);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                try {
                    GUI.client.stopAProject(name);
                    setStatus("stopped");
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

    private void openGraph() {
        try {
            System.out.println("openGraph");

            xAxis = new NumberAxis();
            xAxis.setLabel("time (s)");
            xAxis.setAutoRanging(false);
            yAxis = new NumberAxis();
            yAxis.setLabel("temperature ( °C )");
            lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.setPrefSize(500, 200);
            lineChart.setCreateSymbols(false);

            String pathToTempLog = GUI.client.getTempLogFile(name);
            System.out.println(pathToTempLog);

            tempLogFile = new HashMap<>();

            loadTemperaturesFromLog(pathToTempLog);
            addTemperaturesFromLog();

            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            executor.scheduleAtFixedRate(this::updateGraph, 0, 1, TimeUnit.SECONDS);

            lineChart.setOnScroll(ev -> {
                double deltaY = ev.getDeltaY();

                if (deltaY < 0) {
                    Platform.runLater(()-> {
                        NumberAxis xAxisLocal = ((NumberAxis) lineChart.getXAxis());
                        xAxisLocal.setUpperBound(xAxisLocal.getUpperBound() - 5);
                        xAxisLocal.setLowerBound(xAxisLocal.getLowerBound() - 5);
                    });
                } else {
                    Platform.runLater(()-> {
                        NumberAxis xAxisLocal = ((NumberAxis) lineChart.getXAxis());
                        xAxisLocal.setUpperBound(xAxisLocal.getUpperBound() + 5);
                        xAxisLocal.setLowerBound(xAxisLocal.getLowerBound() + 5);
                    });
                }
                ev.consume();
            });

            Scene scene = new Scene(lineChart, 500, 500);
            Stage graphWindow = new Stage();
            graphWindow.setTitle(name);
            graphWindow.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResource("boge_icon.jpg")).toString()));
            graphWindow.setScene(scene);
            graphWindow.setX(gui.getStage().getX() + 200);
            graphWindow.setY(gui.getStage().getY() + 100);
            graphWindow.show();
            graphWindow.setOnCloseRequest(e -> executor.shutdown());

        } catch (IOException | InterruptedException | NullPointerException e) {
            gui.alert(e);
            e.printStackTrace();
        }
    }

    private void loadTemperaturesFromLog(String pathToTempLog) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(pathToTempLog))) {
            System.out.println("loadTemperaturesFromLog");
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                List<String> values = Arrays.asList(line.split(","));
                for (int i = 2; i<values.size()-1; i++) {
                    String key = values.get(i).trim();
                    String value1 = values.get(i+1).trim();
                    String value2 = values.get(i+2).trim();
                    Pair<String, String> pair = new Pair<>(value1, value2);
                    if (tempLogFile.containsKey(key)) {
                        tempLogFile.get(key).add(pair);
                    }

                    tempLogFile.putIfAbsent(key, new ArrayList<>(Arrays.asList(pair)));
                    i+=2;
                }
            }
        } catch (IOException e) {
            System.err.println("loadTemperaturesFromLog exception");
            throw e;
        }
    }

    private void addTemperaturesFromLog() {
        ObservableList<Blower> blowers = GUIController.getBlowersList();

        System.out.println("addTemperaturesFromLog");
        for (String key : tempLogFile.keySet()) {
            Blower blower = blowers.filtered(b -> b.getId().equals(key)).get(0);
            synchronized (blower) {
                XYChart.Series<Number, Number> currentSerie = new XYChart.Series<>("Blower " + key, blower.getCurrentData());
                XYChart.Series<Number, Number> targetSerie = new XYChart.Series<>("Target " + key, blower.getTargetData());
                lineChart.getData().add(currentSerie);
                lineChart.getData().add(targetSerie);

                blower.getCurrentData().clear();
                blower.getTargetData().clear();
                List<Pair<String, String>> values = tempLogFile.get(key);
                for (int i = 0; i < values.size(); i++) {
                    try {
                        blower.getCurrentData().add(new XYChart.Data<>(i + 1, Float.parseFloat(values.get(i).getKey())));
                        blower.getTargetData().add(new XYChart.Data<>(i + 1, Float.parseFloat(values.get(i).getValue())));
                    } catch (NumberFormatException e) {
                        GeneralLogger.writeExeption(e);
                        System.err.println(values.get(i));
                        System.err.println(e.getMessage());
                        e.printStackTrace();
                    }
                }
                currentSerie.getNode().setStyle("-fx-stroke-width: 3px;");
                targetSerie.getNode().setStyle("-fx-stroke-width: 2px;");
                targetSerie.getNode().setStyle("-fx-opacity: 0.5");

                NumberAxis xAxisLocal = ((NumberAxis) lineChart.getXAxis());
                xAxisLocal.setUpperBound(values.size()+70);
                xAxisLocal.setLowerBound(values.size()-30);
            }
        }
    }

    private void updateGraph() {
        System.out.println("updateGraph");
        ObservableList<Blower> blowers = GUIController.getBlowersList();
        for (String key : tempLogFile.keySet()) {
            Blower blower = blowers.filtered(b -> b.getId().equals(key)).get(0);
            synchronized (blower) {
                int i = blower.getCurrentData().size();
                try {
                    blower.getCurrentData().add(new XYChart.Data<>(i + 1, blower.getCurrentTemp()));
                    blower.getTargetData().add(new XYChart.Data<>(i + 1, blower.getTargetTemp()));

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
    }

//    TODO delete co netreba
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
     * @param name the project name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets status of project.
     *
     * @return the status
     */
    public String getStatus() {
        return status.get();
    }

    /**
     * Sets status of project.
     *
     * @param status the status
     */
    public void setStatus(String status) {
        this.status.set(status);
    }

    /**
     * Gets current phase.
     *
     * @return the current phase
     */
    public String getCurrentPhase() {
        return currentPhase.get();
    }

    /**
     * Sets current phase.
     *
     * @param currentPhase the current phase
     */
    public void setCurrentPhase(String currentPhase) {
        this.currentPhase.set(currentPhase);
    }

//    TODO delete ak netreba
    public HashMap<String, List<Pair<String, String>>> getTempLogFile() {
        return tempLogFile;
    }

    public void setTempLogFile(HashMap<String, List<Pair<String, String>>> tempLogFile) {
        this.tempLogFile = tempLogFile;
    }

    public NumberAxis getxAxis() {
        return xAxis;
    }

    public void setxAxis(NumberAxis xAxis) {
        this.xAxis = xAxis;
    }

    public NumberAxis getyAxis() {
        return yAxis;
    }

    public void setyAxis(NumberAxis yAxis) {
        this.yAxis = yAxis;
    }

    public LineChart<Number, Number> getLineChart() {
        return lineChart;
    }

    public void setLineChart(LineChart<Number, Number> lineChart) {
        this.lineChart = lineChart;
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
