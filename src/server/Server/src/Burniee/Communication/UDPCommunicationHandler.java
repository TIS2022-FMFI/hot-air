package Burniee.Communication;

import Burniee.Controller.ControllerCommunicator;
import Burniee.Logs.GeneralLogger;
import Burniee.Server;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Handle all UDP communication
 */
public class UDPCommunicationHandler extends Thread {
    private static final int MAX_UDP_PACKET_SIZE = 4096;
    public static final byte[] LOOKING_FOR_SERVER_MESSAGE = new byte[] {83, 89, 83};
    public static final byte[] I_AM_THE_SERVER_MESSAGE = new byte[] {72, 65, 76, 76, 79};
    public static final byte[] LOOKING_FOR_CONTROLLERS_MESSAGE = {0x41, 0x48, 0x4f, 0x4a, 0x2b};
    private DatagramSocket socket;
    private final static Random rnd = new Random();
    private final Map<InetAddress, ControllerCommunicator> controllers;

    private static final UDPCommunicationHandler INSTANCE = new UDPCommunicationHandler();
    private UDPCommunicationHandler() {
        controllers = new HashMap<>();
        System.out.println("[UDP] Attempting to start UDP socket");
        GeneralLogger.writeMessage("[UDP] Attempting to start UDP socket");
        socket = null;
        createConnection();
    }

    public static UDPCommunicationHandler getInstance() {return INSTANCE;}
    public void addNewController(ControllerCommunicator controller) {synchronized (controllers) {controllers.put(controller.getIP(), controller);}}

    /**
     * try to start listening for UDP packets
     */
    private void createConnection() {
        for (int i = 0; i < 5; i++) {
            try {
                socket = new DatagramSocket(Server.PORT);
                break;
            } catch (IOException e) {
                e.printStackTrace();
                if (i == 4) {
                    GeneralLogger.writeExeption(e);
                    System.err.println("[UDP] socket failed to start, trying again in 30 seconds");
                    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                    scheduler.schedule(this::createConnection, 30, TimeUnit.SECONDS);
                    return;
                } else {
                    System.err.println("[UDP] socket failed to start, trying again");
                    try {
                        sleep(rnd.nextInt(1000)+1000);
                    } catch (InterruptedException ignored) {}
                }
            }
        }
        System.out.println("[UDP] Socket started successfully");
        GeneralLogger.writeMessage("[UDP] Socket started successfully");
    }

    private static long ipToLong(InetAddress ip) {
        byte[] octets = ip.getAddress();
        long result = 0;
        for (byte octet : octets) {
            result <<= 8;
            result |= octet & 0xff;
        }
        return result;
    }


