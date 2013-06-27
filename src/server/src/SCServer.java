import org.opencv.samples.facedetect.Message;

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
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        debug("Started listening at port " + PORT);

        new Thread(new ServerCommander(this)).start();

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
        System.out.println("[SERVER MAIN THREAD] " + msg);
    }
}

class ServerCommander implements Runnable {

    private final BufferedReader reader;
    private final SCServer server;

    public ServerCommander(SCServer server) {
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.server = server;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(100);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        try {
            String input = reader.readLine();
            if(input != null && input.equalsIgnoreCase("shuwdown")) {
                synchronized(this) {
                    for(ClientThread c : server.getConnectedClients().values()) {
                        c.shutdown();
                    }
                    System.exit(1);
                }
            } else if(input != null && input.equalsIgnoreCase("clients")) {
                synchronized(this) {
                    for(String s : server.getConnectedClients().keySet()) {
                        System.out.println(s);
                    }
                }
            }

        } catch(IOException e) {
            e.printStackTrace();
        }

    }
}

class ClientThread implements Runnable {

    private ObjectInputStream is;
    private ObjectOutputStream os;
    private Socket clientSocket;
    private final Map<String, ClientThread> connectedClients;
    private String myuser;
    private String otheruser;
    private boolean connected;
    private boolean shuttingDown;
    private boolean blinked;
    private long blinktime;


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

    public void shutdown() {
        debug("SHUTDOWN");
        shuttingDown = true;
        if(otheruser != null) {
            ClientThread t = connectedClients.get(otheruser);
            if(t != null) {
                Message m = new Message(myuser, otheruser, null, Message.MSG_ENDGAME, 0);
                t.write(m);
            }
        }
        broadcast(new Message(myuser, otheruser, null, Message.MSG_LEFT, 0));

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
                if(!clientSocket.isConnected() || clientSocket.isClosed()) {
                    shutdown();
                    break;
                }

                Object o = is.readObject();
                if(o instanceof Message) {
                    Message m = (Message)o;
                    System.out.println("[" + m.from + "->" + m.to + " " + m.type + "]");

                    switch(m.type) {
                        case Message.MSG_JOIN:
                            if(myuser != null) {
                                debug("2ND JOIN");
                            } else if(m.from != null) {
                                myuser = m.from;
                                connectedClients.put(myuser, this);
                                broadcast(new Message(myuser, otheruser, null, Message.MSG_JOIN, 0));
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
                            break;
                        case Message.MSG_SENDF:
                            write(m);
                            if(m.to != null && connectedClients.containsKey(m.to)) {
                                otheruser = m.to;
                                ClientThread t = connectedClients.get(m.to);
                                t.write(m);
                            }
                            break;
                        case Message.MSG_SENDBLINK:
                            if(m.to != null && connectedClients.containsKey(m.to)) {
                                synchronized(this) {
                                    ClientThread t = connectedClients.get(m.to);
                                    blinked = true;
                                    blinktime = m.time;
                                    if(t.blinked) {
                                        ClientThread winner = t.blinktime > blinktime ? t : this;
                                        ClientThread loser = winner == this ? t : this;
                                        this.blinked = false;
                                        t.blinked = false;
                                        winner.write(new Message(loser.myuser, winner.myuser, null, Message.MSG_WON, loser.blinktime));
                                        loser.write(new Message(winner.myuser, loser.myuser, null, Message.MSG_LOST, winner.blinktime));
                                    }
                                }

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
            e.printStackTrace();
            shutdown();
        }
    }

    private void debug(String msg) {
        System.out.println("[CLIENTTHREAD/" + myuser + "] " + msg);
    }
}
