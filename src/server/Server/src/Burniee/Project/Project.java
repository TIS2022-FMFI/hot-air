package Burniee.Project;

import Burniee.Communication.ControllerHandler;
import Burniee.Logs.GeneralLogger;
import Burniee.Logs.TemperatureLogger;
import Burniee.xml.XMLException;
import org.xml.sax.SAXException;
import Burniee.Controller.ControllerException;
import Burniee.Server;
import Burniee.xml.XMLAnalyzer;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A project class -> project will come from EXE as xml with instructions on temperatures which need to be applied to each controller
 */
public class Project extends Thread {
    private final String ID;
    private long startedAt;
    private final String name;
    private final String pathToXML;
    private static final byte AIR_FLOW = 100;
    private String phaseName = "";
    private int phaseIndex = 0;
    private boolean phaseEnded = false;
    private boolean projectAtEnd = false;
    private final List<String> handlerIDs;
    private final ScheduledExecutorService logger;
    private final TemperatureLogger temperatureLogger;
    private final Map<String, ControllerHandler> handlers;
    private final List<AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<Integer, Long>>>>> jobs;
    //           ^Queue<Pair<PhaseName, List<Pair<ControllerID, Pair<Temperature, Time>>>>>

    /**
     * prepare for project
     */
    public Project(String pathToXML, String id) throws ParserConfigurationException, IOException, SAXException {
        ID = id;
        phaseIndex = 0;
        this.pathToXML = pathToXML;
        name = XMLAnalyzer.getProjectName(pathToXML);
        handlers = new HashMap<>();
        handlerIDs = new ArrayList<>(XMLAnalyzer.getAllBlowers(pathToXML));
        TemperatureLogger notFinalTemperatureLogger = null;
        List<AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<Integer, Long>>>>> notFinalJobs = null;
        logger = Executors.newScheduledThreadPool(1);

        try {
            List<AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, String>>>> script = XMLAnalyzer.XMLtoCommands(pathToXML);
            notFinalTemperatureLogger = new TemperatureLogger(name, id);
            System.out.println("[Project] starting project " + name);
            GeneralLogger.writeMessage("[Project] starting project " + name);
            for (String i : handlerIDs) {
                ControllerHandler ch = Server.getInstance().findControllerByID(i);
                if (ch == null) {
                    throw new ControllerException("No controller with such ID");
                }
                if (ch.isActive()) {
                    throw new ControllerException("Controller with ID = " + ch.getControllerID() + " is currently being used by another project");
                }
                ch.startProject(this);
                handlers.put(i, ch);
                System.out.println("[Project] controller with id = " + ch.getControllerID() + " found");
                GeneralLogger.writeMessage("[Project] controller with id = " + ch.getControllerID() + " found");
            }
            notFinalJobs = new LinkedList<>();
            List<AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<Integer, Long>>> phaseJobs;

            for (AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, String>>> phase : script) {
                phaseJobs = new LinkedList<>();
                for (AbstractMap.SimpleEntry<String, String> controller : phase.getValue()) {
                    String tempTime = controller.getValue();
                    if (!tempTime.matches("[.0-9]+\\$[.0-9]+")) {
                        throw new XMLException("Expected [.0-9]+\\$[.0-9]+, got " + tempTime);
                    }
                    String[] split = tempTime.split("\\$");
                    int temperature = Integer.parseInt(split[0]);
                    long time = (long) (Float.parseFloat(split[1]));
                    phaseJobs.add(new AbstractMap.SimpleEntry<>(controller.getKey(), new AbstractMap.SimpleEntry<>(temperature, time)));
                }
                notFinalJobs.add(new AbstractMap.SimpleEntry<>(phase.getKey(), phaseJobs));
            }
            System.out.println("[Project] job queue prepared");
            GeneralLogger.writeMessage("[Project] job queue prepared");
        } catch (Exception e) {
            end();
            GeneralLogger.writeExeption(e);
            Server.getInstance().sendExceptionToAllActiveGUIs(e);
            return;
        } finally {
            temperatureLogger = notFinalTemperatureLogger;
            jobs = notFinalJobs;
        }
        logger.scheduleAtFixedRate(() -> {
            String[] temps = new String[handlers.size()], targetTemps = new String[handlers.size()];
            for (int i = 0; i < handlers.size(); i++) {
                ControllerHandler ch = handlers.get(handlerIDs.get(i));
                if (ch != null) {
                    temps[i] = String.valueOf(ch.getController().getCurrentTemperature());
                    targetTemps[i] = String.valueOf(ch.getController().getTargetTemperature());
                }
            }
            try {
                temperatureLogger.logTemperature(phaseName, handlerIDs, Arrays.asList(temps),Arrays.asList(targetTemps));
            } catch (IOException e) {
                GeneralLogger.writeExeption(e);
                Server.getInstance().sendExceptionToAllActiveGUIs(e);
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public TemperatureLogger getLogger() {return temperatureLogger;}

    public synchronized void end() {
        projectAtEnd = true;
        System.out.println("[Project] Project with ID = " + ID + ", name = " + name + " ended");
        GeneralLogger.writeMessage("[Project] Project with ID = " + ID + ", name = " + name + " ended");
        Server.getInstance().removeProject(this);
        try {
            new File(pathToXML).delete();
        } catch (Exception ignored) {}
        for (Map.Entry<String, ControllerHandler> ch : handlers.entrySet()) {
            if (ch.getValue().getProject() != null && ch.getValue().getProject().equals(this)) {
                try {
                    ch.getValue().endProject();
                    ch.getValue().changeControllerParameters(Integer.MAX_VALUE, 0, (short) 100, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                    GeneralLogger.writeExeption(e);
                    Server.getInstance().sendExceptionToAllActiveGUIs(new ControllerException("Stopping of controller with id = " + ch.getValue().getControllerID() + " may have failed"));
                }
            }
        }
        logger.shutdown();
        this.notify();
    }

    public String getID() {return ID;}
    public long getTimeSinceStart() {return System.nanoTime()-startedAt;}
    public synchronized String getPhaseName() {return phaseName;}
    private synchronized void setPhaseName(String name) {phaseName = name;}
    private void incrementPhaseIndex() {phaseIndex++;}
    public String getProjectName() {return name;}
    public boolean isAtEnd() {return projectAtEnd;}

    public synchronized void confirmEndOfPhase() {
        phaseEnded = true;
        this.notifyAll();
    }

    private synchronized void awaitEndOfPhase() {
        while (!phaseEnded && !projectAtEnd) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                GeneralLogger.writeExeption(e);
                e.printStackTrace();
            }
        }
    }

    /**
     * start reading project phases as gathered from xml and sending temperatures to each controller actively participating on this project
     */
    @Override
    public void run() {
        try {
            if (projectAtEnd) {
                return;
            }
            startedAt = System.nanoTime();
            Server.getInstance().addProject(this);
            int temperature;
            long time = 0;
            System.out.println("[Project] Starting first job");
            GeneralLogger.writeMessage("[Project] Starting first job");
            AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<Integer, Long>>>> job;
            for (int i = 0; i < jobs.size(); i++) {
                job = jobs.get(i);
                setPhaseName(job.getKey());
                incrementPhaseIndex();
                System.out.println("[Project] Phase " + job.getKey() + " started");
                GeneralLogger.writeMessage("[Project] Phase " + job.getKey() + " started");
                for (AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<Integer, Long>> controllerJob : job.getValue()) {
                    time = controllerJob.getValue().getValue();
                    temperature = controllerJob.getValue().getKey();
                    ControllerHandler ch = handlers.get(controllerJob.getKey());
                    ch.changeControllerParameters(phaseIndex, temperature, AIR_FLOW, time);
                }
                System.out.println("[Project] Instructions sent to controller(s), awaiting end of phase confirmation");
                GeneralLogger.writeMessage("[Project] Instructions sent to controller(s), awaiting end of phase confirmation");
                phaseEnded = false;
                if (i == jobs.size()-1) {
                    System.out.println("[Project] Last phase reached");
                    GeneralLogger.writeMessage("[Project] Last phase reached");
                    sleep(time*1000);
                    return;
                }
                awaitEndOfPhase();
                if (projectAtEnd) {
                    return;
                }
                System.out.println("[Project] End of phase received, continuing to another phase");
                GeneralLogger.writeMessage("[Project] End of phase received, continuing to another phase");
            }
        } catch (Exception e) {
            e.printStackTrace();
            GeneralLogger.writeExeption(e);
            Server.getInstance().sendExceptionToAllActiveGUIs(e);
        } finally {
            end();
        }
    }
}
