package Burniee.Communication;

import Burniee.Controller.Controller;
import Burniee.Controller.ControllerException;
import Burniee.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ControllerHandler extends Thread {
    private final SocketHandler socket;
    private final Controller controller;

    public ControllerHandler(SocketHandler sh, InetAddress ip) {
        Server.getInstance().addController(this);
        socket = sh;
        controller = new Controller(ip);
    }

    public boolean isActive() {
        return socket.isActive();
    }

    public void stopConnection() {
        Server.getInstance().removeController(this);
        socket.stopSocket();
    }

    private String resolveId(byte[] msg) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            if (msg[i] == 0) {
                break;
            }
            sb.append((char) msg[i]);
        }
        return sb.toString();
    }

    public String getControllerID() {return controller.getID();}
    public Controller getController() {return controller;}

    public void changeId(String newId) throws ControllerException, IOException {
        if (newId.length() > 15) {
            throw new ControllerException("new id too long");
        }
        if (!newId.matches("\\A\\p{ASCII}*\\z")) {
            throw new ControllerException("Non ascii characters found!");
        }
        byte[] id = newId.getBytes(StandardCharsets.US_ASCII);
        byte[] message = new byte[16];
        for (int i = 0; i < message.length; i++) {
            if (i == 15) {
                message[i] = 0b00000010;
            } else if (i < id.length) {
                message[i] = id[i];
            } else {
                message[i] = 0;
            }
        }
        socket.writeMessage(new Message(message, true));
    }

    public void changeControllerParameters(int temperature, short airFlow, long time) throws IOException {
        controller.setTime(time);
        controller.setAirFlow(airFlow);
        controller.setTargetTemperature(temperature);

        ByteBuffer tempBuffer = ByteBuffer.allocate(4).putInt(temperature),
                airFlowBuffer = ByteBuffer.allocate(2).putShort(airFlow),
                timeBuffer = ByteBuffer.allocate(8).putLong(time);
        byte[] byteTemperature = tempBuffer.array(), byteAirFlow = airFlowBuffer.array(), byteTime = timeBuffer.array();
        byte[] message = new byte[] {0, 0, 0, 0,
                byteTemperature[2], byteTemperature[3], //because java has no unsigned
                byteAirFlow[1],
                byteTime[0],byteTime[1],byteTime[2],byteTime[3],byteTime[4],byteTime[5],byteTime[6],byteTime[7],
                (byte) 0b00000001};
        socket.writeMessage(new Message(message, true));
    }

    /**
     * Stop controller after big red button has been pressed
     */
    public void bigRedButton() throws IOException {
        socket.writeMessage(new Message(new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0b10000000}, true));
    }

    /**
     * Await messages from controller, for detailed explanation, please refer to documentation
     */
    @Override
    public void run() {
        byte[] msg;
        while (socket.isActive()) {
            try {
                msg = socket.readMessage(true);
                byte flags = msg[15];
                if (flags == 0b0000010) {
                    controller.setID(resolveId(msg));
                    System.out.println("New ID arrived = " + controller.getID());
                } else if ((flags&0b00000001) == 1) {
                    if ((flags&0b00000100) > 0) {
                        throw new ControllerException("Temperature cannot be read");
                    }
                    if ((flags&0b00001000) > 0) {
                        throw new ControllerException("DAC not found");
                    }
                    if ((flags&0b00010000) > 0) {
                        throw new ControllerException("All is well"); //TODO -> inform GUI that controller is still working
                    }
                    byte[] temp = new byte[] {msg[11], msg[12], msg[13], msg[14]};
                    controller.setCurrentTemperature(ByteBuffer.wrap(temp).order(ByteOrder.LITTLE_ENDIAN).getFloat());
                } else {
                    throw new ControllerException("Unknown message" + Arrays.toString(msg));
                }
            } catch (SocketException e) {
                stopConnection();
                Server.getInstance().sendExceptionToAllActiveGUIs(e);
            } catch (Exception e) {
                Server.getInstance().sendExceptionToAllActiveGUIs(e);
            }
        }
    }
}
