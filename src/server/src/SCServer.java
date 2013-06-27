import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SCServer implements Runnable {

    public static final int PORT = 8888;
    public static final String HOST = "localhost";
    public static final int MAX_CLIENTS = 10;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Map<String, ClientThread> connectedClients;

    private final ClientThread[] threads = new ClientThread[MAX_CLIENTS];

    public SCServer() {
        connectedClients = new ConcurrentHashMap<String, ClientThread>();

        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        debug("Started listening at " + HOST + ":" + PORT);
        while (true) {
            if(connectedClients.size() >= MAX_CLIENTS) {
                debug("MAX CLIENTS!");
                continue;
            }

            try {
                clientSocket = serverSocket.accept();
                for (int i = 0; i < MAX_CLIENTS; i++) {
                    if (threads[i] == null) {
                        debug("New client connected");
                        ClientThread client = new ClientThread(clientSocket, connectedClients);
                        threads[i] = client;
                        new Thread(client).start();
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    private void debug(String msg) {
        System.out.println("[SERVER MAIN THREAD] " + msg);
    }
}

class ClientThread implements Runnable {

    private ObjectInputStream is;
    private ObjectOutputStream os;
    private Socket clientSocket;
    private final Map<String, ClientThread> connectedClients;
    private String myuser;
    private String otheruser;
    boolean connected;

    boolean sending;


    public ClientThread(Socket clientSocket, Map<String, ClientThread> connectedClients) {
        this.connectedClients = connectedClients;
        this.clientSocket = clientSocket;
    }

    private synchronized void broadcast(Message m) {
        for(ClientThread c : connectedClients.values()) {
            if(c.clientSocket.isConnected() && c != this) {
                debug("BROADCAST " + m.from + " TO " + c.myuser);
                c.write(m);
            }
        }
    }

    private void write(Message m) {
        try {
            os.writeObject(m);
            os.flush();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void shutdown() {
        debug("SHUTDOWN");
        if(otheruser != null) {
            ClientThread t = connectedClients.get(otheruser);
            if(t != null) {
                Message m = new Message(myuser, otheruser, null, Message.MSG_ENDGAME, null);
                t.write(m);
            }
        }
        broadcast(new Message(myuser, otheruser, null, Message.MSG_LEFT, null));

        if(myuser != null) {
            connectedClients.remove(myuser);
        }

        try {
            clientSocket.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            os = new ObjectOutputStream(clientSocket.getOutputStream());
            is = new ObjectInputStream(clientSocket.getInputStream());

            while (true) {
                if(!clientSocket.isConnected()) {
                    shutdown();
                    break;
                }

                Object o = is.readObject();
                if(o instanceof Message) {
                    debug("GOT A MSG");
                    Message m = (Message)o;
                    switch(m.type) {
                        case Message.MSG_JOIN:
                            if(myuser != null) {
                                debug("2ND JOIN");
                            } else if(m.from != null) {
                                myuser = m.from;
                                connectedClients.put(myuser, this);
                                broadcast(new Message(myuser, otheruser, null, Message.MSG_JOIN, null));
                                debug("JOINED");
                            } else {
                                debug("M.FROM == NULL");
                            }
                            break;
                        default:
                            break;
                    }
                } else {
                    debug("INVALID OBJECT");
                }
            }
        } catch (Exception e) {
           shutdown();
        }
    }

    private void debug(String msg) {
        System.out.println("[CLIENTTHREAD/" + myuser + "] " + msg);
    }
}
