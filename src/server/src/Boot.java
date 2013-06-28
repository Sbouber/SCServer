
public class Boot {

    public static final int PORT = 8888;

    public static void main(String[] args) {
        SCServer server = new SCServer(PORT);
        ServerCommander cli = new ServerCommander(server);
        new Thread(server).start();
        new Thread(cli).start();
    }

}
