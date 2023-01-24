package Communication;

import Communication.serverExceptions.ControllerException;
import Communication.serverExceptions.ProjectException;
import Communication.serverExceptions.XMLException;
import GUI.GUI;
import GUI.Project;
import javafx.application.Platform;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Client extends Thread {
    private Socket clientSocket;
    private BufferedOutputStream out;
    private BufferedInputStream in;
    public static int PORT = 4002;
    private final static byte[] SERVER_PASSWORD = new byte[]{'a', 'b', 'c', 'd'};
//    private final static byte END_OF_MESSAGE = 4;
    private final String SERVER_IP;

    /**
     * Connect to server using differentIp
     * @param differentIp server ip
     */
    public Client(String differentIp) throws IOException {
        String IP = differentIp;
        try {
            connectToServer(IP);
        } catch (ConnectException ignored) {
            System.err.println("Connecting to server on ip = " + differentIp + " failed, trying UDP discovery");
            try {
                connectToServer("127.0.0.1"); // we need to try localhost first because 2 different applications(server and this) cannot be listening for UPD on one ip at the same time
                IP = "127.0.0.1";
            } catch (ConnectException e) {
                IP = UDPCommunicationHandler.findServerIp();
                connectToServer(IP);
            }
        } finally {
            SERVER_IP = IP;
        }
    }

    /**
     * Use UDP to find server ip
     * @throws ConnectException if connecting goes wrong
     */
    public Client() throws IOException {
        String IP = "";
        try {
            connectToServer("127.0.0.1"); // we need to try localhost first because 2 different applications(server and this) cannot be listening for UPD on one ip at the same time
            IP = "127.0.0.1";
        } catch (ConnectException e) {
            IP = UDPCommunicationHandler.findServerIp();
            connectToServer(IP);
        } finally {
            SERVER_IP = IP;
        }
    }

    public void writeMessage(Message msg) throws IOException {
        out.write(msg.getMessage());
        out.flush();
    }

    /**
     * read message from server
     * @return message from server in form of byte[]
     */
    byte[] readMessage() throws IOException {
        byte[] msgLength = new byte[4];
        for (int i = 0; i < 4; i++) {
            msgLength[i] = (byte) in.read();
            if (msgLength[i] == -1) {
                throw new SocketException("Stream has been closed");
            }
        }
        int len = ByteBuffer.wrap(msgLength).getInt();
        byte[] res = new byte[len];
        for (int i = 0; i < len; i++) {
            res[i] = (byte) in.read(); // this is required because in.read(res) ignores part of xml file for some wild reason
        }
        return res;
    }

    /**
     * read message from server and convert it to string
     * @return message from server in string form
     */
    public String readStringMessage() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] res = readMessage();
        if (res.length == 1 && res[0] == 0) {
            return null;
        }
        out.write(res);
        return out.toString();
    }

    private void connectToServer(String ip) throws IOException {
        byte[] password;
        try {
            clientSocket = new Socket(ip, PORT);
            clientSocket.setSoTimeout(10000);
            out = new BufferedOutputStream(clientSocket.getOutputStream());
            in = new BufferedInputStream(clientSocket.getInputStream());
            password = readMessage();
        } catch (ConnectException e) {
            throw new ConnectException("No server found!");
        }
        if (!Arrays.equals(SERVER_PASSWORD, password)) {
            clientSocket.close();
            out.close();
            in.close();
            throw new ConnectException("No server found!");
        }

        sendTypeOfSocketMessage();
    }

    private void sendTypeOfSocketMessage() throws IOException {
        out.write(MessageBuilder.GUI.build());
        out.flush();
    }

    public void stopConnection() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
    }

    public boolean isConnected() {
        return clientSocket.isConnected() && !clientSocket.isClosed();
    }

    private static Exception getException(String className, String message, byte[] stackTrace) throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        Class<? extends Exception> c;
        try {
            c = (Class<? extends Exception>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            if (className.contains("ControllerException")) {
                c = ControllerException.class;
            } else if (className.contains("ProjectException")) {
                c = ProjectException.class;
            } else if (className.contains("XMLException")) {
                c = XMLException.class;
            } else {
                System.out.println("[Exception] exception class used");
                c = Exception.class;
            }
        }
//        System.out.println("[Exception] " + className + ", " + message);
//        return c.getConstructor(String.class).newInstance(message);
        Exception e = c.getConstructor(String.class).newInstance(message);
        try (ByteArrayInputStream bin = new ByteArrayInputStream(stackTrace); ObjectInput in = new ObjectInputStream(bin)) {
            e.setStackTrace((StackTraceElement[]) in.readObject());
            e.printStackTrace();
        }
        return e;
    }

    private RequestResult.Controller getController() throws ClassNotFoundException, IOException {
        InetAddress ip = InetAddress.getByAddress(readMessage());
        RequestResult.Controller c = new RequestResult.Controller(ip);
        c.setID(readStringMessage());
        c.setCurrentTemperature(ByteBuffer.wrap(readMessage()).getFloat());
        c.setTargetTemperature(ByteBuffer.wrap(readMessage()).getInt());
        c.setAirFlow(ByteBuffer.wrap(readMessage()).getShort());
        c.setTime(ByteBuffer.wrap(readMessage()).getLong());
        c.setProjectName(readStringMessage());
        return c;
    }

    /**
     * Await exceptions and resolve them
     */
    @Override
    public void run() {
        try {
            clientSocket.setSoTimeout(0);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        byte[] msg;
        while (clientSocket.isConnected() && !clientSocket.isClosed()) {
            try {
                msg = readMessage();
                if (MessageBuilder.GUI.Exception.equals(msg)) {
                    Exception e = getException(readStringMessage(), readStringMessage(), readMessage());
                    Platform.runLater(() -> {GUI.gui.alert(e);});
                } else if (MessageBuilder.GUI.Request.NumberOfProjects.equals(msg)) {
                    synchronized (RequestResult.getInstance()) {
                        RequestResult.getInstance().setIntData(ByteBuffer.wrap(readMessage()).getInt());
                        RequestResult.getInstance().notify();
                    }
                } else if (MessageBuilder.GUI.Request.NumberOfControllers.equals(msg)) {
                    synchronized (RequestResult.getInstance()) {
                        RequestResult.getInstance().setIntData(ByteBuffer.wrap(readMessage()).getInt());
                        RequestResult.getInstance().notify();
                    }
                } else if (MessageBuilder.GUI.Request.GetInfoAboutControllers.equals(msg)) {
                    synchronized (RequestResult.getInstance()) {
                        int numberOfControllers = ByteBuffer.wrap(readMessage()).getInt();
                        RequestResult.Controller[] res = new RequestResult.Controller[numberOfControllers];
                        for (int i = 0; i < numberOfControllers; i++) {
                            res[i] = getController();
                        }
                        RequestResult.getInstance().setControllers(res);
                        RequestResult.getInstance().notify();
                    }
                } else if (MessageBuilder.GUI.Request.GetInfoAboutProjects.equals(msg)) {
                    synchronized (RequestResult.getInstance()) {
                        int numberOfProjects = ByteBuffer.wrap(readMessage()).getInt();
                        Project[] res = new Project[numberOfProjects];
                        for (int i = 0; i < numberOfProjects; i++) {
                            String projectID = readStringMessage();
                            long time = ByteBuffer.wrap(readMessage()).getLong();
                            String phase = readStringMessage();
                            Project p = new Project(projectID, phase);
                            res[i] = p;
                        }
                        RequestResult.getInstance().setProjects(res);
                        RequestResult.getInstance().notify();
                    }
                } else if (MessageBuilder.GUI.Request.TemperatureChanged.equals(msg)) {
                    GUI.gui.refresh();
                } else if (MessageBuilder.GUI.Request.RequestTemperatureLog.equals(msg)) {
                    synchronized (RequestResult.getInstance()) {
                        RequestResult rr = RequestResult.getInstance();
                        rr.setStringData(readStringMessage());
                        rr.setByteData(readMessage());
                        rr.notifyAll();
                    }
                } else if (MessageBuilder.GUI.Request.RequestCheckForOldLogFiles.equals(msg)) {
                    if (GUI.gui.deleteLogFiles()) {
                        writeMessage(new Message(MessageBuilder.GUI.Request.RequestCheckForOldLogFiles.build()));
                    }
                }
            } catch (SocketException e) {
                try {
                    System.err.println("Disconnected, stopping connection");
                    stopConnection();
                    Platform.runLater(() -> {GUI.gui.alert(new ConnectException("Disconnected from server!"));});
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
