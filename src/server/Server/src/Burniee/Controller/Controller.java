package Burniee.Controller;

import java.net.InetAddress;

public class Controller {
    public enum Error {BOTH, DAC_NOT_FOUND, TEMP_CAN_NOT_BE_READ, NONE};
    private final InetAddress IP;
    private String ID;
    private float currentTemperature;
    private int targetTemperature;
    private short airFlow;
    private long time;
    private String projectName;
    private Error activeError = Error.NONE;

    public Controller(InetAddress ip) {IP = ip;}

    public synchronized Error getActiveError() {return activeError;}
    public synchronized void setActiveError(Error activeError) {this.activeError = activeError;}

    public synchronized String getProjectName() {return projectName;}
    public synchronized void setProjectName(String projectName) {this.projectName = projectName;}

    public synchronized short getAirFlow() {return airFlow;}
    public synchronized void setAirFlow(short airFlow) {this.airFlow = airFlow;}

    public synchronized long getTime() {return time;}
    public synchronized void setTime(long time) {this.time = time;}

    public synchronized void setID(String newID) {ID = newID;}
    public synchronized String getID() {return ID;}

    public synchronized void setCurrentTemperature(float currentTemperature) {this.currentTemperature = currentTemperature;}
    public synchronized float getCurrentTemperature() {return currentTemperature;}

    public synchronized int getTargetTemperature() {return targetTemperature;}
    public synchronized void setTargetTemperature(int targetTemperature) {this.targetTemperature = targetTemperature;}

    public InetAddress getIP() {return IP;}
}