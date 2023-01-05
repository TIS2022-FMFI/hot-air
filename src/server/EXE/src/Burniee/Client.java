package Burniee;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.Arrays;

public class Client {
    private Socket clientSocket;
    private BufferedOutputStream out;
    private BufferedInputStream in;
    private int PORT;
    private final static byte[] SERVER_PASSWORD = new byte[]{'a', 'b', 'c', 'd'};
    private final String SERVER_IP;

    /**
     * Connect to server using this port
     * @throws ConnectException if connecting goes wrong
     */
    public Client(int port) throws ConnectException {
        PORT = port;
        String IP = "";
        try {
            connectToServer("127.0.0.1");
            IP = "127.0.0.1";
        } catch (ConnectException e) {
            IP = UDPCommunicationHandler.findServerIp();
            connectToServer(IP);
        } finally {
            SERVER_IP = IP;
        }
    }

    /**
     * Use UDP to find server ip
     * @throws ConnectException if connecting goes wrong
     */
    public Client() throws ConnectException {
        this(4002);
    }

    private void writeMessage(Message msg) throws IOException {
        out.write(msg.getMessage());
        out.flush();
    }

    /**
     * read message from server
     * @return message from server in form of byte[]
     */
    private byte[] readMessage() throws IOException {
        byte[] msgLength = new byte[4];
        for (int i = 0; i < 4; i++) {
            msgLength[i] = (byte) in.read();
            if (msgLength[i] == -1) {
                throw new SocketException("Stream has been closed");
            }
        }
        int len = ByteBuffer.wrap(msgLength).getInt();
        byte[] res = new byte[len];
        for (int i = 0; i < len; i++) {
            res[i] = (byte) in.read(); // this is required because in.read(res) ignores part of xml file for some wild reason
        }
        return res;
    }

    /**
     * read message from server and convert it to string
     * @return message from server in string form
     */
    private String readStringMessage() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(readMessage());
        return out.toString();
    }

    private void connectToServer(String ip) throws ConnectException {
        byte[] password = new byte[0];
        try {
            clientSocket = new Socket(ip, PORT);
            clientSocket.setSoTimeout(10000);
            out = new BufferedOutputStream(clientSocket.getOutputStream());
            in = new BufferedInputStream(clientSocket.getInputStream());
            password = readMessage();

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
        sendTypeOfSocketMessage();
    }

    /**
     * Send message to server
     * @param msg message to be sent
     * @param stayConnected if true, socket will remain connected to server
     */
    public void sendMessage(byte[] msg, boolean stayConnected) throws IOException {
        writeMessage(new Message(msg));
        if (!stayConnected) {
            stopConnection();
        }
    }
    public void sendMessage(byte[] msg) throws IOException {sendMessage(msg, true);}
    public void sendMessage(String msg) throws IOException {sendMessage(msg.getBytes(StandardCharsets.UTF_8), true);}
    public void sendMessage(String msg, boolean stayConnected) throws IOException {sendMessage(msg.getBytes(StandardCharsets.UTF_8), stayConnected);}
    public void sendTypeOfSocketMessage() {
        try {
            out.write(MessageBuilder.EXE.build());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send a xml script to server and start new project
     * @param pathToXmlThatIsToBeSent path to the xml script
     */
    public void performInit(String pathToXmlThatIsToBeSent) throws IOException {
        sendMessage(MessageBuilder.EXE.FileTransfer.build());
        sendMessage(pathToXmlThatIsToBeSent);
        if (!Files.exists(Paths.get(pathToXmlThatIsToBeSent), LinkOption.NOFOLLOW_LINKS)) {
            System.err.println("No file found!");
            return;
        }
        File file = new File(pathToXmlThatIsToBeSent);
        sendMessage(file.getName());
        byte[] bytes = Files.readAllBytes(file.toPath());
        sendMessage(bytes);

        stopConnection();
    }

    /**
     * Inform server that a project segment has come to an end
     * @param pathToXml id by which we will distinguish projects
     */
    public void performEndOfSegment(String pathToXml) throws IOException {
        sendMessage(MessageBuilder.EXE.EndOfSegment.build());
        sendMessage(pathToXml, false);
    }

    public void stopConnection() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
    }
}
