package Burniee.Communication;

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

    private static final UDPCommunicationHandler INSTANCE = new UDPCommunicationHandler();
    private UDPCommunicationHandler() {
        System.out.println("[UDP] Attempting to start UDP socket");
        socket = null;
        createConnection();
    }
    public static UDPCommunicationHandler getInstance() {return INSTANCE;}

    private void createConnection() {
        for (int i = 0; i < 5; i++) {
            try {
                socket = new DatagramSocket(Server.PORT);
                break;
            } catch (IOException e) {
                e.printStackTrace();
                if (i == 4) {
                    GeneralLogger.writeExeption(e);
                    System.err.println("[UDP] socket failed to start, trying again in 1 minute");
                    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                    scheduler.schedule(this::createConnection, 1, TimeUnit.MINUTES);
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
    }

    /**
     * We shall collect broadcast addresses from all interfaces of local network
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
                        broadcastList.add(broadcast);
                    }
                }
            }
        } catch (SocketException e) {
            GeneralLogger.writeExeption(e);
            e.printStackTrace();
        }
        return broadcastList;
    }

    public boolean isThereAnotherServer() {
        sendUDPPacket(LOOKING_FOR_SERVER_MESSAGE, getBroadcastAddresses());
        System.out.println("[Server] check if another server is running");
        try {
            socket.setSoTimeout(500);
            byte[] buff = new byte[4096];
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
                socket.send(new DatagramPacket(data, data.length, b, 4002));
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
     * Thread run method, await arrival of a UDP packet and respond to it, currently supported:
     * LOOKING_FOR_SERVER_MESSAGE -> client is looking on broadcast for server, server will send him its ip
     */
    @Override
    public void run() {
        byte[] buffer = new byte[MAX_UDP_PACKET_SIZE];
        try {
            socket.setSoTimeout(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                if (socket == null) {
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        continue;
                    }
                    continue;
                }
                System.out.println("[UDP] awaiting arrival of a packet");
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                System.out.println("[UDP] packet arrived");

                if (areMessagesEqual(packet.getData(), LOOKING_FOR_SERVER_MESSAGE)) {
                    System.out.println("[UDP] packet is looking for server");
                    sendUDPPacket(I_AM_THE_SERVER_MESSAGE, Collections.singletonList(packet.getAddress()));
                    sendUDPPacket(I_AM_THE_SERVER_MESSAGE, getBroadcastAddresses()); //one of them will work
                }

            } catch (IOException e) {
                GeneralLogger.writeExeption(e);
                e.printStackTrace();
            }
        }
    }

    public void stopSocket() {
        socket.close();
    }
}