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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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
//        Server.getInstance().isAnyoneMissingMe(this);
    }

    public boolean isConnected() {return communicator.isConnected();}
    public Project getProject() {return controller.getProject();}
    public void setProject(Project p) {controller.setProject(p);}

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

//    public void stopConnection() {
//        if (project != null) {
//            System.out.println("[Controller] stopping connection");
//            project.end();
//        }
//        Server.getInstance().removeController(this);
//        socket.stopSocket();
//    }

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

//    public void changeId(String newId) throws ControllerException, IOException {
//        System.out.println("[Controller] ID change, new id = " + newId + " old id = " + controller.getID());
//        GeneralLogger.writeMessage("[Controller] ID change, new id = " + newId + " old id = " + controller.getID());
//        if (newId.length() > 15) {
//            throw new ControllerException("new id too long");
//        }
//        if (!newId.matches("\\A\\p{ASCII}*\\z")) {
//            throw new ControllerException("Non ascii characters found!");
//        }
//        byte[] id = newId.getBytes(StandardCharsets.US_ASCII);
//        byte[] message = new byte[16];
//        for (int i = 0; i < message.length; i++) {
//            if (i == 15) {
//                message[i] = 0b00000010;
//            } else if (i < id.length) {
//                message[i] = id[i];
//            } else {
//                message[i] = 0;
//            }
//        }
//        socket.writeMessage(new Message(message, true));
//    }

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

