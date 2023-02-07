package Burniee.Controller;

import Burniee.Communication.ControllerHandler;
import Burniee.Communication.UDPCommunicationHandler;
import Burniee.Logs.GeneralLogger;
import Burniee.Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ControllerCommunicator extends Thread {
    private final static int ACK_BIT = 0b00100000;
    private final static int NEW_ID_BIT = 0b0000010;
    private final static int NEW_TEMPERATURE_ARRIVED_BIT = 0b00000001;
    private final static int MESSAGE_SIZE = 16;
    private final static int MESSAGE_FLAGS_POSITION = 15;
    private final static byte CONTROLLER_PING_MESSAGE_VALUE = (byte) 168;

    private final DatagramSocket socket;
    private final InetAddress IP;
    private final List<AbstractMap.SimpleEntry<byte[], Boolean>> awaitingAck;
    private boolean connected;
    private final ControllerHandler myHandler;
    private boolean receivedTemperature;
    private final ScheduledExecutorService scheduler;

    public ControllerCommunicator(InetAddress ip) throws IOException {
        IP = ip;
        awaitingAck = new LinkedList<>();
        socket = new DatagramSocket();
        connected = true;
        myHandler = new ControllerHandler(this, IP);
        receivedTemperature = false;
        scheduler = Executors.newScheduledThreadPool(20);
        scheduler.scheduleAtFixedRate(() -> {
            if (!hasReceivedTemperature() && isConnected()) {
                disconnect();
            }
        }, 5, 5, TimeUnit.SECONDS);
        UDPCommunicationHandler.getInstance().addNewController(this);
    }

    public InetAddress getIP() {return IP;}

    public synchronized boolean isConnected() {return connected;}
    public synchronized void disconnect() {
        System.out.println("[Controller] Lost connection to controller");
        GeneralLogger.writeMessage("[Controller] Lost connection to controller");
        Server.getInstance().sendExceptionToAllActiveGUIs(new ControllerException("Controller disconnected!"));
        connected = false;
        try {
            if (myHandler.getProject() != null) {
                myHandler.endProject();
            }
        } catch (IOException e) {
            e.printStackTrace();
            GeneralLogger.writeExeption(e);
        }
    }
    public synchronized void reconnect() {
        System.out.println("[Controller] controller reconnected");
        GeneralLogger.writeMessage("[Controller] controller reconnected");
        Server.getInstance().sendExceptionToAllActiveGUIs(new ControllerException("Controller reconnected!"));
        connected = true;
    }

    private synchronized boolean hasReceivedTemperature() {boolean tmp = receivedTemperature; receivedTemperature = false; return tmp;}
    private synchronized void receiveTemperature() {receivedTemperature = true;}

    public void sendPacket(byte[] data) throws IOException {
        socket.send(new DatagramPacket(data, data.length, IP, Server.PORT));
    }
    public void sendPacketAndAwaitAck(byte[] data, int waitForInMillis) throws IOException { //TODO debug this
        sendPacket(data);
        synchronized (awaitingAck) {
            awaitingAck.add(new AbstractMap.SimpleEntry<>(data, false));
        }
        scheduler.schedule(() -> {
            synchronized (awaitingAck) {
                for (AbstractMap.SimpleEntry<byte[], Boolean> msg : awaitingAck) {
                    if (areMessagesEqual(msg.getKey(), data)) { //TODO check if data is copied right
                        if (msg.getValue()) {
                            awaitingAck.remove(msg);
                            return;
                        }
                    }
                }
            }
            try {
                sendPacketAndAwaitAck(data, waitForInMillis);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, waitForInMillis, TimeUnit.MILLISECONDS);
    }

    private boolean areMessagesEqual(byte[] a, byte[] b) {
        for (int i = 0; i < MESSAGE_SIZE; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean isThisMyAck(byte[] msg, byte[] ack) {
        if ((ack[MESSAGE_FLAGS_POSITION]&ACK_BIT) == 0) {return false;}
        for (int i = 0; i < MESSAGE_SIZE-1; i++) {
            if (msg[i] != ack[i]) {return false;}
        }
        return true;
    }

    public void resolvePacket(DatagramPacket packet) throws ControllerException, IOException {
        if (!isConnected()) {reconnect();}
        byte[] data = packet.getData();
        if (data.length < MESSAGE_SIZE) {
            throw new ControllerException("[UDP] Packet of wrong size received");
        }
        byte flags = data[MESSAGE_FLAGS_POSITION];
        if ((flags&ACK_BIT) > 0) {
            synchronized (awaitingAck) {
                for (AbstractMap.SimpleEntry<byte[], Boolean> msg : awaitingAck) {
                    if (isThisMyAck(msg.getKey(), data)) {
                        System.out.println("[Controller] packet accepted");
                        msg.setValue(true);
                        return;
                    }
                }
            }
            System.out.println("[UDP] ack packet for non-existent message arrived = " + Arrays.toString(data));
            GeneralLogger.writeMessage("[UDP] ack packet for non-existent message arrived = " + Arrays.toString(data));
        } else if (flags == NEW_ID_BIT) {
            byte[] ackData = new byte[MESSAGE_SIZE];
            for (int i = 0; i < MESSAGE_SIZE; i++) {
                ackData[i] = (i == MESSAGE_FLAGS_POSITION) ? (byte) (data[i] | ACK_BIT) : data[i];
            }
            sendPacket(ackData);
            System.out.println("ID arrive:");
            System.out.println(packet);
            myHandler.receiveNewID(packet);
        } else if ((flags&NEW_TEMPERATURE_ARRIVED_BIT) > 0) {
            receiveTemperature();
            myHandler.receiveTemperature(packet);
        } else {
            throw new ControllerException("Unknown message" + Arrays.toString(data));
        }
    }

    @Override
    public void run() {
        while (socket.isBound()) {
            try {
//                System.out.println("[Controller] Send ping");
                if (connected) {
                    socket.send(new DatagramPacket(new byte[]{CONTROLLER_PING_MESSAGE_VALUE}, 1, IP, Server.PORT));
                }
                sleep(1000);
            } catch (InterruptedException | IOException e) {
                GeneralLogger.writeExeption(e);
                e.printStackTrace();
            }
        }
    }
}
