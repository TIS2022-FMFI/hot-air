package Communication;

import GUI.Project;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

/**
 * Class for handling all communication between GUI and Server
 */
public class ClientHandler {
    private final Client client;
    private String IP = "";

    /**
     * Use provided IP to connect to server
     * @param IP server IP
     */
    public ClientHandler(String IP, int PORT) throws IOException {
        this.IP = IP;
        Client.PORT = PORT;
        client = new Client(IP);
        client.start();
        Runtime.getRuntime().addShutdownHook(new ExitProcess());
    }

    /**
     * Use default port
     */
    public ClientHandler(String IP) throws IOException {
        this(IP, 4002);
    }

    /**
     * Use UDP discovery to find server
     */
    public ClientHandler(int PORT) throws IOException {
        Client.PORT = PORT;
        client = new Client();
        client.start();
        Runtime.getRuntime().addShutdownHook(new ExitProcess());
    }

    /**
     * Use default port
     */
    public ClientHandler() throws IOException {
        this(4002);
    }

//    /**
//     * Try to reconnect to server
//     */
//    public void reconnect() throws IOException {
//        if (!client.isConnected()) {
//            if (IP.equals("")) {
//                client = new Client();
//            } else {
//                client = new Client(IP);
//            }
//        }
//    }

//    /**
//     * change ID of a specific controller
//     * @param oldID current ID of said controller
//     * @param newID new ID to replace it
//     */
//    public void changeControllerID(String oldID, String newID) throws IOException {
//        if (!client.isConnected()) {
//            throw new ConnectException("Disconnected from server");
//        }
//        if (!newID.matches("\\A\\p{ASCII}*\\z")) {
//            throw new SocketException("Non ascii characters found!");
//        }
//        client.writeMessage(new Message(MessageBuilder.GUI.Request.ChangeControllerID.build()));
//        client.writeMessage(new Message(oldID.getBytes(StandardCharsets.US_ASCII)));
//        client.writeMessage(new Message(newID.getBytes(StandardCharsets.US_ASCII)));
//    }

    /**
     * @return number of active projects
     */
    public int getNumberOfProjects() throws IOException, InterruptedException {
        if (!client.isConnected()) {
            throw new ConnectException("Disconnected from server");
        }
        int result;
        synchronized (RequestResult.getInstance()) {
            client.writeMessage(new Message(MessageBuilder.GUI.Request.NumberOfProjects.build()));
            RequestResult rr = RequestResult.getInstance();
            rr.wait();
            result = rr.getIntData();
        }
        return result;
    }

    /**
     * @return number of connected controllers(Blowers)
     */
    public int getNumberOfControllers() throws IOException, InterruptedException {
        if (!client.isConnected()) {
            throw new ConnectException("Disconnected from server");
        }
        int result;
        synchronized (client) {
            client.writeMessage(new Message(MessageBuilder.GUI.Request.NumberOfControllers.build()));
            RequestResult rr = RequestResult.getInstance();
            rr.wait();
            result = rr.getIntData();
        }
        return result;
    }

    /**
     * Server will try to discover new controllers(Blowers)
     */
    public void searchForNewControllers() throws IOException {
        if (!client.isConnected()) {
            throw new ConnectException("Disconnected from server");
        }
        synchronized (client) {
            client.writeMessage(new Message(MessageBuilder.GUI.Request.SearchForNewControllers.build()));
        }
    }

    /**
     * Stop all controllers(Blowers)
     */
    public void stopAllControllers() throws IOException {
        if (!client.isConnected()) {
            throw new ConnectException("Disconnected from server");
        }
        synchronized (client) {
            client.writeMessage(new Message(MessageBuilder.GUI.Request.BigRedButton.build()));
        }
    }

    /**
     * Stop controller(Blower) with this ID
     */
    public void stopAController(String ID) throws IOException {
        if (!client.isConnected()) {
            throw new ConnectException("Disconnected from server");
        }
        if (!ID.matches("\\A\\p{ASCII}*\\z")) {
            throw new SocketException("Non ascii characters found!");
        }
        synchronized (client) {
            client.writeMessage(new Message(MessageBuilder.GUI.Request.StopThisController.build()));
            client.writeMessage(new Message(ID.getBytes(StandardCharsets.US_ASCII)));
        }
    }

    /**
     * @return IP, ID, currentTemperature, targetTemperature, airFlow, time for each connected controller(Blower)
     */
    public RequestResult.Controller[] getAllControllers() throws IOException, InterruptedException {
        if (!client.isConnected()) {
            throw new ConnectException("Disconnected from server");
        }
        RequestResult.Controller[] res;
        synchronized (client) {
            client.writeMessage(new Message(MessageBuilder.GUI.Request.GetInfoAboutControllers.build()));
            RequestResult rr = RequestResult.getInstance();
            rr.wait();
            res = rr.getControllers();
        }
        return res;
    }

    /**
     * @return array of GUI.Project objects with initialized values from server
     */
    public Project[] getAllProjects() throws IOException, InterruptedException {
        if (!client.isConnected()) {
            throw new ConnectException("Disconnected from server");
        }
        Project[] res;
        synchronized (client) {
            client.writeMessage(new Message(MessageBuilder.GUI.Request.GetInfoAboutProjects.build()));
            RequestResult rr = RequestResult.getInstance();
            rr.wait();
            res = rr.getProjects();
        }
        return res;
    }

    public void unlockController(String id) throws IOException {
        if (!client.isConnected()) {
            throw new ConnectException("Disconnected from server");
        }
        if (!id.matches("\\A\\p{ASCII}*\\z")) {
            throw new SocketException("Non ascii characters found!");
        }
        synchronized (client) {
            client.writeMessage(new Message(MessageBuilder.GUI.Request.UnlockThisController.build()));
            client.writeMessage(new Message(id.getBytes(StandardCharsets.US_ASCII)));
        }
    }

    private class ExitProcess extends Thread {
        @Override
        public void run() {
            try {
                client.stopConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}