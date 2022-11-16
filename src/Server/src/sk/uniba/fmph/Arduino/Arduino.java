package sk.uniba.fmph.Arduino;

import java.net.InetAddress;

public class Arduino {
    public final InetAddress IP;

    public Arduino(InetAddress ip) {
        IP = ip;
    }

    /**
     * TODO -> create communication based on HTTP protocol
     */
    public void sendGetRequest() {}
}
