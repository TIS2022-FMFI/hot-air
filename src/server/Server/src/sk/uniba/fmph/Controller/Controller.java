package sk.uniba.fmph.Controller;

import java.net.InetAddress;

public class Controller {
    public final InetAddress IP;

    public Controller(InetAddress ip) {
        IP = ip;
    }

    /**
     * TODO -> create communication based on HTTP protocol
     */
    public void sendGetRequest() {}
}
