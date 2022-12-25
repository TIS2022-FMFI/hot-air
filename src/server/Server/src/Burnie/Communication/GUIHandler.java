package Burnie.Communication;

import Burnie.Server;

import java.io.IOException;
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
