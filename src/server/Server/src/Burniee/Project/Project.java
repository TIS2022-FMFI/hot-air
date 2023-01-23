package Burniee.Project;

import Burniee.Communication.ControllerHandler;
import Burniee.xml.XMLException;
import org.xml.sax.SAXException;
import Burniee.Controller.ControllerException;
import Burniee.Server;
import Burniee.xml.XMLAnalyzer;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;

public class Project extends Thread {
    private final String ID;
    private long startedAt;
    private final String name;
    private static final byte AIR_FLOW = 100;
    private String phaseName = "";
    private boolean phaseEnded = false;
    private final List<String> handlerIDs;
//    private final Map<String, ControllerHandler> handlers;
    private final Queue<AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<Integer, Long>>>>> jobs;
    //           ^Queue<Pair<PhaseName, List<Pair<ControllerID, Pair<Temperature, Time>>>>>

    public Project(String pathToXML, String id) throws ProjectException, XMLException, ParserConfigurationException, IOException, SAXException, ControllerException {
        ID = id;
        HashMap<String, List<String>> script = XMLAnalyzer.XMLtoCommands(pathToXML);
        name = XMLAnalyzer.getProjectName(pathToXML);
        System.out.println("[Project] starting project " + name);
//        handlers = new HashMap<>();
        handlerIDs = new LinkedList<>();
        jobs = new LinkedList<>();
        int numberOfPhases = script.entrySet().iterator().next().getValue().size(); //TODO change
        List<AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<Integer, Long>>> phaseJobs;
        System.out.println("[Project] searching for controllers");
        for (int i = 0; i < numberOfPhases; i++) {
            phaseJobs = new LinkedList<>();
            for (Map.Entry<String, List<String>> phase : script.entrySet()) {
                if (i == 0) {
                    if (phase.getValue().size() != numberOfPhases) {
                        throw new XMLException("Number of phases for each controller is not the same");
                    }
                    ControllerHandler ch = findControllerByID(phase.getKey());
                    if (ch == null || ch.isActive()) {
                        throw new ControllerException("Controller with ID = " + phase.getKey() + " is currently being used by another project");
                    }
                    ch.startUsing(this);
                    System.out.println("[Project] controller with id = " + ch.getControllerID() + " found");
                    handlerIDs.add(phase.getKey());
//                    handlers.put(phase.getKey(), ch);
                }
                String tempTime = phase.getValue().get(i);
                if (!tempTime.matches("[.0-9]+\\$[.0-9]+")) {
                    throw new XMLException("Expected [.0-9]+\\$[.0-9]+, got " + tempTime);
                }
                String[] split = tempTime.split("\\$");
                int temperature = Integer.parseInt(split[0]);
                long time = (long)(Float.parseFloat(split[1]));
                phaseJobs.add(new AbstractMap.SimpleEntry<>(phase.getKey(), new AbstractMap.SimpleEntry<>(temperature, time)));
            }
            jobs.add(new AbstractMap.SimpleEntry<>(String.valueOf(i), phaseJobs));
        }
        System.out.println("[Project] job queue prepared");

//        for (Map.Entry<String, List<String>> i : script.entrySet()) {
//            ControllerHandler handler = findControllerByID(i.getKey());
//            Queue<AbstractMap.SimpleEntry<Integer, Long>> queue = new LinkedList<>();
//            for (String tempTime : i.getValue()) {
//                if (!tempTime.matches("[.0-9]+\\$[.0-9]+")) {
//                    throw new XMLException("Expected [.0-9]+\\$[.0-9]+, got " + tempTime);
//                }
//                String[] split = tempTime.split("\\$");
//                int temperature = Integer.parseInt(split[0]);
//                long time = (long)(Float.parseFloat(split[1]));
//                queue.add(new AbstractMap.SimpleEntry<>(temperature, time));
//            }
//            controllers.add(new ActiveController(handler, queue, this));
//        }
    }

//    public void begin() {
//        startedAt = System.nanoTime();
//        Server.getInstance().addProject(this);
//        for (ActiveController i : controllers) {
//            if (!i.isAlive()) {
//                i.start();
//            }
//        }
//    }

    public void end() {
        System.out.println("[Project] Project with ID = " + ID + ", name = " + name + " ended");
        Server.getInstance().removeProject(this);
        for (String entry : handlerIDs) {
            ControllerHandler ch = findControllerByID(entry);
            if (ch != null) {
                ch.freeFromService();
                ch.getController().setProjectName(null);
            }
        }
//        for (Map.Entry<String, ControllerHandler> entry : handlers.entrySet()) {
//            entry.getValue().freeFromService();
//            entry.getValue().getController().setProjectName(null);
//        }
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
        while (!phaseEnded) {
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

    @Override
    public void run() {
        try {
            startedAt = System.nanoTime();
            System.out.println("[Project] project started at " + getReadableTime(startedAt));
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
            for (AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<Integer, Long>>>> job : jobs) {
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
//                sleep(time*1000); //TODO should this be here or not?
                awaitEndOfPhase();
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
