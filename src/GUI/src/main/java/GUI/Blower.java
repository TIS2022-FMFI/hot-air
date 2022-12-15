package GUI;

import javafx.scene.control.Hyperlink;

public class Blower {
    private String id;  // do 15 znakov, ascii,
    private String IPAddress;
    private float currentTemp;
    private float targetTemp;
    private String project;
    private Hyperlink link;

    public Blower(String IPAddress, String id, float currentTemp, float targetTemp, String project) {
        this.IPAddress = IPAddress;
        this.id = id;
        this.currentTemp = currentTemp;
        this.targetTemp = targetTemp;
        this.project = project;
        this.link = new Hyperlink(IPAddress + "/settings");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIPAddress() {
        return IPAddress;
    }

    public void setIPAddress(String IPAddress) {
        this.IPAddress = IPAddress;
    }

    public float getCurrentTemp() {
        return currentTemp;
    }

    public void setCurrentTemp(float currentTemp) {
        this.currentTemp = currentTemp;
    }

    public float getTargetTemp() {
        return targetTemp;
    }

    public void setTargetTemp(float targetTemp) {
        this.targetTemp = targetTemp;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public Hyperlink getLink() {
        return link;
    }

    public void setLink(Hyperlink link) {
        this.link = link;
    }
}
