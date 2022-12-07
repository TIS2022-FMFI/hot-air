package sk.uniba.fmph;

import java.io.IOException;
import java.net.*;
import java.util.*;

import static java.lang.System.nanoTime;


/**
 * Handle all UDP communication
 */
public class UDPCommunicationHandler {
    public static final byte[] LOOKING_FOR_SERVER_MESSAGE = new byte[] {83, 89, 83};
    public static final byte[] I_AM_THE_SERVER_MESSAGE = new byte[] {72, 65, 76, 76, 79};

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

    public static String findServerIp() { //TODO -> try localhost first
        try {
            for (int i = 0; i < 5; i++) {
                System.out.println("sending UDP");
                sendUDPPacket(LOOKING_FOR_SERVER_MESSAGE, getBroadcastAddresses());

                DatagramSocket socket = new DatagramSocket(4002);
                socket.setSoTimeout(1000);
                byte[] buff = new byte[4096];
                DatagramPacket packet;
                int j = 0;
                do {
                    System.out.println("received message");
                    packet = new DatagramPacket(buff, buff.length);
                    try {
                        socket.receive(packet);
                    } catch (SocketTimeoutException e) {
                        System.err.println("Timeout");
                        packet = null;
                        break;
                    }
                    j++;
                } while (!areMessagesEqual(packet.getData(), I_AM_THE_SERVER_MESSAGE) && j < 5);
                socket.close();

                if (packet == null) {
                    continue;
                }

                return packet.getAddress().getHostAddress();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
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

    public static boolean areMessagesEqual(byte[] a, byte[] b) {
        for (int i = 0; i < a.length && i < b.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }
}