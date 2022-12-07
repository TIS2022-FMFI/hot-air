package sk.uniba.fmph;

import sk.uniba.fmph.xml.FileReceiver;

import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Main server class
 */
public class Server { //TODO -> sort out exceptions
    private final static Server INSTANCE = new Server();
    public static Server getInstance() {return INSTANCE;}

    private ServerSocket serverSocket;
    public final static int PORT = 4002;

    /**
     * Constructor, find port and initialize TCP socket
     */
    private Server() {
        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        UDPCommunicationHandler.getInstance().start();
        System.out.println("Connection established!");
    }

    /**
     * Start accepting clients
     */
    public void begin() {
        while (!serverSocket.isClosed()) {
            try {
                new SocketHandler(serverSocket.accept()).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stop the server
     * @throws IOException when something goes wrong
     */
    public void exit() throws IOException {
        serverSocket.close();
    }

    /**
     *  a Communication thread created for each client -> allows client->server and server->client communication
     */
    private static class SocketHandler extends Thread { //TODO -> communicate in bytes
        private final static byte[] RECOGNIZE_EXE_MESSAGE = {69, 88, 69};
        private final static byte[] INITIALIZE_FILE_TRANSFER_MESSAGE = {70, 73, 76, 69};
        private final static byte[] END_OF_SEGMENT_MESSAGE = {69, 78, 68};
        private final static byte[] CONTROLLER_RECOGNIZE_ME_MESSAGE = {67, 79, 78, 84, 82, 79, 76};
        private final static byte END_OF_MESSAGE = 3;
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
//            out =  new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true);
            out = new BufferedOutputStream(socket.getOutputStream());
            in = new BufferedInputStream(socket.getInputStream());
            System.out.println("Password send");
            writeBytes(SERVER_PASSWORD);
        }

        /**
         * Stop this socket
         * @throws IOException when something goes wrong
         */
        public void stopSocket() throws IOException {
            socket.close();
            out.close();
            in.close();
        }

        /**
         * close connection if client does not respond with accepted message, if he does, handle the message
         * accepted messages:
         *      INITIALIZE_FILE_TRANSFER_MESSAGE -> xml file will shortly be sent from client
         *      END_OF_SEGMENT_MESSAGE -> EXE is informing us that segment has come to an end
         *      Message from Controller informing us of its IP address for http communication
         */

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

        @Override
        public void run() { // TODO -> remove commented stuff
            byte[] message = new byte[0];
            try {
                message = readLine();
            } catch (IOException e) {
                System.err.println("Client did not respond, disconnecting client");
            }
//            if ("Controller here!".equals(message)) {
//                CommunicationHandler.getInstance().addArduinoToList(new Controller(socket.getInetAddress()));
//                return;
//            }
            try {
                if (Arrays.equals(RECOGNIZE_EXE_MESSAGE, message)) {
                    message = readLine();
                    if (Arrays.equals(END_OF_SEGMENT_MESSAGE, message)) {
                        String segmentName = readStringLine(), id = readStringLine();
                        System.out.println("Segment named " + segmentName + " ended, id = " + id);
                    } else if (Arrays.equals(INITIALIZE_FILE_TRANSFER_MESSAGE, message)) {
                        FileReceiver.acceptFile(in, readStringLine());
                    } else {
                        System.out.println("Wrong message received, disconnecting");
                    }
                    stopSocket();
                    return;
                }
                if (Arrays.equals(CONTROLLER_RECOGNIZE_ME_MESSAGE, message)) {
                    System.out.println("Found controller");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.err.println("Unrecognized message = " + Arrays.toString(message));
//            Path path = Paths.get(message);
//            if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
//                stopSocket();
//                System.out.println("Client did not respond in an accepted way, disconnecting client");
//
//            String message = null;
//            try {
//                while (true) {
//                    message = in.readLine();
//                    if (message == null) {
//                        break;
//                    }
//                    System.out.println(message);
//                }
//                stopSocket();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }
}