package rcomp.logging;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * HTTP server thread for US091 — Remote Accesses Logging Visualisation.
 *
 * Listens on HTTP_PORT and spawns one HttpRequestHandler daemon thread
 * per incoming HTTP request. Shares the AccessLogStore with the UDP
 * receive loop (US090).
 */
public final class HttpLoggingServer implements Runnable {

    public static final int HTTP_PORT = 8090;

    private final AccessLogStore store;

    public HttpLoggingServer(final AccessLogStore store) {
        this.store = store;
    }

    @Override
    public void run() {
        try (final ServerSocket serverSocket = new ServerSocket(HTTP_PORT)) {
            System.out.println("[HTTP-SERVER] Listening on port " + HTTP_PORT);
            System.out.println("[HTTP-SERVER] Events page : http://localhost:" + HTTP_PORT + "/");
            System.out.println("[HTTP-SERVER] Active page : http://localhost:" + HTTP_PORT + "/active");

            while (true) {
                final Socket client = serverSocket.accept();
                final Thread t = new Thread(new HttpRequestHandler(client, store));
                t.setDaemon(true);
                t.start();
            }
        } catch (final IOException e) {
            System.err.println("[HTTP-SERVER] Fatal: " + e.getMessage());
        }
    }
}