    /**
     * We shall collect broadcast addresses from all interfaces of local network (except for those in range 10.1.x.x, because we have been forbidden those)
     * @return list of broadcast addresses
     */
    public static List<InetAddress> getBroadcastAddresses() {
        List<InetAddress> broadcastList = new LinkedList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback())
                    continue;
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast != null) {
                        long ip = ipToLong(broadcast), ipMin = ipToLong(InetAddress.getByName("10.1.0.0")), ipMax = ipToLong(InetAddress.getByName("10.1.255.255"));
                        if (ip < ipMin || ip > ipMax) {
                            broadcastList.add(broadcast);
                        }
                    }
                }
            }
        } catch (SocketException | UnknownHostException e) {
            GeneralLogger.writeExeption(e);
            e.printStackTrace();
        }
        return broadcastList;
    }

    public boolean isThereAnotherServer() {
        sendUDPPacket(LOOKING_FOR_SERVER_MESSAGE, getBroadcastAddresses());
        System.out.println("[Server] check if another server is running");
        GeneralLogger.writeMessage("[Server] check if another server is running");
        try {
            if (socket == null) {
                System.out.println("[UDP] Check for another server failed");
                GeneralLogger.writeMessage("[UDP] Check for another server failed");
                return false;
            }
            socket.setSoTimeout(500);
            byte[] buff = new byte[MAX_UDP_PACKET_SIZE];
            DatagramPacket packet;
            long startTime = System.currentTimeMillis();
            while (startTime+2000 > System.currentTimeMillis()) {
                packet = new DatagramPacket(buff, buff.length);
                try {
                    socket.receive(packet);
//                    System.out.println(Arrays.toString(packet.getData()));
                    if (areMessagesEqual(packet.getData(), I_AM_THE_SERVER_MESSAGE)) {
                        return true;
                    }
                } catch (SocketTimeoutException e) {
                    sendUDPPacket(LOOKING_FOR_SERVER_MESSAGE, getBroadcastAddresses());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Send udp packet containing data to all ips
     * @param data contents of udp packet
     * @param ips addresses to which packet will be sent
     */
    public static void sendUDPPacket(byte[] data, List<InetAddress> ips) {
        try (DatagramSocket socket = new DatagramSocket()) {
            for (InetAddress b : ips) {
                socket.send(new DatagramPacket(data, data.length, b, Server.PORT));
            }
        } catch (IOException e) {
            GeneralLogger.writeExeption(e);
            e.printStackTrace();
        }
    }

    private boolean areMessagesEqual(byte[] a, byte[] b) {
        for (int i = 0; i < a.length && i < b.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Thread run method, await arrival of a UDP packet and resolve it, currently supported:
     * LOOKING_FOR_SERVER_MESSAGE -> client is looking on broadcast for server, server will send him its ip
     * other discovery messages -> ignore them (I_AM_THE_SERVER_MESSAGE and LOOKING_FOR_CONTROLLERS_MESSAGE)
     * controller packets -> always have message length of 16, send them to ControllerCommunicator according to the packets ip
     */
    @Override
    public void run() {
        byte[] buffer = new byte[MAX_UDP_PACKET_SIZE];
        while (socket == null) {
            try {
                System.out.println("[UDP] socket not activated, waiting for a minute and trying again");
                GeneralLogger.writeMessage("[UDP] socket not activated, waiting for a minute and trying again");
                sleep(31000);
            } catch (InterruptedException ignored) {}
        }
        try {
            socket.setSoTimeout(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (socket.isBound()) {
            try {
//                System.out.println("[UDP] awaiting arrival of a packet");
//                GeneralLogger.writeMessage("[UDP] awaiting arrival of a packet");
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
//                System.out.println("[UDP] packet arrived " + packet.getData()[15]);
                GeneralLogger.writeMessage("[UDP] packet arrived");

                if (areMessagesEqual(packet.getData(), LOOKING_FOR_SERVER_MESSAGE)) {
                    System.out.println("[UDP] packet is looking for server");
                    GeneralLogger.writeMessage("[UDP] packet is looking for server");
                    sendUDPPacket(I_AM_THE_SERVER_MESSAGE, Collections.singletonList(packet.getAddress()));
                    sendUDPPacket(I_AM_THE_SERVER_MESSAGE, getBroadcastAddresses()); //one of them will work
                } else if (packet.getLength() == 16 && !areMessagesEqual(packet.getData(), I_AM_THE_SERVER_MESSAGE) && !areMessagesEqual(packet.getData(), LOOKING_FOR_CONTROLLERS_MESSAGE)) {
                    synchronized (controllers) {
                        if (controllers.containsKey(packet.getAddress())) {
                            controllers.get(packet.getAddress()).resolvePacket(packet);
                        } else {
                            ControllerCommunicator cm = new ControllerCommunicator(packet.getAddress());
                            cm.resolvePacket(packet);
                            cm.start();
                        }
                    }
                }
            } catch (Exception e) {
                GeneralLogger.writeExeption(e);
                e.printStackTrace();
            }
        }
        System.out.println("[UDP] UDP socket no longer bound");
        GeneralLogger.writeMessage("[UDP] UDP socket no longer bound");
    }

    public void stopSocket() {
        socket.close();
    }
}