package sk.uniba.fmph.Burnie.TCP;

import sk.uniba.fmph.Burnie.xml.FileReceiver;

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
     * @throws IOException when something goes wrong
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

    public byte[] readMessage() throws IOException {
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
     * read special(it has a fixed length) message from controller
     * @param ignored readMessage() overload
     * @return byte[] -> message from controller
     * @throws IOException TODO
     */
    public byte[] readMessage(boolean ignored) throws IOException {
        final int MESSAGE_LENGTH = 16;
        byte[] res = new byte[MESSAGE_LENGTH];
        int count = in.read(res);
        if (count == -1) {
            throw new SocketException("Stream has been closed");
        }
        if (count != MESSAGE_LENGTH) {
            throw new SocketException("Bad packet received, expected length " + MESSAGE_LENGTH + "got " + count);
        }
        return res;
    }

    public String readStringMessage() throws IOException {
        byte[] msgLength = new byte[4];
        for (int i = 0; i < 4; i++) {
            msgLength[i] = (byte) in.read();
        }
        int len = ByteBuffer.wrap(msgLength).getInt();
        byte[] res = new byte[len];
        int count = in.read(res);
        if (count == -1) {
            throw new SocketException("Stream has been closed");
        }
        if (count != len) {
            throw new SocketException("Bad packet received, expected length " + len + "got " + count);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(res);
        return out.toString();
    }

    public void receiveAFile() throws IOException {
        FileReceiver.acceptFile(in, readStringMessage());
    }
}
