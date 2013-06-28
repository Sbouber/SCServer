import org.opencv.samples.facedetect.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;

public class ClientThread implements Runnable {

    private ObjectInputStream is;
    private ObjectOutputStream os;
    private Socket clientSocket;
    private final Map<String, ClientThread> connectedClients;
    private String myUser;
    private String otherUser;
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
                debug("BROADCAST " + m.from + " TO " + c.myUser);
                c.write(m);
            }
        }
    }

    private void write(Message m) {
        try {
            os.writeObject(m);
            os.flush();
            debug("Wrote " + m.from + "->" + m.to + " " + m.type);
        } catch(IOException e) {
            if(!shuttingDown) {
                shutdown();
            }
        }
    }

    private synchronized void setOtherUser(String otherUser) {
        this.otherUser = otherUser;
    }

    public void shutdown() {
        debug("SHUTDOWN");
        shuttingDown = true;
        if(otherUser != null) {
            ClientThread t = connectedClients.get(otherUser);
            if(t != null) {
                Message m = new Message(myUser, otherUser, null, Message.MSG_ENDGAME, 0);
                t.write(m);
            }
        }
        broadcast(new Message(myUser, otherUser, null, Message.MSG_LEFT, 0));

        if(myUser != null) {
            connectedClients.remove(myUser);
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
                            if(myUser != null) {
                                debug("2ND JOIN");
                            } else if(m.from != null) {
                                myUser = m.from;
                                connectedClients.put(myUser, this);
                                broadcast(new Message(myUser, otherUser, null, Message.MSG_JOIN, 0));
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
                                otherUser = m.to;
                                ClientThread t = connectedClients.get(m.to);
                                t.setOtherUser(myUser);
                                t.write(m);
                            }
                            break;
                        case Message.MSG_SENDF:
                            write(m);
                            if(m.to != null && connectedClients.containsKey(m.to)) {
                                otherUser = m.to;
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
                                        winner.write(new Message(loser.myUser, winner.myUser, null, Message.MSG_WON, loser.blinktime));
                                        loser.write(new Message(winner.myUser, loser.myUser, null, Message.MSG_LOST, winner.blinktime));
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
        if(SCServer.debug)
            System.out.println("[CLIENTTHREAD/" + myUser + "] " + msg);
    }
}
