package Burniee;

import Burniee.Communication.ControllerHandler;
import Burniee.Communication.GUIHandler;
import Burniee.Communication.SocketHandler;
import Burniee.Communication.UDPCommunicationHandler;
import Burniee.Project.Project;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main server class
 */
public class Server {
    private final static Server INSTANCE = new Server();
    public static Server getInstance() {return INSTANCE;}

    private ServerSocket serverSocket;
    public static int PORT = 4002;
    public final static String CONFIG_FILE_NAME = "server.config";

    private final List<GUIHandler> activeGUIs = new LinkedList<>();
    private final List<ControllerHandler> controllers = new LinkedList<>();
    private final List<Project> activeProjects = new LinkedList<>();

    /**
     * Constructor, find port and initialize TCP socket
     */
    private Server() {
        Properties prop = new Properties();
        File f = new File(CONFIG_FILE_NAME);
        if (f.exists()) {
            try (FileInputStream fis = new FileInputStream(f.getName())) {
                prop.load(fis);
                PORT = Integer.parseInt(prop.getProperty("PORT"));
                System.out.println("New PORT = " + PORT);
            } catch (IOException ignored) {}
        } else {
            try (FileOutputStream fos = new FileOutputStream(f.getName())) {
                prop.setProperty("PORT", "4002");
                prop.store(fos, null);
                System.out.println("File created");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Server start failed!");
            return;
        }
        UDPCommunicationHandler.getInstance().start();
        System.out.println("Server started!");
    }

    /**
     * Start accepting clients
     */
    public void begin() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
                System.out.println("[UDP] Sending regular UDP discovery packet to broadcast");
                UDPCommunicationHandler.sendUDPPacket(UDPCommunicationHandler.LOOKING_FOR_CONTROLLERS_MESSAGE, UDPCommunicationHandler.getBroadcastAddresses());
            }, 0, 3, TimeUnit.MINUTES);
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
    public synchronized void addGUI(GUIHandler sh) {
        synchronized (activeGUIs) {
            activeGUIs.add(sh);
        }
    }

    /**
     * A GUI has disconnected from server
     * @param sh GUI
     */
    public synchronized void removeGUI(GUIHandler sh) {
        synchronized (activeGUIs) {
            activeGUIs.remove(sh);
        }
    }

    public synchronized List<GUIHandler> getAllGUIS() {return activeGUIs;}

    /**
     * Add a newly connected controller to list of controllers
     * @param ch handler for said controller
     */
    public synchronized void addController(ControllerHandler ch) {
        synchronized (controllers) {
            ControllerHandler toRemove = null;
            for (ControllerHandler i : controllers) {
                if (i.getControllerID().equals(ch.getControllerID()) &&
                    i.getController().getIP().equals(ch.getController().getIP())) {
                    toRemove = i;
                }
            }
            if (toRemove != null) {
                if (toRemove.isActive()) {
                    ch.startUsing(toRemove.getProject());
                }
                toRemove.stopConnection();
            }
            controllers.add(ch);
        }
    }

    /**
     * Remove a controller that suddenly disconnected from server
     * @param ch handler for said controller
     */
    public synchronized void removeController(ControllerHandler ch) {
        synchronized (controllers) {
            controllers.remove(ch);
        }
    }

    public synchronized List<ControllerHandler> getControllers() {return controllers;}

    /**
     * Add a newly started project to list of Projects
     * @param p Project
     */
    public synchronized void addProject(Project p) {
        synchronized (activeProjects) {
            activeProjects.add(p);
        }
    }

    /**
     * Remove a project that is at its end
     * @param p Project
     */
    public synchronized void removeProject(Project p) {
//        System.out.println("Removing project");
        synchronized (activeProjects) {
            activeProjects.remove(p);
        }
    }

    public synchronized List<Project> getActiveProjects() {return activeProjects;}

    /**
     * An exception has arrisen in server or other parts, and we will attempt to send it to any active GUI
     * @param th the exception
     */
    public void sendExceptionToAllActiveGUIs(Throwable th) {
        th.printStackTrace();
        System.out.println("[TCP] Sending exception to " + getAllGUIS().size() + " GUIs");
        try {
            String c = th.getClass().getCanonicalName();
            String msg = th.getMessage();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oo = new ObjectOutputStream(baos);
            oo.writeObject(th.getStackTrace());
            byte[] exception = baos.toByteArray();
            List<GUIHandler> toRemove = new LinkedList<>();
            synchronized (activeGUIs) {
                for (GUIHandler gui : activeGUIs) {
                    if (!gui.getSocket().isActive()) {
                        toRemove.add(gui);
                        continue;
                    }
                    gui.sendException(c, msg, exception);
                }
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