
import java.io.*;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SCClient implements SCMessageListener {

    public static final int PORT = 8888;
    public static final String HOST = "localhost";

    private Socket clientSocket;
    private ObjectOutputStream os;
    private ObjectInputStream is;
    private ConcurrentLinkedQueue<Message> msgQueue;

    private boolean init;

    private final String myUser;
    private String otherUser;

    private final SCMessageListener listener;

    public SCClient(String myUser, SCMessageListener listener) {
        this.myUser = myUser;

        //todo assign listener
        //this.listener = listener;
        this.listener = this;

        try {
            clientSocket = new Socket(HOST, PORT);
            clientSocket.setTcpNoDelay(true);
            os = new ObjectOutputStream(clientSocket.getOutputStream());
            is = new ObjectInputStream(clientSocket.getInputStream());
        } catch (Exception e) {
           return;
        }

        msgQueue = new ConcurrentLinkedQueue<Message>();

        new Thread(new SCClientReader()).start();
        new Thread(new SCClientWriter()).start();
        new Thread(new SCTerminalReader()).start();
        msgQueue.add(new Message(myUser, otherUser, null, Message.MSG_JOIN, null));

        init = true;
    }

    public boolean isInitialized() {
        return init;
    }

    public String getMyUser() {
        return myUser;
    }

    public String getOtherUser() {
        return otherUser;
    }

    private void debug(String msg) {
        System.out.println("[CLIENT/"+ myUser + "] " + msg);
    }

    public void addMsg(Message msg) {
        msgQueue.add(msg);
    }

    public void getOnlinePlayers() {
        msgQueue.add(new Message(myUser ,otherUser, null, Message.MSG_DISC, null));
    }

    public void sendFrame(byte[] frame) {
        msgQueue.add(new Message(myUser, otherUser, frame, Message.MSG_SENDF, null));
    }

    public void sendBlink(Timestamp t) {
        msgQueue.add(new Message(myUser, otherUser, null, Message.MSG_SENDBLINK, t));
    }

    public void acceptRequest(String player) {
        msgQueue.add(new Message(myUser, otherUser, null, Message.MSG_ACKREQ, null));
    }

    public void sendRequest(String player) {
        msgQueue.add(new Message(myUser, player, null, Message.MSG_REQGAME, null));
    }

    @Override
    public void onWin() {
        debug("onWin");
    }

    @Override
    public void onLose() {
        debug("onLose");
    }

    @Override
    public void onPlayerJoined(String player) {
        debug("onPlayerJoined " + player);
    }

    @Override
    public void onPlayerLeft(String player) {
        debug("onPlayerLeft " + player);
    }

    @Override
    public void onGameRequest(String player) {
        debug("onGameRequest " + player);
        otherUser = player;
    }

    @Override
    public void onRequestAccepted(String player) {
        debug("onRequestAccepted");
    }

    @Override
    public void onRequestDenied(String player) {
        debug("onRequestDenied");
    }

    @Override
    public void onFrameReceived(byte[] frame) {
        debug("onFrameReceived");
    }

    @Override
    public void onConnectionClosed() {
        debug("onConnectionClosed");
    }

    @Override
    public void onConnected() {
        debug("onConnected");
    }

    @Override
    public void onGameEnd() {
        debug("onGameEnd");
    }

    @Override
    public void onGameStart() {
        debug("onGameStart");
    }

    private class SCTerminalReader implements Runnable {
        public void run() {
            while(true) {
                try {
                    Thread.sleep(1000);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    System.out.print("Input for " + myUser);
                    String input = new BufferedReader(new InputStreamReader(System.in)).readLine();
                    msgQueue.add(new Message(myUser, input.substring(1), null, Integer.parseInt(input.substring(0, 1)), null));
                    System.out.println("added");
                } catch(Exception e) {
                    e.printStackTrace();
                }

            }
        }

    }

    private class SCClientReader implements Runnable {
        @Override
        public void run() {
            while(clientSocket.isConnected()) {
                try {
                    Object o = is.readObject();
                    if(o != null && o instanceof Message) {
                        Message m = (Message) o;

                        switch(m.type) {
                            case Message.MSG_JOIN:
                                listener.onPlayerJoined(m.from);
                                break;
                            case Message.MSG_ACKJOIN:
                                listener.onConnected();
                                break;
                            case Message.MSG_NACKJOIN:
                                listener.onConnectionClosed();
                                break;
                            case Message.MSG_ENDGAME:
                                listener.onGameEnd();
                                break;
                            case Message.MSG_LOST:
                                listener.onLose();
                                break;
                            case Message.MSG_WON:
                                listener.onWin();
                                break;
                            case Message.MSG_SENDF:
                                listener.onFrameReceived(m.frame);
                                break;
                            case Message.MSG_ACKREQ:
                                listener.onRequestAccepted(m.from);
                                break;
                            case Message.MSG_NACKREQ:
                                listener.onRequestDenied(m.from);
                                break;
                            case Message.MSG_REQGAME:
                                listener.onGameRequest(m.from);
                                break;
                            case Message.MSG_STARTGAME:
                                listener.onGameStart();
                                break;
                            case Message.MSG_LEFT:
                                listener.onPlayerLeft(m.from);
                            default:
                                break;
                        }

                    }
                } catch(Exception e) {
                    listener.onConnectionClosed();
                    break;
                }
            }

            listener.onConnectionClosed();
        }
    }

    private class SCClientWriter implements Runnable {

        @Override
        public void run() {
            while (clientSocket.isConnected()) {
                try {
                    Message m = null;
                    if(!msgQueue.isEmpty() && (m = msgQueue.poll()) != null) {
                        if(m.type == Message.MSG_SENDF) {
                            byte[] bytes = new byte[10000];
                            for(int i = 0; i < 10000; i++) {
                                bytes[i] = 66;
                            }
                            Message m2 = new Message(m.from, m.to, bytes, Message.MSG_SENDF, null);
                        }
                        os.writeObject(m);
                        os.flush();
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                    listener.onConnectionClosed();
                    break;
                }
            }
            listener.onConnectionClosed();
        }
    }
}
