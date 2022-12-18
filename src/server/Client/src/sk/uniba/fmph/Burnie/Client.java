package sk.uniba.fmph.Burnie;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Client extends Thread {
    private Socket clientSocket;
    private BufferedOutputStream out;
    private BufferedInputStream in;
    private final static int PORT = 4002;
    private final static byte[] SERVER_PASSWORD = new byte[]{'a', 'b', 'c', 'd'};
//    private final static byte END_OF_MESSAGE = 4;
    private final String SERVER_IP;

    /**
     * Connect to server using differentIp
     * @param differentIp server ip
     * @throws ConnectException if connecting goes wrong
     */
    public Client(String differentIp) throws IOException { //TODO -> check if differentIp is an IP
        SERVER_IP = differentIp;
        connectToServer(SERVER_IP);
    }

    /**
     * Use UDP to find server ip
     * @throws ConnectException if connecting goes wrong
     */
    public Client() throws IOException {
        String IP = "";
        try {
            connectToServer("127.0.0.1"); // we need to try localhost first because 2 different applications(server and this) cannot be listening for UPD on one ip at the same time
            IP = "127.0.0.1";
        } catch (ConnectException e) {
            IP = UDPCommunicationHandler.findServerIp();
            connectToServer(IP);
        } finally {
            SERVER_IP = IP;
        }
        System.out.println("Connected!");
//        try {
//            sendMessage(MessageBuilder.GUI.Request.NumberOfProjects.build());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public void writeMessage(Message msg) throws IOException {
        out.write(msg.getMessage());
        out.flush();
    }

    /**
     * read message from server
     * @return message from server in form of byte[]
     * @throws IOException TODO
     */
    byte[] readMessage() throws IOException {
        byte[] msgLength = new byte[4];
        for (int i = 0; i < 4; i++) {
            msgLength[i] = (byte) in.read();
            if (msgLength[i] == -1) {
                throw new SocketException("Stream has been closed");
            }
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
    private String readStringMessage() throws IOException {
        byte[] msgLength = new byte[4];
        for (int i = 0; i < 4; i++) {
            msgLength[i] = (byte) in.read();
            if (msgLength[i] == -1) {
                throw new SocketException("Stream has been closed");
            }
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

    private void connectToServer(String ip) throws IOException {
        byte[] password = new byte[0];
        try {
            clientSocket = new Socket(ip, PORT);
            clientSocket.setSoTimeout(10000);
            out = new BufferedOutputStream(clientSocket.getOutputStream());
            in = new BufferedInputStream(clientSocket.getInputStream());
            password = readMessage();
        } catch (ConnectException e) {
            throw new ConnectException("No server found!");
        }
        if (!Arrays.equals(SERVER_PASSWORD, password)) {
            clientSocket.close();
            out.close();
            in.close();
            throw new ConnectException("No server found!");
        }

        sendTypeOfSocketMessage();
    }

    private void sendTypeOfSocketMessage() throws IOException {
        out.write(MessageBuilder.GUI.build());
        out.flush();
    }

    public void stopConnection() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
    }

    private static Exception getException(String className, byte[] stackTrace) throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        Class<? extends Exception> c = (Class<? extends Exception>) Class.forName(className);
        Exception e = c.getConstructor().newInstance();
        try (ByteArrayInputStream bin = new ByteArrayInputStream(stackTrace); ObjectInput in = new ObjectInputStream(bin)) {
            e.setStackTrace((StackTraceElement[]) in.readObject());
            return e;
        }
    }

    /**
     * Await exceptions and resolve them
     */
    @Override
    public void run() {
        try {
            clientSocket.setSoTimeout(0);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        byte[] msg;
        while (clientSocket.isConnected() && !clientSocket.isClosed()) {
            try {
                msg = readMessage();
                if (MessageBuilder.GUI.Exception.equals(msg)) {
                    Exception e = getException(readStringMessage(), readMessage());
                    throw e;
                } else if (MessageBuilder.GUI.Request.NumberOfProjects.equals(msg)) {
                    synchronized (RequestResult.getInstance()) {
                        RequestResult.getInstance().setIntData(ByteBuffer.wrap(readMessage()).getInt());
                        RequestResult.getInstance().notify();
                    }
                } else if (MessageBuilder.GUI.Request.NumberOfControllers.equals(msg)) {
                    synchronized (RequestResult.getInstance()) {
                        RequestResult.getInstance().setIntData(ByteBuffer.wrap(readMessage()).getInt());
                        RequestResult.getInstance().notify();
                    }
                }
            } catch (SocketException e) {
                try {
                    stopConnection();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
