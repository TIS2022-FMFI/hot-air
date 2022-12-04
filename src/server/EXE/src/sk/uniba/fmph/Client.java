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
    private BufferedOutputStream out;
    private BufferedInputStream in;
    private final static int PORT = 4002;
    private final static byte[] SERVER_PASSWORD = new byte[]{'a', 'b', 'c', 'd'};
    private final static byte[] RECOGNIZE_EXE_MESSAGE = {69, 88, 69};
    private final static byte[] INITIALIZE_FILE_TRANSFER_MESSAGE = {70, 73, 76, 69};
    private final static byte[] END_OF_SEGMENT_MESSAGE = {69, 78, 68};
    private final static byte END_OF_MESSAGE = 3;
    private final String SERVER_IP;

    public Client(String differentIp) throws ConnectException { //TODO -> check if differentIp is an IP
        SERVER_IP = differentIp;
        connectToServer(SERVER_IP);
    }

    public Client() throws ConnectException {
        String IP = "";
        try {
            connectToServer("127.0.0.1");
            IP = "127.0.0.1";
        } catch (ConnectException e) {
            IP = UDPCommunicationHandler.findServerIp();
            connectToServer(IP);
        } finally {
            SERVER_IP = IP;
            System.out.println(IP);
        }
    }

    private void writeBytes(byte[] msg) throws IOException {
        out.write(msg);
        out.write(END_OF_MESSAGE);
        out.flush();
    }

    private byte[] readLine() throws IOException {
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte b = 0;
        int count = 0;
        for (; count < 4096; count++) {
            b = (byte) in.read();
            if (b == END_OF_MESSAGE || b == -1) {
                break;
            }
            buffer[count] = b;
        }
        out.write(buffer, 0, count);
        return out.toByteArray();
    }

    private String readStringLine() throws IOException {
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte b = 0;
        int count = 0;
        for (; count < 4096; count++) {
            b = (byte) in.read();
            if (b == END_OF_MESSAGE || b == -1) {
                break;
            }
            buffer[count] = b;
        }
        out.write(buffer, 0, count);
        return out.toString();
    }

    private void connectToServer(String ip) throws ConnectException {
        byte[] password = new byte[0];
        System.out.println("a");
        try {
            clientSocket = new Socket(ip, PORT);
            clientSocket.setSoTimeout(10000);
            out = new BufferedOutputStream(clientSocket.getOutputStream());
            in = new BufferedInputStream(clientSocket.getInputStream());
            System.out.println("want to read password");
            password = readLine();
            System.out.println("b");

        } catch (ConnectException e) {
            throw new ConnectException("No server found!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!Arrays.equals(SERVER_PASSWORD, password)) {
            try {
                clientSocket.close();
                out.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            throw new ConnectException("No server found!");
        }
        System.out.println("Printing exe message");
        try {
            writeBytes(RECOGNIZE_EXE_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(byte[] msg, boolean stayConnected) throws IOException {
        writeBytes(msg);
        System.out.println("Message delivered!");
        if (!stayConnected) {
            stopConnection();
        }
    }
    public void sendMessage(byte[] msg) throws IOException {sendMessage(msg, false);}
    public void sendMessage(String msg) throws IOException {sendMessage(msg.getBytes(StandardCharsets.UTF_8), false);}
    public void sendMessage(String msg, boolean stayConnected) throws IOException {sendMessage(msg.getBytes(StandardCharsets.UTF_8), stayConnected);}

    public void performInit(String pathToXmlThatIsToBeSent) throws IOException {
        sendMessage(INITIALIZE_FILE_TRANSFER_MESSAGE, true);
        if (!Files.exists(Paths.get(pathToXmlThatIsToBeSent), LinkOption.NOFOLLOW_LINKS)) {
            System.err.println("No file found!");
            return;
        }
        File file = new File(pathToXmlThatIsToBeSent);
        sendMessage(file.getName(), true);
        byte[] buffer = new byte[4096];
        BufferedInputStream input = new BufferedInputStream(Files.newInputStream(file.toPath()));
        BufferedOutputStream output = new BufferedOutputStream(clientSocket.getOutputStream());

        int count;
        System.out.println("getting ready to transfer");
        while ((count = input.read(buffer)) > 0) {
            output.write(buffer, 0, count);
            System.out.println("transferring");
        }
        System.out.println("Done");

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
