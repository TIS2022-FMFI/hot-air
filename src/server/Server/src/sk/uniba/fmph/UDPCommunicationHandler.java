package sk.uniba.fmph;

import java.io.IOException;
import java.net.*;
import java.util.*;


/**
 * Handle all UDP communication
 */
public class UDPCommunicationHandler extends Thread {
    private static final int MAX_UDP_PACKET_SIZE = 4096;
    public static final byte[] LOOKING_FOR_SERVER_MESSAGE = new byte[] {83, 89, 83};
    public static final byte[] I_AM_THE_SERVER_MESSAGE = new byte[] {72, 65, 76, 76, 79};
    private final DatagramSocket socket;

    private static final UDPCommunicationHandler INSTANCE = new UDPCommunicationHandler();
    private UDPCommunicationHandler() {
        DatagramSocket s = null;
        try {
            s = new DatagramSocket(Server.PORT);
        } catch (IOException e) {
            System.err.println("UDP socket failed to start");
            e.printStackTrace();
        } finally {
            socket = s;
        }
    }
    public static UDPCommunicationHandler getInstance() {return INSTANCE;}

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
            e.printStackTrace();
        }
        return broadcastList;
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
     * LOOKING_FOR_SERVER_MESSAGE -> client is looking on broadcast for server, server will send him ip
     */
    @Override
    public void run() {
        byte[] buffer = new byte[MAX_UDP_PACKET_SIZE];
        while (true) {
            try {
                if (socket == null) {
                    return;
                }
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                if (areMessagesEqual(packet.getData(), LOOKING_FOR_SERVER_MESSAGE)) {
                    sendUDPPacket(I_AM_THE_SERVER_MESSAGE, Collections.singletonList(packet.getAddress()));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopSocket() {
        socket.close();
    }
}