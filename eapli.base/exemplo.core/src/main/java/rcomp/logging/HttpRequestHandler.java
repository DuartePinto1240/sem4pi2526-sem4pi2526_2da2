package rcomp.logging;

import java.io.*;
import java.net.Socket;
import java.util.List;

/**
 * Handles one HTTP/1.0 GET request for the US091 logging visualisation server.
 *
 * Routes:
 *   GET /            → events HTML page
 *   GET /events      → events HTML page
 *   GET /active      → active users HTML page
 *   GET /api/events  → JSON array of recent log entries
 *   GET /api/active  → JSON array of active sessions
 */
final class HttpRequestHandler implements Runnable {

    private final Socket        socket;
    private final AccessLogStore store;

    HttpRequestHandler(final Socket socket, final AccessLogStore store) {
        this.socket = socket;
        this.store  = store;
    }

    @Override
    public void run() {
        try (
                final BufferedReader in  = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                final PrintWriter    out = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
            final String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            // Parse: GET /path HTTP/1.0
            final String[] parts = requestLine.split(" ");
            if (parts.length < 2) return;
            final String path = parts[1].split("\\?")[0]; // strip query string
            final String query = parts[1].contains("?") ? parts[1].split("\\?")[1] : "";

            // Drain headers
            String header;
            while ((header = in.readLine()) != null && !header.isEmpty()) { /* skip */ }

            switch (path) {
                case "/api/events": serveEventsJson(out, query);  break;
                case "/api/active": serveActiveJson(out);         break;
                case "/active":     serveActivePage(out);         break;
                default:            serveEventsPage(out);         break;
            }

        } catch (final IOException e) {
            // Client disconnected — ignore
        } finally {
            try { socket.close(); } catch (final IOException ignored) {}
        }
    }

    // ── JSON API ──────────────────────────────────────────────────────────────

