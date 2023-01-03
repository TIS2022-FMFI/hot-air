package Communication;

import java.net.InetAddress;
import java.util.List;

public class RequestResult {
    private static final RequestResult INSTANCE = new RequestResult();
    private RequestResult() {}
    public static RequestResult getInstance() {return INSTANCE;}

    private int intData;
    private String stringData;
    private Controller[] controllers;

    public int getIntData() {return intData;}
    public void setIntData(int intData) {this.intData = intData;}

    public String getStringData() {return stringData;}
    public void setStringData(String stringData) {this.stringData = stringData;}

    public Controller[] getControllers() {return controllers;}
    public void setControllers(Controller[] controllers) {this.controllers = controllers;}

    public static class Controller {
        private final InetAddress IP;
        private String ID;
        private int currentTemperature;
        private int targetTemperature;
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

        public int getTargetTemperature() {return targetTemperature;}
        public void setTargetTemperature(int targetTemperature) {this.targetTemperature = targetTemperature;}

        public InetAddress getIP() {return IP;}
    }
}
