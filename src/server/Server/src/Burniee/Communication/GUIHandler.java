package Burniee.Communication;

import Burniee.Controller.Controller;
import Burniee.Controller.ControllerException;
import Burniee.Project.Project;
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

    private byte[] getObjectBytes(Object o) throws IOException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(bao);
        oo.writeObject(o);
        return bao.toByteArray();
    }

    public void sendException(String className, byte[] exception) throws IOException {
        socket.writeMessage(new Message(MessageBuilder.GUI.Exception.build()));
        socket.writeMessage(new Message(className.getBytes(StandardCharsets.UTF_8)));
        socket.writeMessage(new Message(exception));
    }

    @Override
    public void run() {
        byte[] msg;
        while (socket.isActive()) {
            try {
                msg = socket.readMessage();
                if (MessageBuilder.GUI.Request.NumberOfControllers.equals(msg)) {
                    socket.writeMessage(new Message(MessageBuilder.GUI.Request.NumberOfControllers.build()));
                    socket.writeMessage(new Message(ByteBuffer.allocate(4).putInt(Server.getInstance().getControllers().size()).array()));
                } else if (MessageBuilder.GUI.Request.NumberOfProjects.equals(msg)) {
                    socket.writeMessage(new Message(MessageBuilder.GUI.Request.NumberOfProjects.build()));
                    socket.writeMessage(new Message(ByteBuffer.allocate(4).putInt(Server.getInstance().getActiveProjects().size()).array()));
                } else if (MessageBuilder.GUI.Request.ChangeControllerID.equals(msg)) { //TODO -> check if new id is unique
                    String oldID = socket.readStringMessage(), newID = socket.readStringMessage();
                    for (ControllerHandler i : Server.getInstance().getControllers()) {
                        if (i.getControllerID().equals(oldID)) {
                            i.changeId(newID);
                            break;
                        }
                    }
                } else if (MessageBuilder.GUI.Request.SearchForNewControllers.equals(msg)) {
                    UDPCommunicationHandler.sendUDPPacket(UDPCommunicationHandler.LOOKING_FOR_CONTROLLERS_MESSAGE, UDPCommunicationHandler.getBroadcastAddresses());
                } else if (MessageBuilder.GUI.Request.BigRedButton.equals(msg)) {
                    for (ControllerHandler ch : Server.getInstance().getControllers()) {
                        ch.bigRedButton();
                    }
                    for (Project p : Server.getInstance().getActiveProjects()) {
                        p.end();
                    }
                } else if (MessageBuilder.GUI.Request.StopThisController.equals(msg)) {
                    String ID = socket.readStringMessage();
                    for (ControllerHandler ch : Server.getInstance().getControllers()) {
                        if (ch.getControllerID().equals(ID)) {
                            ch.bigRedButton();
                            return;
                        }
                    }
                    throw new ControllerException("No controller with ID = " + ID);
                } else if (MessageBuilder.GUI.Request.GetInfoAboutControllers.equals(msg)) {
                    socket.writeMessage(new Message(MessageBuilder.GUI.Request.GetInfoAboutControllers.build()));
                    socket.writeMessage(new Message(ByteBuffer.allocate(4).putInt(Server.getInstance().getControllers().size()).array()));
                    for (ControllerHandler ch : Server.getInstance().getControllers()) {
                        Controller c = ch.getController();
                        socket.writeMessage(new Message(getObjectBytes(c)));
                    }
                }
            } catch (SocketException e) {
                socket.stopSocket();
                Server.getInstance().sendExceptionToAllActiveGUIs(e);
            } catch (Exception e) {
                Server.getInstance().sendExceptionToAllActiveGUIs(e);
            }
        }
    }
}
