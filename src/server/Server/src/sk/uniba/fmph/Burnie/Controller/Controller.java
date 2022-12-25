package sk.uniba.fmph.Burnie.Controller;

import java.net.InetAddress;

public class Controller {
    private final InetAddress IP;
    private String ID;
    private int currentTemperature;
    private short airFlow;
    private long time;

    public Controller(InetAddress ip) {
        IP = ip;
    }

    public short getAirFlow() {return airFlow;}
    public void setAirFlow(short airFlow) {this.airFlow = airFlow;}

    public long getTime() {return time;}
    public void setTime(long time) {this.time = time;}

    public void setID(String newID) {ID = newID;}
    public String getID() {return ID;}

    public void setCurrentTemperature(int currentTemperature) {this.currentTemperature = currentTemperature;}
    public int getCurrentTemperature() {return currentTemperature;}

}
