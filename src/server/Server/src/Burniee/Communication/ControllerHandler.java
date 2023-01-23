package Burniee.Communication;

import Burniee.Controller.Controller;
import Burniee.Controller.ControllerException;
import Burniee.Project.Project;
import Burniee.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ControllerHandler extends Thread {
    private final SocketHandler socket;
    private final Controller controller;
    private boolean isActive = false;
    private Project project;

    public ControllerHandler(SocketHandler sh, InetAddress ip) {
        socket = sh;
        controller = new Controller(ip);
        Server.getInstance().addController(this);
    }

    public boolean isConnected() {return socket.isActive();}
    public Project getProject() {return project;}
    public boolean isActive() {
        return isActive;
    }

    public synchronized void startUsing(Project p) {
        isActive = true;
        project = p;
    }

    public synchronized void freeFromService() {
        isActive = false;
        if (project != null) {
            project.end();
        }
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
        System.out.println("[Controller] ID change, new id = " + newId + " old id = " + controller.getID());
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
        System.out.println("[Controller] Sending new parameters");
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
        System.out.println("[Controller] Stopping controller with id = " + controller.getID());
        socket.writeMessage(new Message(new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0b10000000}, true));
    }

    public void unlock() throws IOException {
        System.out.println("[Controller] Unlocking controller with id = " + controller.getID());
        socket.writeMessage(new Message(new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0b01000000}, true));
    }

    /**
     * Await messages from controller, for detailed explanation, please refer to documentation
     */
    @Override
    public void run() {
        byte[] msg;
        try {
            socket.getSocket().setSoTimeout(10000);
        } catch (IOException e) {e.printStackTrace();}
        while (socket.isActive()) {
            try {
                msg = socket.readMessage(true);
//                System.out.println("[Controller] new message from controller arrived");
                byte flags = msg[15];
                if (flags == 0b0000010) {
                    controller.setID(resolveId(msg));
                    System.out.println("[Controller] New ID arrived = " + controller.getID());
                } else if ((flags&0b00000001) == 1) {
                    if ((flags&0b00000100) > 0) {
                        if (controller.getActiveError() == Controller.Error.NONE) {
                            controller.setActiveError(Controller.Error.TEMP_CAN_NOT_BE_READ);
                            throw new ControllerException("Temperature cannot be read");
                        } else if (controller.getActiveError() == Controller.Error.DAC_NOT_FOUND) {
                            controller.setActiveError(Controller.Error.BOTH);
                            throw new ControllerException("Temperature cannot be read");
                        }
                    } else {
                        if (controller.getActiveError() == Controller.Error.TEMP_CAN_NOT_BE_READ) {
                            controller.setActiveError(Controller.Error.NONE);
                        } else if (controller.getActiveError() == Controller.Error.BOTH) {
                            controller.setActiveError(Controller.Error.DAC_NOT_FOUND);
                        }
                    }
                    if ((flags&0b00001000) > 0) {
                        if (controller.getActiveError() == Controller.Error.NONE) {
                            controller.setActiveError(Controller.Error.DAC_NOT_FOUND);
                            throw new ControllerException("DAC not found");
                        } else if (controller.getActiveError() == Controller.Error.TEMP_CAN_NOT_BE_READ) {
                            controller.setActiveError(Controller.Error.BOTH);
                            throw new ControllerException("DAC not found");
                        }
                    } else {
                        if (controller.getActiveError() == Controller.Error.DAC_NOT_FOUND) {
                            controller.setActiveError(Controller.Error.NONE);
                        } else if (controller.getActiveError() == Controller.Error.BOTH) {
                            controller.setActiveError(Controller.Error.TEMP_CAN_NOT_BE_READ);
                        }
                    }
                    if ((flags&0b00010000) > 0) {
                        if (!isActive) {
                            System.out.println("[Controller] controller with id = " + getControllerID() + " is being used without a project");
                            startUsing(null);
                        }
                    } else {
                        if (isActive) {
                            System.out.println("[Controller] controller with id = " + getControllerID() + " is no longer being used");
                            freeFromService();
                        }
                    }
                    byte[] temp = new byte[] {msg[11], msg[12], msg[13], msg[14]};
                    controller.setCurrentTemperature(ByteBuffer.wrap(temp).order(ByteOrder.LITTLE_ENDIAN).getFloat());
//                    System.out.println("[Controller] temperature arrived = " + controller.getCurrentTemperature());
                } else {
                    throw new ControllerException("Unknown message" + Arrays.toString(msg));
                }
            } catch (SocketException e) {
                stopConnection();
                System.out.println("[Controller] Lost connection to controller");
//                Server.getInstance().sendExceptionToAllActiveGUIs(e);
            } catch (Exception e) {
                Server.getInstance().sendExceptionToAllActiveGUIs(e);
            }
        }
    }
}
