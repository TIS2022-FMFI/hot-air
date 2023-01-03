package Burniee.Controller;

import java.net.InetAddress;

public class Controller {
    private final InetAddress IP;
    private String ID;
    private float currentTemperature;
    private int targetTemperature;
    private short airFlow;
    private long time;

    public Controller(InetAddress ip) {IP = ip;}

    public short getAirFlow() {return airFlow;}
    public void setAirFlow(short airFlow) {this.airFlow = airFlow;}

    public long getTime() {return time;}
    public void setTime(long time) {this.time = time;}

    public void setID(String newID) {ID = newID;}
    public String getID() {return ID;}

    public void setCurrentTemperature(float currentTemperature) {this.currentTemperature = currentTemperature;}
    public float getCurrentTemperature() {return currentTemperature;}

    public int getTargetTemperature() {return targetTemperature;}
    public void setTargetTemperature(int targetTemperature) {this.targetTemperature = targetTemperature;}

    public InetAddress getIP() {return IP;}
}