package GUI;

import Logs.GeneralLogger;
import javafx.beans.property.SimpleFloatProperty;
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

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
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
    private SimpleStringProperty id;
    private Hyperlink link;
    private String IPAddress;
    private SimpleFloatProperty currentTemp;
    private Hyperlink graph;
    private SimpleFloatProperty targetTemp;
    private SimpleStringProperty projectName;

    private final Button stopButton;
    private final Button hiddenButton;

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
//        this.graph = new Hyperlink("a");
//        this.graph.setOnAction(new EventHandler<ActionEvent>() {
//            @Override
//            public void handle(ActionEvent event) {
//                try {
////                    String pathToTempLog = GUI.client.getTempLogFile(projectNameProperty().getValue()); // cesta ku suboru
//                    String pathToTempLog = "C:\\Users\\Karin\\Downloads\\pokus_temperatures_2023.01.23.23.08.44.csv"; // cesta ku suboru
//                    System.out.println(pathToTempLog);
//
//                    HashMap<String, List<String>> temperatures = new HashMap<>();
//                    try (BufferedReader br = new BufferedReader(new FileReader(pathToTempLog))) {
//                        String line;
//                        while ((line = br.readLine()) != null) {
//                            System.out.println(line);
//                            List<String> values = Arrays.asList(line.split(","));
//                            for (int i = 2; i<values.size()-1; i++) {
//                                if (temperatures.containsKey(values.get(i))) {
//                                    temperatures.get(values.get(i)).add(values.get(i+1).trim());
//                                }
//
//                                temperatures.putIfAbsent(values.get(i), new ArrayList<>(Arrays.asList(values.get(i+1))));
//                                i++;
//                            }
//                        }
//                    }
//                    System.out.println("temperatures");
//                    for (String s : temperatures.keySet()) {
//                        System.out.println(s + " = " + temperatures.get(s));
//                    }
//
//                    LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
//
//                    for (String key : temperatures.keySet()) {
//                        XYChart.Series<Number, Number> series = new XYChart.Series<>();
//                        series.setName("Blower " + key);
//                        lineChart.getData().add(series);
//                        final int[] i = new int[1];
//                        for (i[0] = 0; i[0] <temperatures.get(key).size(); i[0]++) {
//                            try {
//                                series.getData().add(new XYChart.Data<>(i[0], Float.parseFloat(temperatures.get(key).get(i[0]))));
//                            } catch (NumberFormatException e) {
//                                GeneralLogger.writeExeption(e);
//                                System.out.println(temperatures.get(key).get(i[0]));
//                                System.out.println(e.getMessage());
//                            }
//                        }
//
//                        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
//                        executor.scheduleAtFixedRate(() -> {
//                            ObservableList<Blower> blowers = GUIController.getBlowersList();
//                            Blower blower = blowers.filtered(b-> b.idProperty().getValue().equals(key)).get(0);
//                            series.getData().add(new XYChart.Data<>(i[0], blower.currentTempProperty().getValue()));
//                            i[0]++;
//                        }, 0, 1, TimeUnit.SECONDS);
//                    }
//
//
//                    Scene scene = new Scene(lineChart, 400, 300);
//                    Stage newWindow = new Stage();
//                    newWindow.setTitle("GRAPH " + idProperty().getValue());
//                    newWindow.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResource("boge_icon.jpg")).toString()));
//                    newWindow.setScene(scene);
//                    newWindow.setX(gui.getStage().getX() + 200);
//                    newWindow.setY(gui.getStage().getY() + 100);
//                    newWindow.show();
//
//                } catch (IOException | NullPointerException e) { // InterruptedException
//                    gui.alert(e);
//                    e.printStackTrace();
//                }
//
////                LineChart<Number, Number> lineChart = new LineChart<>(new NumberAxis(), new NumberAxis());
////                XYChart.Series<Number, Number> series = new XYChart.Series<>();
////                series.setName("Blower " + idProperty().getValue());
////                lineChart.getData().add(series);
////
////                ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
////                executor.scheduleAtFixedRate(() -> {
////                    series.getData().add(new XYChart.Data<>(count, Math.random()));
////                    count++;
////                }, 0, 2, TimeUnit.SECONDS);
////
////                Scene scene = new Scene(lineChart, 400, 300);
////                Stage newWindow = new Stage();
////                newWindow.setTitle("GRAPH " + idProperty().getValue());
////                newWindow.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResource("boge_icon.jpg")).toString()));
////                newWindow.setScene(scene);
////                newWindow.setX(gui.getStage().getX() + 200);
////                newWindow.setY(gui.getStage().getY() + 100);
////                newWindow.show();
//            }
//        });
        this.targetTemp = new SimpleFloatProperty(targetTemp);;
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
    public void setGraph(Hyperlink graph) {
        this.graph.setText(graph.getText());
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

    public XYChart.Series<Number, Number> getSeries() {
        return series;
    }

    public void setSeries(XYChart.Series<Number, Number> series) {
        this.series = series;
    }

    private XYChart.Series<Number, Number> series = new XYChart.Series<>();

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
