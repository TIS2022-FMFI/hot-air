package sk.uniba.fmph;

import sk.uniba.fmph.Arduino.Arduino;
import sk.uniba.fmph.Arduino.CommunicationHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main server class
 */
public class Server extends Thread {
    private ServerSocket serverSocket;
    private final int[] listOfFreePorts = {4002, 4003, 4004, 4005, 4006, 4007, 4008, 4009, 4010, 4011};

    /**
     * Constructor, find port and initialize TCP socket
     * @throws ConnectException when no port is found
     */
    public Server() throws ConnectException {
        boolean portFound = false;
        for (int port : listOfFreePorts) {
            try {
                serverSocket = new ServerSocket(port);
                portFound = true;
                break;
            } catch (BindException e) {
                System.out.printf("Port %d occupied, trying next one\n", port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!portFound) {
            throw new ConnectException("No free port found!");
        }
        System.out.println("Connection established!");
    }

    /**
     * Start accepting clients
     */
    public void run() {
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
    public static void main(String[] args) {
        try {
            Server server=new Server();
            server.start();
            CommunicationHandler.getInstance().requestArduinoIps();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  a Communication thread created for each client -> allows client->server and server->client communication
     */
    private class SocketHandler extends Thread {
        private final Socket socket;
        private final PrintWriter out;
        private final BufferedReader in;
        /**
         * A 'password' server will send so client can recognize it, or try different port if wrong server is running on this one
         */
        private final String SERVER_PASSWORD = "abcd"; //open to suggestions

        /**
         * Constructor, receive socket, create in and out communication streams, print SERVER_PASSWORD, close connection
         * if client does not respond with accepted message
         * accepted messages:
         *      Path to xml file -> for starting
         *      Message from Arduino informing us of its IP address for http communication
         * @param s a socket to which my client is connected
         * @throws IOException when something goes wrong with socket
         */
        public SocketHandler(Socket s) throws IOException {
            socket = s;
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println(SERVER_PASSWORD);
            String message = "";
            try {
                message = in.readLine();
            } catch (IOException e) {
                System.out.println("Client did not respond, disconnecting client");
            }
            System.out.println(message);
            if ("Arduino here!".equals(message)) {
                CommunicationHandler.getInstance().addArduinoToList(new Arduino(socket.getInetAddress()));
                return;
            }
            Path path = Paths.get(message);
            if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                stopSocket();
                System.out.println("Client did not respond in an accepted way, disconnecting client");
            }
            //send file on its way
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
         * thread run method, await messages and handle them
         */
        @Override
        public void run() {
            String message = null;
            try {
                while (true) {
                    message = in.readLine();
                    if (message == null) {
                        break;
                    }
                    System.out.println(message);
                }
                stopSocket();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}