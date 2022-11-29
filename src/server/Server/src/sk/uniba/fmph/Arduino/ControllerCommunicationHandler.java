package sk.uniba.fmph.Arduino;

import sk.uniba.fmph.UDPCommunicationHandler;

import java.util.*;

public class ControllerCommunicationHandler {
    private static final ControllerCommunicationHandler INSTANCE = new ControllerCommunicationHandler();
    private ControllerCommunicationHandler() {}
    public static ControllerCommunicationHandler getInstance() {return INSTANCE;}

    private final List<Controller> controllers = new LinkedList<>();

    /**
     * Send UDP broadcast to local network and await response from all controllers
     */
    public void requestArduinoIps() {
        byte[] password = {0x41, 0x48, 0x4f, 0x4a, 0x2b};
        UDPCommunicationHandler.sendUDPPacket(password, UDPCommunicationHandler.getBroadcastAddresses());
    }

    public void addArduinoToList(Controller a) {
        System.out.println(a.IP.getHostAddress());
        System.out.println("Controller added!");
        controllers.add(a);
    }
}
