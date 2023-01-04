package Burniee.Communication;

import Burniee.Project.Project;
import Burniee.Server;

import java.net.SocketException;

public class EXEHandler extends Thread {
    private final SocketHandler socket;

    public EXEHandler(SocketHandler sh) {
        socket = sh;
    }

    @Override
    public void run() {
        byte[] msg;
        while (socket.isActive()) {
            try {
                msg = socket.readMessage();
                if (MessageBuilder.EXE.FileTransfer.equals(msg)) {
                    String projectID = socket.readStringMessage();
                    String pathToFile = socket.receiveAFile();
                    Project p = new Project(pathToFile, projectID);
                    p.begin();
                    socket.stopSocket();
                } else if (MessageBuilder.EXE.EndOfSegment.equals(msg)) {
                    String id = socket.readStringMessage();
                    for (Project p : Server.getInstance().getActiveProjects()) {
                        if (p.getID().equals(id)) {
                            p.confirmEndOfPhaseForAllControllers();
                            break;
                        }
                    }
                    socket.stopSocket();
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
