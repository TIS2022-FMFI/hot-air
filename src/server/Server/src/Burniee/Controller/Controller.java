package Burniee.Controller;

import Burniee.Project.Project;

import java.net.InetAddress;

public class Controller {
    public enum Error {BOTH, DAC_NOT_FOUND, TEMP_CAN_NOT_BE_READ, NONE};
    private final InetAddress IP;
    private String ID;
    private float currentTemperature;
    private int targetTemperature;
    private short airFlow;
    private long time;
    private Project project = null;
    private Error activeError = Error.NONE;
    private boolean stopped;

    public Controller(InetAddress ip) {IP = ip;}

    public synchronized Error getActiveError() {return activeError;}
    public synchronized void setActiveError(Error activeError) {this.activeError = activeError;}

    public synchronized Project getProject() {return project;}
    public synchronized void setProject(Project project) {this.project = project;}

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

    public synchronized void setStopped(boolean s) {stopped = s;}
    public synchronized boolean getStopped() {return stopped;}

    public InetAddress getIP() {return IP;}
}