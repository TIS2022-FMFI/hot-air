package Burniee;

import Burniee.Communication.ControllerHandler;
import Burniee.Communication.GUIHandler;
import Burniee.Communication.SocketHandler;
import Burniee.Communication.UDPCommunicationHandler;
import Burniee.Logs.GeneralLogger;
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
                GeneralLogger.writeMessage("New PORT = " + PORT);
            } catch (IOException ignored) {}
        } else {
            try (FileOutputStream fos = new FileOutputStream(f.getName())) {
                prop.setProperty("PORT", "4002");
                prop.store(fos, null);
                System.out.println("File created");
                GeneralLogger.writeMessage("File created");
            } catch (IOException e) {
                e.printStackTrace();
                GeneralLogger.writeExeption(e);
            }
        }
        if (UDPCommunicationHandler.getInstance().isThereAnotherServer()) {
            System.err.println("Another instance of server is running, shutting down!");
            GeneralLogger.writeMessage("Another instance of server is running, shutting down!");
            try {
                exit();
            } catch (IOException ignored) {}
        }
        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
            GeneralLogger.writeExeption(e);
            System.err.println("Server start failed!");
            return;
        }
        UDPCommunicationHandler.getInstance().start();
        System.out.println("Server started!");
        GeneralLogger.writeMessage("Server started!");
        GeneralLogger.writeMessage("Server started!");
    }

    /**
     * Start accepting clients
     */
    public void begin() {
        GeneralLogger.deleteOldLogs();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
                System.out.println("[UDP] Sending regular UDP discovery packet to broadcast");
                GeneralLogger.writeMessage("[UDP] Sending regular UDP discovery packet to broadcast");
                UDPCommunicationHandler.sendUDPPacket(UDPCommunicationHandler.LOOKING_FOR_CONTROLLERS_MESSAGE, UDPCommunicationHandler.getBroadcastAddresses());
            }, 0, 3, TimeUnit.MINUTES);
        while (!serverSocket.isClosed()) {
            try {
                new SocketHandler(serverSocket.accept());
            } catch (IOException e) {
                GeneralLogger.writeExeption(e);
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

    public List<GUIHandler> getAllGUIS() {
        List<GUIHandler> res;
        synchronized (activeGUIs) {
            res = activeGUIs;
        }
        return res;
    }

    /**
     * Add a newly connected controller to list of controllers
     * @param ch handler for said controller
     */
    public void addController(ControllerHandler ch) {
        synchronized (controllers) {
            List<ControllerHandler> toRemove = new LinkedList<>();
            for (ControllerHandler i : controllers) {
                if (i.getController().getIP().equals(ch.getController().getIP())) {
                    toRemove.add(i);
                }
            }
            for (ControllerHandler i : toRemove) {
                if (i.isActive()) {
                    i.freeFromService();
                    ch.startUsing(i.getProject());
                    try {
                        ch.changeControllerParameters(i.getController().getTargetTemperature(), i.getController().getAirFlow(), i.getController().getTime());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("[Controller] found duplicate");
                i.stopConnection();
            }
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

    public List<ControllerHandler> getControllers() {
        List<ControllerHandler> chs;
        synchronized (controllers) {
            chs = controllers;
        }
        return chs;
    }

    /**
     * Add a newly started project to list of Projects
     * @param p Project
     */
    public void addProject(Project p) {
        synchronized (activeProjects) {
            activeProjects.add(p);
        }
    }

    /**
     * Remove a project that is at its end
     * @param p Project
     */
    public void removeProject(Project p) {
//        System.out.println("Removing project");
        synchronized (activeProjects) {
            activeProjects.remove(p);
        }
    }

    public Project findProjectByName(String name) {
        for (Project p : activeProjects) {
            if (p.getProjectName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    public List<Project> getActiveProjects() {
        List<Project> res;
        synchronized (activeProjects) {
            res = activeProjects;
        }
        return res;
    }

//    public void isAnyoneMissingMe(ControllerHandler ch) {
//        synchronized (activeProjects) {
//            for (Project p : activeProjects) {
//                if (p.beYouMyLostChild(ch)) {
//                    return;
//                }
//            }
//        }
//    }

    public void sendRequestForDeletingOldLogFiles() {
        synchronized (activeGUIs) {
            for (GUIHandler gui : activeGUIs) {
                try {
                    gui.sendRequestForDeletingOldFiles();
                } catch (IOException e) {
                    GeneralLogger.writeExeption(e);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * An exception has arrisen in server or other parts, and we will attempt to send it to any active GUI
     * @param th the exception
     */
    public void sendExceptionToAllActiveGUIs(Throwable th) {
        th.printStackTrace();
        System.err.println(th.getMessage());
        System.out.println("[TCP] Sending exception to " + getAllGUIS().size() + " GUIs");
        GeneralLogger.writeMessage("[TCP] Sending exception to " + getAllGUIS().size() + " GUIs");
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
            GeneralLogger.writeExeption(e);
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