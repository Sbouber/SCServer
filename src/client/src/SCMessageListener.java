
public interface SCMessageListener {

    void onWin();

    void onLose();

    void onPlayerJoined(String player);

    void onPlayerLeft(String player);

    void onGameRequest(String player);

    void onRequestAccepted(String player);

    void onRequestDenied(String player);

    void onFrameReceived(byte[] frame);

    void onConnectionClosed();

    void onConnected();

    void onGameEnd();

    void onGameStart();
}
