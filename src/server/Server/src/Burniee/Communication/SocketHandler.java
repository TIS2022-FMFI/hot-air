package Burniee.Communication;

import Burniee.xml.FileReceiver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

/**
 *  a Communication thread created for each client -> allows client->server and server->client communication
 */
public class SocketHandler {
    private final Socket socket;
    private final BufferedOutputStream out;
    private final BufferedInputStream in;
    /**
     * A 'password' server will send so client can recognize it, or try different port if wrong server is running on this one
     */
    private final static byte[] SERVER_PASSWORD = new byte[]{'a', 'b', 'c', 'd'}; //open to suggestions

    /**
     * Constructor, receive socket, create in and out communication streams, send SERVER_PASSWORD to client
     * @param s a socket to which my client is connected
     * @throws IOException when something goes wrong with socket
     */
    public SocketHandler(Socket s) throws IOException {
        socket = s;
        out = new BufferedOutputStream(socket.getOutputStream());
        in = new BufferedInputStream(socket.getInputStream());
        writeMessage(new Message(SERVER_PASSWORD));
        byte type = readTypeOfSocket();
        if (MessageBuilder.GUI.is(type)) {
            new GUIHandler(this).start();
        } else if (MessageBuilder.EXE.is(type)) {
            new EXEHandler(this).start();
        } else if (MessageBuilder.Controller.is(type)) {
            new ControllerHandler(this, s.getInetAddress()).start();
        }
    }

    /**
     * Stop this socket
     */
    public void stopSocket() {
        try {
            socket.close();
            out.close();
            in.close();
        } catch (IOException ignored) {}
    }

    public boolean isActive() {
        return socket.isConnected() && !socket.isClosed();
    }

    public void writeMessage(Message msg) throws IOException {
        out.write(msg.getMessage());
        out.flush();
    }

    public byte readTypeOfSocket() throws IOException {
        return (byte)in.read();
    }

    /**
     * Read message from socket
     * @param special if true, the message is from controller and has a fixed length
     * @return byte[] -> received message
     */
    public byte[] readMessage(boolean special) throws IOException {
        int len;
        if (special) {
            len = 16;
        } else {
            byte[] msgLength = new byte[4];
            for (int i = 0; i < 4; i++) {
                msgLength[i] = (byte) in.read();
                if (msgLength[i] == -1) {
                    throw new SocketException("Stream has been closed");
                }
            }
            len = ByteBuffer.wrap(msgLength).getInt();
        }
        byte[] res = new byte[len];
        for (int i = 0; i < len; i++) {
            res[i] = (byte) in.read(); // this is required because in.read(res) ignores part of xml file for some wild reason
        }
        return res;
    }

    /**
     * Read message from socket
     * @return byte[] -> received message
     */
    public byte[] readMessage() throws IOException {
        return readMessage(false);
    }

    /**
     * Read message from socket and convert it to string
     * @return byte[] -> received message
     */
    public String readStringMessage() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(readMessage(false));
        return out.toString();
    }

    public String receiveAFile() throws IOException {
        String filename = readStringMessage();
        return FileReceiver.acceptFile(readMessage(), filename);
//        return FileReceiver.acceptFile(in, readStringMessage());
    }
}
