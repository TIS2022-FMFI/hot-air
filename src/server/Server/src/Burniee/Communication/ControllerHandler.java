package Burniee.Communication;

import Burniee.Controller.Controller;
import Burniee.Controller.ControllerCommunicator;
import Burniee.Controller.ControllerException;
import Burniee.Logs.GeneralLogger;
import Burniee.Project.Project;
import Burniee.Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ControllerHandler extends Thread {
    private final static int CHANGE_TEMPERATURE_ACK_WAIT_DELAY_IN_MILLIS = 1000;
    private final static int BIG_RED_BUTTON_ACK_WAIT_DELAY_IN_MILLIS = 500;
    private final static int UNLOCK_ACK_WAIT_DELAY_IN_MILLIS = 500;
    private final static int CHANGE_PARAMETERS_FLAGS = 0b00000001;
    private final static int BIG_RED_BUTTON_FLAGS = 0b10000000;
    private final static int UNLOCK_FLAGS = 0b01000000;
    private final static int CAN_NOT_READ_TEMPERATURE_BIT = 0b00000100;
    private final static int DAC_NOT_FOUND_BIT = 0b00001000;

    private final ControllerCommunicator communicator;
    private final Controller controller;
    private boolean isActive = false;

    public ControllerHandler(ControllerCommunicator cc, InetAddress ip) {
        communicator = cc;
        controller = new Controller(ip);
        Server.getInstance().addController(this);
    }

    public boolean isConnected() {return communicator.isConnected();}
    public Project getProject() {return controller.getProject();}

    public boolean isActive() {
        if (isActive && (controller.getProject() == null || controller.getProject().isAtEnd())) {
            isActive = false;
        }
        return isActive;
    }

    public synchronized void startProject(Project p) {
        isActive = true;
        controller.setProject(p);
    }

    public synchronized void endProject() throws IOException {
        isActive = false;
        Project p = null;
        if (controller.getProject() != null && !controller.getProject().isAtEnd()) {
            p = controller.getProject();
        }
        controller.setTargetTemperature(0);
        controller.setProject(null);
        if (p != null) {
            p.end();
        }
    }

    private String resolveId(byte[] msg) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            if (msg[i] == '\0') {
                break;
            }
            sb.append((char) msg[i]);
        }
        return sb.toString();
    }

    public String getControllerID() {return controller.getID();}
    public Controller getController() {return controller;}

    public synchronized void changeControllerParameters(int phaseIndex, int temperature, short airFlow, long time) throws IOException {
        System.out.println("[Controller] Sending new parameters");
        GeneralLogger.writeMessage("[Controller] Sending new parameters");
        controller.setTime(time);
        controller.setAirFlow(airFlow);
        controller.setTargetTemperature(temperature);

        ByteBuffer phaseIndexBuffer = ByteBuffer.allocate(4).putInt(phaseIndex),
                tempBuffer = ByteBuffer.allocate(4).putInt(temperature),
                airFlowBuffer = ByteBuffer.allocate(2).putShort(airFlow),
                timeBuffer = ByteBuffer.allocate(8).putLong(time);
        byte[] bytePhaseIndex = phaseIndexBuffer.array(), byteTemperature = tempBuffer.array(), byteAirFlow = airFlowBuffer.array(), byteTime = timeBuffer.array();
        byte[] message = new byte[]{bytePhaseIndex[0], bytePhaseIndex[1], bytePhaseIndex[2], bytePhaseIndex[3],
                byteTemperature[2], byteTemperature[3], //because java has no unsigned
                byteAirFlow[1],
                byteTime[0], byteTime[1], byteTime[2], byteTime[3], byteTime[4], byteTime[5], byteTime[6], byteTime[7],
                (byte) CHANGE_PARAMETERS_FLAGS};
        communicator.sendPacketAndAwaitAck(message, CHANGE_TEMPERATURE_ACK_WAIT_DELAY_IN_MILLIS);
    }

    /**
     * Stop controller after big red button has been pressed
     */
    public synchronized void bigRedButton() throws IOException {
        System.out.println("[Controller] Stopping controller with id = " + controller.getID());
        GeneralLogger.writeMessage("[Controller] Stopping controller with id = " + controller.getID());
        controller.setStopped(true);
        communicator.sendPacketAndAwaitAck(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) BIG_RED_BUTTON_FLAGS}, BIG_RED_BUTTON_ACK_WAIT_DELAY_IN_MILLIS);
    }

    public synchronized void unlock() throws IOException {
        System.out.println("[Controller] Unlocking controller with id = " + controller.getID());
        GeneralLogger.writeMessage("[Controller] Unlocking controller with id = " + controller.getID());
        controller.setStopped(false);
        communicator.sendPacketAndAwaitAck(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) UNLOCK_FLAGS}, UNLOCK_ACK_WAIT_DELAY_IN_MILLIS);
    }

    public void receiveNewID(DatagramPacket packet) {
        byte[] data = packet.getData();
        controller.setID(resolveId(data));
        System.out.println("[Controller] New ID arrived = " + controller.getID());
        GeneralLogger.writeMessage("[Controller] New ID arrived = " + controller.getID());
    }

    public void receiveTemperature(DatagramPacket packet) {
        byte[] data = packet.getData();
        byte flags = data[15];
        if ((flags & CAN_NOT_READ_TEMPERATURE_BIT) > 0) {
            if (controller.getActiveError() == Controller.Error.NONE) {
                controller.setActiveError(Controller.Error.TEMP_CAN_NOT_BE_READ);
                GeneralLogger.writeExeption(new ControllerException("Temperature cannot be read"));
                Server.getInstance().sendExceptionToAllActiveGUIs(new ControllerException("Temperature cannot be read"));
            } else if (controller.getActiveError() == Controller.Error.DAC_NOT_FOUND) {
                controller.setActiveError(Controller.Error.BOTH);
                GeneralLogger.writeExeption(new ControllerException("Temperature cannot be read"));
                Server.getInstance().sendExceptionToAllActiveGUIs(new ControllerException("Temperature cannot be read"));
            }
        } else {
            if (controller.getActiveError() == Controller.Error.TEMP_CAN_NOT_BE_READ) {
                controller.setActiveError(Controller.Error.NONE);
            } else if (controller.getActiveError() == Controller.Error.BOTH) {
                controller.setActiveError(Controller.Error.DAC_NOT_FOUND);
            }
        }
        if ((flags & DAC_NOT_FOUND_BIT) > 0) {
            if (controller.getActiveError() == Controller.Error.NONE) {
                controller.setActiveError(Controller.Error.DAC_NOT_FOUND);
                GeneralLogger.writeExeption(new ControllerException("DAC not found"));
                Server.getInstance().sendExceptionToAllActiveGUIs(new ControllerException("DAC not found"));
            } else if (controller.getActiveError() == Controller.Error.TEMP_CAN_NOT_BE_READ) {
                controller.setActiveError(Controller.Error.BOTH);
                GeneralLogger.writeExeption(new ControllerException("DAC not found"));
                Server.getInstance().sendExceptionToAllActiveGUIs(new ControllerException("DAC not found"));
            }
        } else {
            if (controller.getActiveError() == Controller.Error.DAC_NOT_FOUND) {
                controller.setActiveError(Controller.Error.NONE);
            } else if (controller.getActiveError() == Controller.Error.BOTH) {
                controller.setActiveError(Controller.Error.TEMP_CAN_NOT_BE_READ);
            }
        }
        byte[] temp = new byte[]{data[11], data[12], data[13], data[14]};
        controller.setCurrentTemperature(ByteBuffer.wrap(temp).order(ByteOrder.LITTLE_ENDIAN).getFloat());
    }
}