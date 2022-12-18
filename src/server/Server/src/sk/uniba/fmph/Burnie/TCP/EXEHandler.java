package sk.uniba.fmph.Burnie.TCP;

import sk.uniba.fmph.Burnie.Server;

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
                    socket.receiveAFile();
                    socket.stopSocket();
                } else if (MessageBuilder.EXE.EndOfSegment.equals(msg)) {
                    String segmentName = socket.readStringMessage(), id = socket.readStringMessage();
                    System.out.println("Segment named " + segmentName + " ended, id = " + id);
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
