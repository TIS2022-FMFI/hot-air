package Burniee.Communication;

import Burniee.Controller.Controller;
import Burniee.Controller.ControllerException;
import Burniee.Project.Project;
import Burniee.Project.ProjectException;
import Burniee.Server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class GUIHandler extends Thread {
    private final SocketHandler socket;

    public GUIHandler(SocketHandler sh) {
        socket = sh;
        Server.getInstance().addGUI(this);
    }

    public SocketHandler getSocket() {return socket;}

    public void stopSocket() {
        socket.stopSocket();
        Server.getInstance().removeGUI(this);
    }

//    private byte[] getObjectBytes(Object o) throws IOException {
//        ByteArrayOutputStream bao = new ByteArrayOutputStream();
//        ObjectOutputStream oo = new ObjectOutputStream(bao);
//        oo.writeObject(o);
//        return bao.toByteArray();
//    }

    private void sendController(Controller c) throws IOException {
        System.out.println("[GUI] sending info about controller with id = " + c.getID() + " and current temperature = " + c.getCurrentTemperature());
        socket.writeMessage(new Message(c.getIP().getAddress()));
        socket.writeMessage(new Message(c.getID().getBytes()));
        socket.writeMessage(new Message(ByteBuffer.allocate(4).putFloat(c.getCurrentTemperature()).array()));
        socket.writeMessage(new Message(ByteBuffer.allocate(4).putInt(c.getTargetTemperature()).array()));
        socket.writeMessage(new Message(ByteBuffer.allocate(2).putShort(c.getAirFlow()).array()));
        socket.writeMessage(new Message(ByteBuffer.allocate(8).putLong(c.getTime()).array()));
        if (c.getProjectName() == null) {
            socket.writeMessage(new Message(new byte[]{(byte)0}));
        } else {
            socket.writeMessage(new Message(c.getProjectName().getBytes()));
        }
    }

    public void sendException(String className, String message, byte[] exception) throws IOException {
        socket.writeMessage(new Message(MessageBuilder.GUI.Exception.build()));
        socket.writeMessage(new Message(className.getBytes(StandardCharsets.UTF_8)));
        socket.writeMessage(new Message(message.getBytes(StandardCharsets.UTF_8)));
        socket.writeMessage(new Message(exception));
    }

    public void updateTemperature() throws IOException {
        socket.writeMessage(new Message(MessageBuilder.GUI.Request.TemperatureChanged.build()));
    }

    @Override
    public void run() {
        byte[] msg;
        while (socket.isActive()) {
            try {
                System.out.println("[GUI] new message arrived");
                msg = socket.readMessage();
                if (MessageBuilder.GUI.Request.NumberOfControllers.equals(msg)) {
                    System.out.println("[GUI] request for number of controllers, result = "+ Server.getInstance().getControllers().size());
                    socket.writeMessage(new Message(MessageBuilder.GUI.Request.NumberOfControllers.build()));
                    socket.writeMessage(new Message(ByteBuffer.allocate(4).putInt(Server.getInstance().getControllers().size()).array()));
                } else if (MessageBuilder.GUI.Request.NumberOfProjects.equals(msg)) {
                    System.out.println("[GUI] request for number of projects, result = "+ Server.getInstance().getActiveProjects().size());
                    socket.writeMessage(new Message(MessageBuilder.GUI.Request.NumberOfProjects.build()));
                    socket.writeMessage(new Message(ByteBuffer.allocate(4).putInt(Server.getInstance().getActiveProjects().size()).array()));
                } else if (MessageBuilder.GUI.Request.ChangeControllerID.equals(msg)) { //TODO -> check if new id is unique
                    String oldID = socket.readStringMessage(), newID = socket.readStringMessage();
                    System.out.println("[GUI] request for new id = " + newID + " for controller with id = " + oldID);
                    for (ControllerHandler i : Server.getInstance().getControllers()) {
                        if (i.getControllerID().equals(oldID)) {
                            i.changeId(newID);
                            return;
                        }
                    }
                    throw new ControllerException("No controller with id = " + oldID + " found!");
                } else if (MessageBuilder.GUI.Request.SearchForNewControllers.equals(msg)) {
                    System.out.println("[GUI] request to search for new controllers");
                    UDPCommunicationHandler.sendUDPPacket(UDPCommunicationHandler.LOOKING_FOR_CONTROLLERS_MESSAGE, UDPCommunicationHandler.getBroadcastAddresses());
                } else if (MessageBuilder.GUI.Request.BigRedButton.equals(msg)) {
                    System.out.println("[GUI] request to stop all controllers, and end all projects");
                    for (ControllerHandler ch : Server.getInstance().getControllers()) {
                        ch.bigRedButton();
                    }
                    for (Project p : Server.getInstance().getActiveProjects()) {
                        p.end();
                    }
                } else if (MessageBuilder.GUI.Request.StopThisController.equals(msg)) {
                    String ID = socket.readStringMessage();
                    System.out.println("[GUI] request to stop controller with id = " + ID);
                    for (ControllerHandler ch : Server.getInstance().getControllers()) {
                        if (ch.getControllerID().equals(ID)) {
                            ch.bigRedButton();
                            return;
                        }
                    }
                    throw new ControllerException("No controller with ID = " + ID);
                } else if (MessageBuilder.GUI.Request.GetInfoAboutControllers.equals(msg)) {
                    System.out.println("[GUI] request for info about all " + Server.getInstance().getControllers().size() + " controllers");
                    socket.writeMessage(new Message(MessageBuilder.GUI.Request.GetInfoAboutControllers.build()));
                    socket.writeMessage(new Message(ByteBuffer.allocate(4).putInt(Server.getInstance().getControllers().size()).array()));
                    for (ControllerHandler ch : Server.getInstance().getControllers()) {
                        Controller c = ch.getController();
                        sendController(c);
//                        socket.writeMessage(new Message(getObjectBytes(c)));
                    }
                } else if (MessageBuilder.GUI.Request.GetInfoAboutProjects.equals(msg)) {
                    System.out.println("[GUI] request for info about all " + Server.getInstance().getActiveProjects().size() + " projects");
                    socket.writeMessage(new Message(MessageBuilder.GUI.Request.GetInfoAboutProjects.build()));
                    socket.writeMessage(new Message(ByteBuffer.allocate(4).putInt(Server.getInstance().getActiveProjects().size()).array()));
                    for (Project p : Server.getInstance().getActiveProjects()) {
                        socket.writeMessage(new Message(p.getID().getBytes()));
                        socket.writeMessage(new Message(ByteBuffer.allocate(8).putLong(p.getTimeSinceStart()).array()));
                        socket.writeMessage(new Message(p.getPhaseName().getBytes()));
                    }
                } else if (MessageBuilder.GUI.Request.UnlockThisController.equals(msg)) {
                    String ID = socket.readStringMessage();
                    System.out.println("[GUI] request to unlock controller with id = " + ID);
                    for (ControllerHandler ch : Server.getInstance().getControllers()) {
                        if (ch.getControllerID().equals(ID)) {
                            ch.unlock();
                            return;
                        }
                    }
                    throw new ControllerException("No controller with ID = " + ID);
                }
            } catch (SocketException e) {
                stopSocket();
            } catch (Exception e) {
                Server.getInstance().sendExceptionToAllActiveGUIs(e);
            }
        }
    }
}
