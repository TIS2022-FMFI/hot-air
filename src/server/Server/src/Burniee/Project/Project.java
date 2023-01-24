package Burniee.Project;

import Burniee.Communication.ControllerHandler;
import Burniee.Communication.UDPCommunicationHandler;
import Burniee.Logs.TemperatureLogger;
import Burniee.xml.XMLException;
import org.xml.sax.SAXException;
import Burniee.Controller.ControllerException;
import Burniee.Server;
import Burniee.xml.XMLAnalyzer;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Project extends Thread {
    private final String ID;
    private long startedAt;
    private final String name;
    private static final byte AIR_FLOW = 100;
    private String phaseName = "";
    private boolean phaseEnded = false;
    private boolean projectAtEnd = false;
    private final List<String> handlerIDs;
    private final ScheduledExecutorService logger;
    private final TemperatureLogger temperatureLogger;
//    private final Map<String, ControllerHandler> handlers;
    private final List<AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<Integer, Long>>>>> jobs;
    //           ^Queue<Pair<PhaseName, List<Pair<ControllerID, Pair<Temperature, Time>>>>>

    public Project(String pathToXML, String id) throws ProjectException, XMLException, ParserConfigurationException, IOException, SAXException, ControllerException {
        ID = id;
        List<AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, String>>>> script = XMLAnalyzer.XMLtoCommands(pathToXML);
        name = XMLAnalyzer.getProjectName(pathToXML);
        temperatureLogger = new TemperatureLogger(name);
        if (TemperatureLogger.numFilesToDelete() > 0) {
            Server.getInstance().sendRequestForDeletingOldLogFiles();
        }
        System.out.println("[Project] starting project " + name);
        handlerIDs = new LinkedList<>();
        jobs = new LinkedList<>();
        List<AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<Integer, Long>>> phaseJobs;
        System.out.println("[Project] searching for controllers");
        for (AbstractMap.SimpleEntry<String, String> controller : script.get(0).getValue()) {
            ControllerHandler ch = findControllerByID(controller.getKey());
            if (ch == null || ch.isActive()) {
                throw new ControllerException("Controller with ID = " + controller.getKey() + " is currently being used by another project");
            }
            ch.startUsing(this);
            System.out.println("[Project] controller with id = " + ch.getControllerID() + " found");
            handlerIDs.add(controller.getKey());
        }

        for (AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, String>>> phase : script) {
            phaseJobs = new LinkedList<>();
            for (AbstractMap.SimpleEntry<String, String> controller : phase.getValue()) {
                String tempTime = controller.getValue();
                if (!tempTime.matches("[.0-9]+\\$[.0-9]+")) {
                    throw new XMLException("Expected [.0-9]+\\$[.0-9]+, got " + tempTime);
                }
                String[] split = tempTime.split("\\$");
                int temperature = Integer.parseInt(split[0]);
                long time = (long)(Float.parseFloat(split[1]));
                phaseJobs.add(new AbstractMap.SimpleEntry<>(controller.getKey(), new AbstractMap.SimpleEntry<>(temperature, time)));
            }
            jobs.add(new AbstractMap.SimpleEntry<>(phase.getKey(), phaseJobs));
        }
        System.out.println("[Project] job queue prepared");

        logger = Executors.newScheduledThreadPool(1);
        logger.scheduleAtFixedRate(() -> {
            List<String> temps = new LinkedList<>();
            for (int i = 0; i < handlerIDs.size(); i++) {
                ControllerHandler ch = findControllerByID(handlerIDs.get(i));
                if (ch != null) {
                    temps.set(i, String.valueOf(ch.getController().getCurrentTemperature()));
                }
            }
            try {
                temperatureLogger.logTemeperature(phaseName, handlerIDs, temps);
            } catch (IOException e) {
                Server.getInstance().sendExceptionToAllActiveGUIs(e);
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public TemperatureLogger getLogger() {return temperatureLogger;}

    public synchronized void end() {
        projectAtEnd = true;
        System.out.println("[Project] Project with ID = " + ID + ", name = " + name + " ended");
        Server.getInstance().removeProject(this);
        for (String entry : handlerIDs) {
            ControllerHandler ch = findControllerByID(entry);
            if (ch != null) {
                ch.getController().setProjectName(null);
                ch.setProject(null);
                ch.freeFromService();
            }
        }
        logger.shutdown();
        this.notify();
    }

    public String getID() {return ID;}
    public long getTimeSinceStart() {return System.nanoTime()-startedAt;}
    public synchronized String getPhaseName() {return phaseName;}
    private synchronized void setPhaseName(String name) {phaseName = name;}

    public synchronized void confirmEndOfPhase() {
        phaseEnded = true;
        this.notifyAll();
    }

    private synchronized void awaitEndOfPhase() {
        while (!phaseEnded && !projectAtEnd) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private ControllerHandler findControllerByID(String id) {
        List<ControllerHandler> controllers = Server.getInstance().getControllers();
        for (ControllerHandler c : controllers) {
            if (c.getControllerID().equals(id)) {
                return c;
            }
        }
        return null;
    }

    private String getReadableTime(Long nanos){
        long tempSec    = nanos/(1000*1000*1000);
        long sec        = tempSec % 60;
        long min        = (tempSec /60) % 60;
        long hour       = (tempSec /(60*60)) % 24;
        long day        = (tempSec / (24*60*60)) % 24;

        return String.format("%dd %dh %dm %ds (day, hour, min, sec)", day,hour,min,sec);
    }

    private void coolAllControllers() throws IOException {
        for (String controller : handlerIDs) {
            ControllerHandler ch = findControllerByID(controller);
            if (ch != null) {
                ch.changeControllerParameters(0, (short) 100, 0);
            }
        }
    }

    @Override
    public void run() {
        try {
            startedAt = System.nanoTime();
//            System.out.println("[Project] project started at " + getReadableTime(startedAt));
            Server.getInstance().addProject(this);
            for (String entry : handlerIDs) {
                ControllerHandler ch = findControllerByID(entry);
                if (ch != null) {
                    ch.getController().setProjectName(name);
                }
            }
//            for (Map.Entry<String, ControllerHandler> entry : handlers.entrySet()) {
//                entry.getValue().getController().setProjectName(name);
//            }
            int temperature;
            long time = 0;
            System.out.println("[Project] Starting first job");
            AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<Integer, Long>>>> job;
            for (int i = 0; i < jobs.size(); i++) {
                job = jobs.get(i);
//            for (AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<Integer, Long>>>> job : jobs) {
                setPhaseName(job.getKey());
                System.out.println("[Project] Phase " + job.getKey() + " started");
                for (AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<Integer, Long>> controllerJob : job.getValue()) {
                    time = controllerJob.getValue().getValue();
                    temperature = controllerJob.getValue().getKey();
                    ControllerHandler ch = findControllerByID(controllerJob.getKey());
                    if (ch != null) {
                        ch.changeControllerParameters(temperature, AIR_FLOW, time);
                    }
                }
                System.out.println("[Project] Instructions sent to controller(s), awaiting end of phase confirmation");
                phaseEnded = false;
                if (i == jobs.size()-1) {
                    sleep(time*1000);
                    coolAllControllers();
                    return;
                }
                awaitEndOfPhase();
                if (projectAtEnd) {
                    coolAllControllers();
                    return;
                }
                System.out.println("[Project] End of phase received, continuing to another phase");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Server.getInstance().sendExceptionToAllActiveGUIs(e);
        } finally {
            end();
        }
    }
}
