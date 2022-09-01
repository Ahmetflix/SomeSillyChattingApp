package xyz.ahmetflix.chattingserver;

public class ServerShutdownThread extends Thread {
    private final Server server;

    public ServerShutdownThread(Server server) {
        this.server = server;
    }

    @Override
    public void run() {
        try {
            server.stop();
        } finally {
            try {
                server.reader.getTerminal().restore();
            } catch (Exception e) {
            }
        }
    }
}
