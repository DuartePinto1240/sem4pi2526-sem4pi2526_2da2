package rcomp.logging;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Standalone logging server for US090 + US091.
 *
 * Starts two threads:
 *   1. UDP receive loop (US090) — receives access log events from TCP servers
 *   2. HTTP server (US091)      — exposes two AJAX-updated pages for the Admin
 *
 * Run:
 *   java -cp <fat-jar> rcomp.logging.RemoteAccessesLoggingServer [udpPort] [httpPort]
 *
 * Defaults: UDP=9090, HTTP=8090
 */
public final class RemoteAccessesLoggingServer {

    public static final int UDP_PORT  = 9090;
    private static final int BUFFER_SIZE = 1024;
    private static final String LOG_FILE = "remote_access_log.txt";

    public static void main(final String[] args) {
        final int udpPort  = args.length > 0 ? Integer.parseInt(args[0]) : UDP_PORT;
        final int httpPort = args.length > 1 ? Integer.parseInt(args[1]) : HttpLoggingServer.HTTP_PORT;

        System.out.println("[LOGGING-SERVER] AlSafe Remote Accesses Logging Server");
        System.out.println("[LOGGING-SERVER] UDP  port : " + udpPort);
        System.out.println("[LOGGING-SERVER] HTTP port : " + httpPort);

        final AccessLogStore store = new AccessLogStore();

        // Thread 1 — HTTP server (US091)
        final Thread httpThread = new Thread(new HttpLoggingServer(store));
        httpThread.setDaemon(true);
        httpThread.start();

        // Thread 2 — UDP receive loop (US090), runs in the main thread
        runUdpLoop(store, udpPort);
    }

    private static void runUdpLoop(final AccessLogStore store, final int port) {
        try (
                final DatagramSocket socket = new DatagramSocket(port);
                final PrintWriter    logFile = new PrintWriter(new FileWriter(LOG_FILE, true))
        ) {
            System.out.println("[LOGGING-SERVER] UDP listening on port " + port);
            final byte[] buf = new byte[BUFFER_SIZE];

            while (true) {
                final DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                socket.receive(pkt);

                final String payload = new String(pkt.getData(), 0, pkt.getLength(), "UTF-8").trim();
                System.out.println("[LOGGING-SERVER] Received: " + payload);

                try {
                    final AccessLogEntry entry = AccessLogEntry.fromPayload(payload);
                    store.addEntry(entry);

                    // Append to log file (US090 persistence)
                    logFile.println(payload);
                    logFile.flush();

                } catch (final IllegalArgumentException e) {
                    System.err.println("[LOGGING-SERVER] Malformed datagram ignored: " + e.getMessage());
                }
            }
        } catch (final IOException e) {
            System.err.println("[LOGGING-SERVER] Fatal UDP error: " + e.getMessage());
        }
    }
}