    private void serveEventsJson(final PrintWriter out, final String query) {
        int n = 50;
        if (query.startsWith("n=")) {
            try { n = Integer.parseInt(query.substring(2)); } catch (final NumberFormatException ignored) {}
        }
        final List<AccessLogEntry> entries = store.getRecentEntries(n);
        final StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) json.append(",");
            json.append(entries.get(i).toJson());
        }
        json.append("]");
        sendJson(out, json.toString());
    }

    private void serveActiveJson(final PrintWriter out) {
        final List<AccessLogEntry> active = store.getActiveSessions();
        final StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < active.size(); i++) {
            if (i > 0) json.append(",");
            final AccessLogEntry e = active.get(i);
            json.append("{")
                    .append("\"username\":\"").append(e.username).append("\",")
                    .append("\"service\":\"").append(e.service).append("\",")
                    .append("\"loginTime\":\"").append(e.timestamp).append("\",")
                    .append("\"clientIp\":\"").append(e.clientIp).append("\",")
                    .append("\"clientPort\":").append(e.clientPort)
                    .append("}");
        }
        json.append("]");
        sendJson(out, json.toString());
    }

    private void sendJson(final PrintWriter out, final String body) {
        out.print("HTTP/1.0 200 OK\r\n");
        out.print("Content-Type: application/json\r\n");
        out.print("Content-Length: " + body.length() + "\r\n");
        out.print("Access-Control-Allow-Origin: *\r\n");
        out.print("\r\n");
        out.print(body);
        out.flush();
    }

    // ── HTML Pages ────────────────────────────────────────────────────────────

    private void serveEventsPage(final PrintWriter out) {
        final String body = "<!DOCTYPE html><html><head>"
                + "<meta charset='UTF-8'>"
                + "<title>AISafe — Remote Access Events</title>"
                + "<style>"
                + "body{font-family:sans-serif;margin:20px;background:#f5f5f5}"
                + "h1{color:#333}table{border-collapse:collapse;width:100%;background:#fff}"
                + "th,td{border:1px solid #ddd;padding:8px;text-align:left}"
                + "th{background:#4a90d9;color:#fff}"
                + "tr:nth-child(even){background:#f9f9f9}"
                + ".badge{padding:2px 6px;border-radius:3px;font-size:12px}"
                + ".LOGIN_SUCCESS{background:#d4edda;color:#155724}"
                + ".LOGIN_FAIL{background:#f8d7da;color:#721c24}"
                + ".LOGOUT{background:#d1ecf1;color:#0c5460}"
                + ".DISCONNECT{background:#fff3cd;color:#856404}"
                + "#status{margin-bottom:10px;color:#666;font-size:13px}"
                + "</style></head><body>"
                + "<h1>AlSafe — Remote Access Events</h1>"
                + "<p id='status'>Loading...</p>"
                + "<table id='tbl'><thead><tr>"
                + "<th>Timestamp</th><th>Event</th><th>Service</th>"
                + "<th>Username</th><th>Client IP</th><th>Port</th>"
                + "</tr></thead><tbody id='tbody'></tbody></table>"
                + "<p><a href='/active'>View Active Users</a></p>"
                + "<script>"
                + "function fetchAndUpdate(){"
                + "  fetch('/api/events?n=50').then(r=>r.json()).then(data=>{"
                + "    const tb=document.getElementById('tbody');"
                + "    tb.innerHTML=data.map(e=>"
                + "      '<tr><td>'+e.timestamp+'</td>'"
                + "      +'<td><span class=\"badge '+e.eventType+'\">'+e.eventType+'</span></td>'"
                + "      +'<td>'+e.service+'</td>'"
                + "      +'<td>'+e.username+'</td>'"
                + "      +'<td>'+e.clientIp+'</td>'"
                + "      +'<td>'+e.clientPort+'</td></tr>'"
                + "    ).join('');"
                + "    document.getElementById('status').textContent='Last updated: '+new Date().toLocaleTimeString()+' ('+data.length+' events)';"
                + "  }).catch(()=>{"
                + "    document.getElementById('status').textContent='Update failed — retrying...';"
                + "  });"
                + "}"
                + "fetchAndUpdate();"
                + "setInterval(fetchAndUpdate, 3000);"
                + "</script></body></html>";
        sendHtml(out, body);
    }

    private void serveActivePage(final PrintWriter out) {
        final String body = "<!DOCTYPE html><html><head>"
                + "<meta charset='UTF-8'>"
                + "<title>AISafe — Active Users</title>"
                + "<style>"
                + "body{font-family:sans-serif;margin:20px;background:#f5f5f5}"
                + "h1{color:#333}table{border-collapse:collapse;width:100%;background:#fff}"
                + "th,td{border:1px solid #ddd;padding:8px;text-align:left}"
                + "th{background:#5cb85c;color:#fff}"
                + "tr:nth-child(even){background:#f9f9f9}"
                + "#status{margin-bottom:10px;color:#666;font-size:13px}"
                + "</style></head><body>"
                + "<h1>AlSafe — Currently Active Users</h1>"
                + "<p id='status'>Loading...</p>"
                + "<table id='tbl'><thead><tr>"
                + "<th>Username</th><th>Service</th><th>Login Time</th>"
                + "<th>Client IP</th><th>Port</th>"
                + "</tr></thead><tbody id='tbody'></tbody></table>"
                + "<p><a href='/'>View All Events</a></p>"
                + "<script>"
                + "function fetchAndUpdate(){"
                + "  fetch('/api/active').then(r=>r.json()).then(data=>{"
                + "    const tb=document.getElementById('tbody');"
                + "    if(data.length===0){"
                + "      tb.innerHTML='<tr><td colspan=\"5\" style=\"text-align:center;color:#888\">No active users</td></tr>';"
                + "    } else {"
                + "      tb.innerHTML=data.map(e=>"
                + "        '<tr><td>'+e.username+'</td>'"
                + "        +'<td>'+e.service+'</td>'"
                + "        +'<td>'+e.loginTime+'</td>'"
                + "        +'<td>'+e.clientIp+'</td>'"
                + "        +'<td>'+e.clientPort+'</td></tr>'"
                + "      ).join('');"
                + "    }"
                + "    document.getElementById('status').textContent='Last updated: '+new Date().toLocaleTimeString()+' ('+data.length+' active)';"
                + "  }).catch(()=>{"
                + "    document.getElementById('status').textContent='Update failed — retrying...';"
                + "  });"
                + "}"
                + "fetchAndUpdate();"
                + "setInterval(fetchAndUpdate, 3000);"
                + "</script></body></html>";
        sendHtml(out, body);
    }

    private void sendHtml(final PrintWriter out, final String body) {
        out.print("HTTP/1.0 200 OK\r\n");
        out.print("Content-Type: text/html; charset=UTF-8\r\n");
        out.print("Content-Length: " + body.getBytes().length + "\r\n");
        out.print("\r\n");
        out.print(body);
        out.flush();
    }
}