import sk.uniba.fmph.Burnie.Server;
import sk.uniba.fmph.Burnie.UDPCommunicationHandler;

public class Main {
    public static void main(String[] args) {
        UDPCommunicationHandler.sendUDPPacket(UDPCommunicationHandler.LOOKING_FOR_CONTROLLERS_MESSAGE, UDPCommunicationHandler.getBroadcastAddresses());
        Server.getInstance().begin();
    }
}
