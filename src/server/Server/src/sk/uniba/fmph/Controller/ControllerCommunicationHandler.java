package sk.uniba.fmph.Controller;

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
    public void requestControllerIps() {
        UDPCommunicationHandler.sendUDPPacket(UDPCommunicationHandler.LOOKING_FOR_CONTROLLERS_MESSAGE, UDPCommunicationHandler.getBroadcastAddresses());
    }

    public void addControllerToList(Controller a) {
        System.out.println(a.IP.getHostAddress());
        System.out.println("Controller added!");
        controllers.add(a);
    }
}
