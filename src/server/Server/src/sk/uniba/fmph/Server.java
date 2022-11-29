package sk.uniba.fmph;

import sk.uniba.fmph.xml.FileReceiver;

import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Main server class
 */
public class Server { //TODO -> sort out exceptions
    private final static Server INSTANCE = new Server();
    public static Server getInstance() {return INSTANCE;}

    private ServerSocket serverSocket;
//    private final int[] listOfFreePorts = new int[] {4002, 4003, 4004, 4005, 4006, 4007, 4008, 4009, 4010, 4011}; //TODO
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
//        int myPort = -1;
//        for (int port : listOfFreePorts) {
//            try {
//                serverSocket = new ServerSocket(port);
//                myPort = port;
//                break;
//            } catch (BindException e) {
//                System.out.printf("Port %d occupied, trying next one\n", port);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        PORT = myPort;
//        if (myPort == -1) {
////            throw new ConnectException("No free port found!");
//            System.err.println("No free port found, terminating"); //TODO -> terminate
//            return;
//        }
        UDPCommunicationHandler.getInstance().start();
        System.out.println("Connection established!");
    }

    /**
     * Start accepting clients
     */
    public void begin() {
//        if (PORT == -1) {
//            return;
//        }
        while (!serverSocket.isClosed()) {
            try {
//                new SocketHandler(serverSocket.accept());
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
    private static class SocketHandler extends Thread {
//        private static enum messages {RECOGNIZE_EXE_MESSAGE, INITIALIZE_FILE_TRANSFER_MESSAGE, END_OF_SEGMENT_MESSAGE}
        private final static String RECOGNIZE_EXE_MESSAGE = "EXE here";
        private final static String INITIALIZE_FILE_TRANSFER_MESSAGE = "Prepare for file transfer";
        private final static String END_OF_SEGMENT_MESSAGE = "segment is done";
        private final Socket socket;
        private final PrintWriter out;
        private final BufferedReader in;
        /**
         * A 'password' server will send so client can recognize it, or try different port if wrong server is running on this one
         */
        private final static String SERVER_PASSWORD = "abcd"; //open to suggestions

        /**
         * Constructor, receive socket, create in and out communication streams, send SERVER_PASSWORD to client
         * @param s a socket to which my client is connected
         * @throws IOException when something goes wrong with socket
         */
        public SocketHandler(Socket s) throws IOException {
            socket = s;
            out =  new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out.println(SERVER_PASSWORD);
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
        @Override
        public void run() { // TODO -> remove commented stuff
            String message = "";
            try {
                message = in.readLine();
            } catch (IOException e) {
                System.err.println("Client did not respond, disconnecting client");
            }
//            if ("Controller here!".equals(message)) {
//                CommunicationHandler.getInstance().addArduinoToList(new Controller(socket.getInetAddress()));
//                return;
//            }
            try {
                if (RECOGNIZE_EXE_MESSAGE.equals(message)) {
                    message = in.readLine();
                    if (END_OF_SEGMENT_MESSAGE.equals(message)) {
                        String segmentName = in.readLine(), id = in.readLine();
                        System.out.println("Segment named " + segmentName + " ended, id = " + id);
                    } else if (INITIALIZE_FILE_TRANSFER_MESSAGE.equals(message)) {
                        message = in.readLine();
                        FileReceiver.acceptFile(in, message);
                    } else {
                        System.out.println("Wrong message received, disconnecting");
                    }
                    stopSocket();
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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