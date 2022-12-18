package sk.uniba.fmph.Burnie;

import sk.uniba.fmph.Burnie.Controller.Controller;
import sk.uniba.fmph.Burnie.TCP.ControllerHandler;
import sk.uniba.fmph.Burnie.TCP.GUIHandler;
import sk.uniba.fmph.Burnie.TCP.SocketHandler;

import java.io.*;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.List;

/**
 * Main server class
 */
public class Server {
    private final static Server INSTANCE = new Server();
    public static Server getInstance() {return INSTANCE;}

    private ServerSocket serverSocket;
    public final static int PORT = 4002;

    private final List<GUIHandler> activeGUIs = new LinkedList<>();
    private final List<ControllerHandler> controllers = new LinkedList<>();

    /**
     * Constructor, find port and initialize TCP socket
     */
    private Server() {
        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Server start failed!");
            return;
        }
        UDPCommunicationHandler.getInstance().start();
        System.out.println("Connection established!");
    }

    /**
     * Start accepting clients
     */
    public void begin() {
        while (!serverSocket.isClosed()) {
            try {
                new SocketHandler(serverSocket.accept());
            } catch (IOException e) {
                System.err.println("Accepting new connection unsuccessful");
                sendExceptionToAllActiveGUIs(e);
            }
        }
    }

    /**
     * new GUI has connected to server
     * @param sh GUI
     */
    public void addGUI(GUIHandler sh) {
        synchronized (activeGUIs) {
            activeGUIs.add(sh);
        }
    }

    /**
     * A GUI has disconnected from server
     * @param sh GUI
     */
    public void removeGUI(GUIHandler sh) {
        synchronized (activeGUIs) {
            activeGUIs.remove(sh);
        }
    }

    /**
     * Add a newly connected controller to list of controllers
     * @param ch handler for said controller
     */
    public void addController(ControllerHandler ch) {
        synchronized (controllers) {
            controllers.add(ch);
        }
    }

    /**
     * Remove a controller that suddenly disconnected from server
     * @param ch handler for said controller
     */
    public void removeController(ControllerHandler ch) {
        synchronized (controllers) {
            controllers.remove(ch);
        }
    }

    public List<ControllerHandler> getControllers() {return controllers;}

    /**
     * An exception has arrisen in server or other parts, and we will attempt to send it to any active GUI
     * @param th the exception
     */
    public synchronized void sendExceptionToAllActiveGUIs(Throwable th) {
        try {
            String c = th.getClass().getCanonicalName();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oo = new ObjectOutputStream(baos);
            oo.writeObject(th.getStackTrace());
            byte[] exception = baos.toByteArray();
            List<GUIHandler> toRemove = new LinkedList<>();
            for (GUIHandler gui : activeGUIs) {
                if (!gui.getSocket().isActive()) {
                    toRemove.add(gui);
                    continue;
                }
                gui.sendException(c, exception);
            }
            for (GUIHandler gui : toRemove) {
                removeGUI(gui);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stop the server
     * @throws IOException when something goes wrong
     */
    public void exit() throws IOException {
        serverSocket.close();
    }
}