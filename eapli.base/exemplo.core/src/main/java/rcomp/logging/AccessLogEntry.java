package rcomp.logging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Immutable value object representing one remote access log entry (US090/US091).
 */
public final class AccessLogEntry {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public final EventType     eventType;
    public final String        service;
    public final LocalDateTime timestamp;
    public final String        username;
    public final String        clientIp;
    public final int           clientPort;

    public AccessLogEntry(final EventType eventType,
                          final String service,
                          final LocalDateTime timestamp,
                          final String username,
                          final String clientIp,
                          final int clientPort) {
        this.eventType  = eventType;
        this.service    = service;
        this.timestamp  = timestamp;
        this.username   = username;
        this.clientIp   = clientIp;
        this.clientPort = clientPort;
    }

    /**
     * Parses a pipe-separated payload into an AccessLogEntry.
     * Format: eventType|service|timestamp|username|clientIp|clientPort
     */
    public static AccessLogEntry fromPayload(final String payload) {
        final String[] p = payload.split("\\|", 6);
        if (p.length < 6) throw new IllegalArgumentException("Invalid payload: " + payload);
        return new AccessLogEntry(
                EventType.valueOf(p[0].trim()),
                p[1].trim(),
                LocalDateTime.parse(p[2].trim(), FMT),
                p[3].trim(),
                p[4].trim(),
                Integer.parseInt(p[5].trim()));
    }

    /**
     * Serialises this entry to a pipe-separated string (same format as UDP payload).
     */
    public String toPayload() {
        return eventType + "|" + service + "|" + timestamp.format(FMT)
                + "|" + username + "|" + clientIp + "|" + clientPort;
    }

    /**
     * Serialises this entry to a JSON object string.
     */
    public String toJson() {
        return "{"
                + "\"eventType\":\"" + eventType + "\","
                + "\"service\":\"" + service + "\","
                + "\"timestamp\":\"" + timestamp.format(FMT) + "\","
                + "\"username\":\"" + username + "\","
                + "\"clientIp\":\"" + clientIp + "\","
                + "\"clientPort\":" + clientPort
                + "}";
    }

    @Override
    public String toString() {
        return toPayload();
    }
}