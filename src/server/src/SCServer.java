import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SCServer implements Runnable {

    public static final int PORT = 8888;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Map<String, ClientThread> connectedClients;

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
        debug("Started listening at port " + PORT);
        while (true) {
            try {
                clientSocket = serverSocket.accept();
                debug("New client connected");
                ClientThread client = new ClientThread(clientSocket, connectedClients);
                new Thread(client).start();
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
    boolean shuttingDown;

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
            if(!shuttingDown) {
                shutdown();
            }
        }
    }

    private synchronized void setOtherUser(String otherUser) {
        this.otheruser = otherUser;
    }

    private void shutdown() {
        debug("SHUTDOWN");
        shuttingDown = true;
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
                    Message m = (Message)o;
                    switch(m.type) {
                        case Message.MSG_JOIN:
                            if(myuser != null) {
                                debug("2ND JOIN");
                            } else if(m.from != null) {
                                myuser = m.from;
                                connectedClients.put(myuser, this);
                                broadcast(new Message(myuser, otheruser, null, Message.MSG_JOIN, null));
                            } else {
                                debug("M.FROM == NULL");
                            }
                            break;
                        case Message.MSG_REQGAME:
                            if(m.to != null && connectedClients.containsKey(m.to)) {
                                connectedClients.get(m.to).write(m);
                            }
                            break;
                        case Message.MSG_ACKREQ:
                            if(m.to != null && connectedClients.containsKey(m.to)) {
                                otheruser = m.to;
                                ClientThread t = connectedClients.get(m.to);
                                t.setOtherUser(myuser);
                                t.write(m);
                            }
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
