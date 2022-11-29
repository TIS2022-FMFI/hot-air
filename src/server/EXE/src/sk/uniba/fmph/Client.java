package sk.uniba.fmph;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.Arrays;

public class Client {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private final static int[] LIST_OF_FREE_PORTS = {4002, 4003, 4004, 4005, 4006, 4007, 4008, 4009, 4010, 4011};
    private final static String SERVER_PASSWORD = "abcd", RECOGNIZE_ME_MESSAGE = "EXE here";
    private final static String INITIALIZE_FILE_TRANSFER_MESSAGE = "Prepare for file transfer";
    private final static String END_OF_SEGMENT_MESSAGE = "segment is done";
//    private static enum messages {RECOGNIZE_ME_MESSAGE, INITIALIZE_FILE_TRANSFER_MESSAGE, END_OF_SEGMENT_MESSAGE}
    private final String SERVER_IP; //TODO -> look for server using udp

    public Client(String differentIp) throws ConnectException { //TODO -> check if differentIp is an IP
        SERVER_IP = differentIp;
        connectToServer();
    }

    public Client() throws ConnectException {
        this(findServerIp());
    }

    private static String findServerIp() {
        UDPCommunicationHandler.sendUDPPacket(UDPCommunicationHandler.LOOKING_FOR_SERVER_MESSAGE, UDPCommunicationHandler.getBroadcastAddresses());
        try {
            DatagramSocket socket = new DatagramSocket(4002);
            byte[] buff = new byte[4096];
            DatagramPacket packet;

            do {
                packet = new DatagramPacket(buff, buff.length);
                socket.receive(packet);
            } while (!UDPCommunicationHandler.areMessagesEqual(packet.getData(), UDPCommunicationHandler.I_AM_THE_SERVER_MESSAGE));
            socket.close();

            return packet.getAddress().getHostAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void connectToServer() throws ConnectException {
        boolean serverFound = false;
        for (int port : LIST_OF_FREE_PORTS) {
            try {
                clientSocket = new Socket(SERVER_IP, port);
                out =  new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8)), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                if (!SERVER_PASSWORD.equals(in.readLine())) { // not my server
                    clientSocket.close();
                    out.close();
                    in.close();
                    continue;
                }
                serverFound = true;
                break;
            } catch (IOException e) {
                System.out.printf("Port %d occupied, trying next one\n", port);
            }
        }
        out.println(RECOGNIZE_ME_MESSAGE);
        if (!serverFound) {
            throw new ConnectException("No server found!");
        }
    }

    public void sendMessage(String msg) throws IOException {
        sendMessage(msg, false);
    }
    public void sendMessage(String msg, boolean stayConnected) throws IOException {
        out.println(msg);
        System.out.println("Message delivered!");
        if (!stayConnected) {
            stopConnection();
        }
    }

//    public void sendMessage(messages msg) throws IOException {
//        sendMessage(msg, false);
//    }
//
//    public void sendMessage(messages msg, boolean stayConnected) throws IOException {
//        out.println(msg);
//        System.out.println("Message delivered!");
//        if (!stayConnected) {
//            stopConnection();
//        }
//    }

    public void performInit(String pathToXmlThatIsToBeSent) throws IOException {
        sendMessage(INITIALIZE_FILE_TRANSFER_MESSAGE, true);
        if (!Files.exists(Paths.get(pathToXmlThatIsToBeSent), LinkOption.NOFOLLOW_LINKS)) {
            System.err.println("No file found!");
            return;
        }
        File file = new File(pathToXmlThatIsToBeSent);
        sendMessage(file.getName(), true);
        char[] buffer = new char[4096];
        BufferedReader input = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8));
        BufferedWriter output = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));

        int count;
        while ((count = input.read(buffer)) > 0) {
            output.write(buffer, 0, count);
        }

        input.close();
        output.close();
        stopConnection();
    }

    public void performEndOfSegment(String nameOfBlock, String pathToXml) throws IOException {
        sendMessage(END_OF_SEGMENT_MESSAGE, true);
        sendMessage(nameOfBlock, true);
        sendMessage(pathToXml);
    }

    public void stopConnection() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
    }
}
