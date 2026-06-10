package rcomp.logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread-safe in-memory store for remote access log entries (US090/US091).
 *
 * Shared by the UDP receive loop (US090) and the HTTP server (US091).
 * Bounded to MAX_ENTRIES to prevent unbounded memory growth.
 */
public final class AccessLogStore {

    private static final int MAX_ENTRIES = 1000;

    private final List<AccessLogEntry> entries = new ArrayList<>();

    /** Adds an entry. Evicts the oldest if the store is full. */
    public synchronized void addEntry(final AccessLogEntry entry) {
        if (entries.size() >= MAX_ENTRIES) entries.remove(0);
        entries.add(entry);
    }

    /**
     * Returns the last n entries, most recent first.
     * If n <= 0 or n > size, returns all entries.
     */
    public synchronized List<AccessLogEntry> getRecentEntries(final int n) {
        final int size = entries.size();
        final int from = (n > 0 && n < size) ? size - n : 0;
        final List<AccessLogEntry> result = new ArrayList<>(entries.subList(from, size));
        Collections.reverse(result);
        return result;
    }

    /**
     * Returns currently active sessions.
     *
     * A session is active if the most recent event for a given
     * (username, service, clientIp, clientPort) tuple is LOGIN_SUCCESS
     * with no subsequent LOGOUT or DISCONNECT.
     */
    public synchronized List<AccessLogEntry> getActiveSessions() {
        // key = "username|service|ip|port"
        final Map<String, AccessLogEntry> latest = new LinkedHashMap<>();
        for (final AccessLogEntry e : entries) {
            final String key = e.username + "|" + e.service + "|"
                    + e.clientIp + "|" + e.clientPort;
            latest.put(key, e); // keeps last event per tuple
        }

        final List<AccessLogEntry> active = new ArrayList<>();
        for (final AccessLogEntry e : latest.values()) {
            if (e.eventType == EventType.LOGIN_SUCCESS) active.add(e);
        }
        return active;
    }

    public synchronized int size() {
        return entries.size();
    }
}