//package Burniee.Communication;
//
//import Burniee.Controller.Controller;
//import Burniee.Controller.ControllerException;
//import Burniee.Logs.GeneralLogger;
//import Burniee.Project.Project;
//import Burniee.Server;
//
//import java.io.IOException;
//import java.net.InetAddress;
//import java.net.SocketException;
//import java.net.SocketTimeoutException;
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.nio.charset.StandardCharsets;
//import java.util.Arrays;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//
//public class ControllerHandler extends Thread {
//    private final SocketHandler socket;
//    private final Controller controller;
//    private boolean isActive = false;
//    private boolean activeStateChangeDelay = false;
//    private Project project;
//
//    public ControllerHandler(SocketHandler sh, InetAddress ip) {
//        socket = sh;
//        controller = new Controller(ip);
//        Server.getInstance().addController(this);
////        Server.getInstance().isAnyoneMissingMe(this);
//    }
//
//    public boolean isConnected() {return socket.isActive();}
//    public Project getProject() {return project;}
//    public void setProject(Project p) {project = p;}
//    public boolean isActive() {
//        if (isActive && (project == null || project.isAtEnd())) {
//            isActive = false;
//        }
//        return isActive;
//    }
//
////    public synchronized void override(Project p) {
////        isActive = true;
////        project = p;
////    }
//
//    public synchronized void startUsing(Project p) {
////        System.out.println("[Controller] free from service attempt");
////        GeneralLogger.writeMessage("[Controller] free from service attempt");
////        if (activeStateChangeDelay) {return;}
////        System.out.println("[Controller] free from service success");
////        GeneralLogger.writeMessage("[Controller] free from service success");
//        isActive = true;
//        project = p;
//        controller.setProjectName(p.getProjectName());
////        activeStateChangeDelay = true;
////        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
////        scheduler.schedule(() -> {
////            activeStateChangeDelay = false;
////        }, 10, TimeUnit.SECONDS);
//    }
//
//    public synchronized void freeFromService() {
////        System.out.println("[Controller] free from service attempt");
////        GeneralLogger.writeMessage("[Controller] free from service attempt");
////        if (activeStateChangeDelay) {return;}
////        System.out.println("[Controller] free from service success");
////        GeneralLogger.writeMessage("[Controller] free from service success");
//        isActive = false;
//        if (project != null) {
//            project.end();
//        }
//        controller.setTargetTemperature(0);
////        activeStateChangeDelay = true;
////        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
////        scheduler.schedule(() -> {
////            activeStateChangeDelay = false;
////        }, 10, TimeUnit.SECONDS);
//    }
//
//    public void stopConnection() {
//        if (project != null) {
//            System.out.println("[Controller] stopping connection");
//            project.end();
//        }
//        Server.getInstance().removeController(this);
//        socket.stopSocket();
//    }
//
//    private String resolveId(byte[] msg) {
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < 15; i++) {
//            if (msg[i] == '\0') {
//                break;
//            }
//            sb.append((char) msg[i]);
//        }
//        return sb.toString();
//    }
//
//    public String getControllerID() {return controller.getID();}
//    public Controller getController() {return controller;}
//
//    public void changeId(String newId) throws ControllerException, IOException {
//        System.out.println("[Controller] ID change, new id = " + newId + " old id = " + controller.getID());
//        GeneralLogger.writeMessage("[Controller] ID change, new id = " + newId + " old id = " + controller.getID());
//        if (newId.length() > 15) {
//            throw new ControllerException("new id too long");
//        }
//        if (!newId.matches("\\A\\p{ASCII}*\\z")) {
//            throw new ControllerException("Non ascii characters found!");
//        }
//        byte[] id = newId.getBytes(StandardCharsets.US_ASCII);
//        byte[] message = new byte[16];
//        for (int i = 0; i < message.length; i++) {
//            if (i == 15) {
//                message[i] = 0b00000010;
//            } else if (i < id.length) {
//                message[i] = id[i];
//            } else {
//                message[i] = 0;
//            }
//        }
//        socket.writeMessage(new Message(message, true));
//    }
//
//    public synchronized void changeControllerParameters(int temperature, short airFlow, long time) throws IOException {
//        System.out.println("[Controller] Sending new parameters");
//        GeneralLogger.writeMessage("[Controller] Sending new parameters");
//        controller.setTime(time);
//        controller.setAirFlow(airFlow);
//        controller.setTargetTemperature(temperature);
//
//        ByteBuffer tempBuffer = ByteBuffer.allocate(4).putInt(temperature),
//                airFlowBuffer = ByteBuffer.allocate(2).putShort(airFlow),
//                timeBuffer = ByteBuffer.allocate(8).putLong(time);
//        byte[] byteTemperature = tempBuffer.array(), byteAirFlow = airFlowBuffer.array(), byteTime = timeBuffer.array();
//        byte[] message = new byte[] {0, 0, 0, 0,
//                byteTemperature[2], byteTemperature[3], //because java has no unsigned
//                byteAirFlow[1],
//                byteTime[0],byteTime[1],byteTime[2],byteTime[3],byteTime[4],byteTime[5],byteTime[6],byteTime[7],
//                (byte) 0b00000001};
//        socket.writeMessage(new Message(message, true));
//    }
//
//    /**
//     * Stop controller after big red button has been pressed
//     */
//    public synchronized void bigRedButton() throws IOException {
//        System.out.println("[Controller] Stopping controller with id = " + controller.getID());
//        GeneralLogger.writeMessage("[Controller] Stopping controller with id = " + controller.getID());
//        controller.setStopped(true);
//        socket.writeMessage(new Message(new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0b10000000}, true));
//    }
//
//    public synchronized void unlock() throws IOException {
//        System.out.println("[Controller] Unlocking controller with id = " + controller.getID());
//        GeneralLogger.writeMessage("[Controller] Unlocking controller with id = " + controller.getID());
//        controller.setStopped(false);
//        socket.writeMessage(new Message(new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0b01000000}, true));
//    }
//
//    /**
//     * Await messages from controller, for detailed explanation, please refer to documentation
//     */
//    @Override
//    public void run() {
//        byte[] msg;
//        try {
//            socket.getSocket().setSoTimeout(5000);
//        } catch (IOException e) {e.printStackTrace();GeneralLogger.writeExeption(e);}
//        while (socket.isActive()) {
//            try {
//                msg = socket.readMessage(true);
////                System.out.println("[Controller] new message from controller arrived");
//                byte flags = msg[15];
//                if (flags == 0b0000010) {
//                    controller.setID(resolveId(msg));
//                    System.out.println("[Controller] New ID arrived = " + controller.getID());
//                    GeneralLogger.writeMessage("[Controller] New ID arrived = " + controller.getID());
//                } else if ((flags&0b00000001) == 1) {
//                    if ((flags&0b00000100) > 0) {
//                        if (controller.getActiveError() == Controller.Error.NONE) {
//                            controller.setActiveError(Controller.Error.TEMP_CAN_NOT_BE_READ);
//                            throw new ControllerException("Temperature cannot be read");
//                        } else if (controller.getActiveError() == Controller.Error.DAC_NOT_FOUND) {
//                            controller.setActiveError(Controller.Error.BOTH);
//                            throw new ControllerException("Temperature cannot be read");
//                        }
//                    } else {
//                        if (controller.getActiveError() == Controller.Error.TEMP_CAN_NOT_BE_READ) {
//                            controller.setActiveError(Controller.Error.NONE);
//                        } else if (controller.getActiveError() == Controller.Error.BOTH) {
//                            controller.setActiveError(Controller.Error.DAC_NOT_FOUND);
//                        }
//                    }
//                    if ((flags&0b00001000) > 0) {
//                        if (controller.getActiveError() == Controller.Error.NONE) {
//                            controller.setActiveError(Controller.Error.DAC_NOT_FOUND);
//                            throw new ControllerException("DAC not found");
//                        } else if (controller.getActiveError() == Controller.Error.TEMP_CAN_NOT_BE_READ) {
//                            controller.setActiveError(Controller.Error.BOTH);
//                            throw new ControllerException("DAC not found");
//                        }
//                    } else {
//                        if (controller.getActiveError() == Controller.Error.DAC_NOT_FOUND) {
//                            controller.setActiveError(Controller.Error.NONE);
//                        } else if (controller.getActiveError() == Controller.Error.BOTH) {
//                            controller.setActiveError(Controller.Error.TEMP_CAN_NOT_BE_READ);
//                        }
//                    }
////                    if ((flags&0b00010000) > 0) {
////                        if (!isActive) {
////                            System.out.println("[Controller] controller with id = " + getControllerID() + " is being used without a project");
////                            startUsing(null);
////                        }
////                    } else {
////                        if (isActive) {
////                            System.out.println("[Controller] controller with id = " + getControllerID() + " is no longer being used");
////                            freeFromService();
////                        }
////                    }
//                    byte[] temp = new byte[] {msg[11], msg[12], msg[13], msg[14]};
//                    controller.setCurrentTemperature(ByteBuffer.wrap(temp).order(ByteOrder.LITTLE_ENDIAN).getFloat());
////                    System.out.println("[Controller] temperature arrived = " + controller.getCurrentTemperature());
//                } else {
//                    throw new ControllerException("Unknown message" + Arrays.toString(msg));
//                }
//            } catch (SocketException | SocketTimeoutException e) {
//                stopConnection();
//                GeneralLogger.writeExeption(e);
//                System.out.println("[Controller] Lost connection to controller");
//                GeneralLogger.writeMessage("[Controller] Lost connection to controller");
//                Server.getInstance().sendExceptionToAllActiveGUIs(new ControllerException("Controller disconnected!"));
//            } catch (Exception e) {
//                Server.getInstance().sendExceptionToAllActiveGUIs(e);
//                GeneralLogger.writeExeption(e);
//            }
//        }
//    }
//}
