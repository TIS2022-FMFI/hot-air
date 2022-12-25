package Burnie.Project;

import Burnie.Communication.ControllerHandler;
import Burnie.xml.XMLException;
import org.xml.sax.SAXException;
import Burnie.Controller.ControllerException;
import Burnie.Server;
import Burnie.xml.XMLAnalyzer;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;

public class Project {
    private final List<ActiveController> controllers;
    private final String ID;

    public Project(String pathToXML, String id) throws ControllerException, XMLException, ParserConfigurationException, IOException, SAXException {
        HashMap<String, List<String>> script = XMLAnalyzer.XMLtoCommands(pathToXML);
//        System.out.println("Hallo");
        ID = id;
        controllers = new LinkedList<>();
        for (Map.Entry<String, List<String>> i : script.entrySet()) {
//            System.out.println(i.toString());
            ControllerHandler handler = findControllerByID(i.getKey());
            Queue<AbstractMap.SimpleEntry<Integer, Long>> queue = new LinkedList<>();
            int a = 0;
            for (String tempTime : i.getValue()) { //TODO -> check if its temp$time or time$temp
                if (!tempTime.matches("[.0-9]+\\$[.0-9]+")) {
                    throw new XMLException("Expected [.0-9]+\\$[.0-9]+, got " + tempTime);
                }
                if (a == 5) {
                    break;
                }
                a++;
//                System.out.println(tempTime);
                String[] split = tempTime.split("\\$");
                int temperature = Integer.parseInt(split[0]);
                long time = (long)(Float.parseFloat(split[1]));
                queue.add(new AbstractMap.SimpleEntry<>(temperature, time));
            }
            controllers.add(new ActiveController(handler, queue, this));
        }
    }

    public void begin() {
        Server.getInstance().addProject(this);
        for (ActiveController i : controllers) {
            if (!i.isAlive()) {
                i.start();
            }
        }
    }

    public void end() {
        Server.getInstance().removeProject(this);
    }

    public String getID() {return ID;}

    public void confirmEndOfPhaseForAllControllers() {
        for (ActiveController controller : controllers) {
            controller.confirmEndOfPhase();
        }
    }

    private ControllerHandler findControllerByID(String id) throws ControllerException {
        List<ControllerHandler> controllers = Server.getInstance().getControllers();
        for (ControllerHandler c : controllers) {
            if (c.getControllerID().equals(id)) {
                return c;
            }
        }
        throw new ControllerException("No controller with id = " + id + ", stopping");
    }

}
