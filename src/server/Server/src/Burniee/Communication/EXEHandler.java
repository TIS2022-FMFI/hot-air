package Burniee.Communication;

import Burniee.Logs.GeneralLogger;
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
                System.out.println("[EXE] received a message from EXE");
                if (MessageBuilder.EXE.FileTransfer.equals(msg)) {
                    System.out.println("[EXE] starting a file transfer");
                    String projectID = socket.readStringMessage();
                    String pathToFile = socket.receiveAFile();
                    System.out.println("[EXE] new Project with id = " + projectID + " is about to start");
                    Project p = new Project(pathToFile, projectID);
                    p.start();
                    socket.stopSocket();
                } else if (MessageBuilder.EXE.EndOfSegment.equals(msg)) {
                    String id = socket.readStringMessage();
                    System.out.println("[EXE] End of phase message for project with id = " + id + " arrived!");
                    for (Project p : Server.getInstance().getActiveProjects()) {
                        if (p.getID().equals(id)) {
                            System.out.println("[EXE] Found the right project and confirmed end of phase");
                            p.confirmEndOfPhase();
                            socket.stopSocket();
                            return;
                        }
                    }
                    System.err.println("[EXE] Project with this id not found");
                    socket.stopSocket();
                }
            } catch (SocketException e) {
                socket.stopSocket();
                Server.getInstance().sendExceptionToAllActiveGUIs(e);
            } catch (Exception e) {
                GeneralLogger.writeExeption(e);
                Server.getInstance().sendExceptionToAllActiveGUIs(e);
            }
        }
    }
}
