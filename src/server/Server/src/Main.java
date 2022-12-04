import sk.uniba.fmph.Controller.ControllerCommunicationHandler;
import sk.uniba.fmph.Server;

public class Main {
    public static void main(String[] args) {
        Server.getInstance().begin();
        ControllerCommunicationHandler.getInstance().requestControllerIps();
    }
}
