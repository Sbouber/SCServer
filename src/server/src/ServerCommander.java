import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Creates a CLI to access the server.
 * Commands:
 *  -shutdown
 *  -debug  --- enable debugging messages
 *  -!debug --- disable debugging messages
 *  -clients --- prints a list of connected clients
 */
public class ServerCommander implements Runnable {

    private final BufferedReader reader;
    private final SCServer server;

    public ServerCommander(SCServer server) {
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.server = server;
    }

    @Override
    public void run() {
        while(true) {
            try {
                Thread.sleep(100);

                String input = reader.readLine();
                if(input != null && input.equalsIgnoreCase("shutdown")) {
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
                } else if(input != null && input.equalsIgnoreCase("debug")) {
                    SCServer.debug = true;
                } else if(input != null && input.equalsIgnoreCase("!debug")) {
                    SCServer.debug = false;
                }

            } catch(IOException e) {
                e.printStackTrace();
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
