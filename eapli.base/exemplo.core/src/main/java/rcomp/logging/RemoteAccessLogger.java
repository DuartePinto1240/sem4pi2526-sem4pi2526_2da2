package rcomp.logging;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;

/**
 * UDP sender utility used by the three TCP servers (US44, US78, US86) to
 * log remote access events to the Remote Accesses Logging Server (US090).
 *
 * Non-blocking: all exceptions are silently swallowed so a logging failure
 * never interrupts the TCP server's normal operation.
 */
public final class RemoteAccessLogger {

    private final String host;
    private final int    port;

    public RemoteAccessLogger(final String host, final int port) {
        this.host = host;
        this.port = port;
    }

    /** Convenience constructor using default logging server port. */
    public RemoteAccessLogger(final String host) {
        this(host, RemoteAccessesLoggingServer.UDP_PORT);
    }

    /** Logs a remote access event. Never throws. */
    public void log(final EventType eventType,
                    final String service,
                    final String username,
                    final String clientIp,
                    final int clientPort) {
        try {
            final AccessLogEntry entry = new AccessLogEntry(
                    eventType, service, LocalDateTime.now(),
                    username, clientIp, clientPort);
            final byte[] payload = entry.toPayload().getBytes("UTF-8");
            final InetAddress addr = InetAddress.getByName(host);
            final DatagramPacket pkt =
                    new DatagramPacket(payload, payload.length, addr, port);
            try (final DatagramSocket sock = new DatagramSocket()) {
                sock.send(pkt);
            }
        } catch (final Exception ignored) {
            // Non-blocking — logging failures must not affect TCP servers
        }
    }
}