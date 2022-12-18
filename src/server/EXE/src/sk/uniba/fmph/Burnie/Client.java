package sk.uniba.fmph.Burnie;

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
    private final static int PORT = 4002;
    private final static byte[] SERVER_PASSWORD = new byte[]{'a', 'b', 'c', 'd'};
    private final String SERVER_IP;

    /**
     * Connect to server using differentIp
     * @param differentIp server ip
     * @throws ConnectException if connecting goes wrong
     */
    public Client(String differentIp) throws ConnectException { //TODO -> check if differentIp is an IP
        SERVER_IP = differentIp;
        connectToServer(SERVER_IP);
    }

    /**
     * Use UDP to find server ip
     * @throws ConnectException if connecting goes wrong
     */
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
        }
    }

    private void writeMessage(Message msg) throws IOException {
        out.write(msg.getMessage());
        out.flush();
    }

    /**
     * read message from server
     * @return message from server in form of byte[]
     * @throws IOException TODO
     */
    private byte[] readLine() throws IOException {
        byte[] msgLength = new byte[4];
        for (int i = 0; i < 4; i++) {
            msgLength[i] = (byte) in.read();
        }
        int len = ByteBuffer.wrap(msgLength).getInt();
        byte[] res = new byte[len];
        int count = in.read(res);
        if (count != len) {
            throw new SocketException("Bad packet received, expected length " + len + "got " + count);
        }
        return res;
    }

    /**
     * read message from server and convert it to string
     * @return message from server in string form
     * @throws IOException TODO
     */
    private String readStringLine() throws IOException {
        byte[] msgLength = new byte[4];
        for (int i = 0; i < 4; i++) {
            msgLength[i] = (byte) in.read();
        }
        int len = ByteBuffer.wrap(msgLength).getInt();
        byte[] res = new byte[len];
        int count = in.read(res);
        if (count != len) {
            throw new SocketException("Bad packet received, expected length " + len + "got " + count);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(res);
        return out.toString();
    }

    private void connectToServer(String ip) throws ConnectException {
        byte[] password = new byte[0];
        try {
            clientSocket = new Socket(ip, PORT);
            clientSocket.setSoTimeout(10000);
            out = new BufferedOutputStream(clientSocket.getOutputStream());
            in = new BufferedInputStream(clientSocket.getInputStream());
            password = readLine();

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
     * @throws IOException TODO
     */
    public void sendMessage(byte[] msg, boolean stayConnected) throws IOException {
        writeMessage(new Message(msg));
        System.out.println("Message delivered!");
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

    public void performInit(String pathToXmlThatIsToBeSent) throws IOException {
        sendMessage(MessageBuilder.EXE.FileTransfer.build());
        if (!Files.exists(Paths.get(pathToXmlThatIsToBeSent), LinkOption.NOFOLLOW_LINKS)) {
            System.err.println("No file found!");
            return;
        }
        File file = new File(pathToXmlThatIsToBeSent);
        sendMessage(file.getName());
        byte[] buffer = new byte[4096];
        BufferedInputStream input = new BufferedInputStream(Files.newInputStream(file.toPath()));
        BufferedOutputStream output = new BufferedOutputStream(clientSocket.getOutputStream());

        int count;
//        System.out.println("getting ready to transfer");
        while ((count = input.read(buffer)) > 0) {
            output.write(buffer, 0, count);
//            System.out.println("transferring");
        }
//        System.out.println("Done");

        input.close();
        output.close();
        stopConnection();
    }

    public void performEndOfSegment(String nameOfBlock, String pathToXml) throws IOException {
        sendMessage(MessageBuilder.EXE.EndOfSegment.build());
        sendMessage(nameOfBlock);
        sendMessage(pathToXml, false);
    }

    public void stopConnection() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
    }
}
