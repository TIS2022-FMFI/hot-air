package sk.uniba.fmph.Burnie.TCP;

import sk.uniba.fmph.Burnie.Controller.Controller;
import sk.uniba.fmph.Burnie.Server;

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
                    //TODO
                    int TEMP_FIXED_VAL = 5;
                    socket.writeMessage(new Message(MessageBuilder.GUI.Request.NumberOfProjects.build()));
                    socket.writeMessage(new Message(ByteBuffer.allocate(4).putInt(TEMP_FIXED_VAL).array()));
                } else if (MessageBuilder.GUI.Request.ChangeControllerID.equals(msg)) { //TODO -> check unique
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
