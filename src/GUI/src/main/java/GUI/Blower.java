package GUI;

import javafx.scene.control.Hyperlink;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Class for blowers.
 */
public class Blower {
    private String id;  // do 15 znakov, ascii,
    private String IPAddress;
    private float currentTemp;
    private float targetTemp;
    private String project;
    private Hyperlink link;

    /**
     * Instantiates a new Blower.
     *
     * @param IPAddress   the ip address
     * @param id          the id
     * @param currentTemp the current temp
     * @param targetTemp  the target temp
     * @param project     the corresponding project
     */
    public Blower(String IPAddress, String id, float currentTemp, float targetTemp, String project) {
        this.IPAddress = IPAddress;
        this.id = id;
        this.currentTemp = currentTemp;
        this.targetTemp = targetTemp;
        this.project = project;
//        this.link = new Hyperlink(IPAddress + "/settings");  // todo na debug
        this.link = new Hyperlink("https://www.google.sk/");
        this.link.setOnAction(event -> {
            try {
                System.out.println(link.getText());
                Desktop.getDesktop().browse(new URI(this.link.getText()));
            } catch (IOException | URISyntaxException e) {
                // todo zapisat do logov
                e.printStackTrace();
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
}
