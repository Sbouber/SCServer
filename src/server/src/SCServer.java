import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic multi-threaded server using serialization.
 */
public class SCServer implements Runnable {

    public static boolean debug = true;

    private final int port;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Map<String, ClientThread> connectedClients;

    public SCServer(int port) {
        this.port = port;
        connectedClients = new ConcurrentHashMap<String, ClientThread>();
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        debug("Started listening at port " + port);

        while (true) {
            try {
                clientSocket = serverSocket.accept();
                debug("New client connected");
                ClientThread client = new ClientThread(clientSocket, connectedClients);
                new Thread(client).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Map<String, ClientThread> getConnectedClients() {
        return connectedClients;
    }

    private void debug(String msg) {
        if(debug)
            System.out.println("[SERVER MAIN THREAD] " + msg);
    }
}

