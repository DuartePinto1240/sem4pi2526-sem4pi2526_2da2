package rcomp;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * TCP client for US86 — Pilot remote access.
 *
 * <pre>
 *   java -cp &lt;fat-jar&gt; rcomp.PilotClient [host] [port]
 * </pre>
 * Defaults: host = localhost, port = 2228.
 *
 * <p>Available commands after login:</p>
 * <pre>
 *   LOGIN &lt;email&gt; &lt;password&gt;
 *   LIST_DRAFT_PLANS
 *   LIST_ALL_PLANS
 *   IMPORT_FLIGHT_PLAN &lt;localFilePath.fp&gt;
 *   LIST_WEATHER_AREAS
 *   LIST_WEATHER_DATA &lt;areaCode&gt;
 *   INSERT_WEATHER &lt;flightPlanId&gt; &lt;weatherDataId&gt;
 *   TEST_FLIGHT_PLAN &lt;flightPlanId&gt;
 *   LOGOUT
 * </pre>
 */
public class PilotClient {

    private static final String DEFAULT_HOST = "localhost";
    private static final int    DEFAULT_PORT = 2228;

    public static void main(final String[] args) throws IOException {
        final String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        final int    port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        System.out.printf("[PILOT-CLIENT] Connecting to %s:%d%n", host, port);

        try (
            final Socket        socket = new Socket(host, port);
            final BufferedReader in    = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final PrintWriter    out   = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            final Scanner        kbd   = new Scanner(System.in)
        ) {
            System.out.println("[PILOT-CLIENT] Connected.");
            printHelp();

            while (kbd.hasNextLine()) {
                final String input = kbd.nextLine().trim();
                if (input.isEmpty()) continue;

                // IMPORT_FLIGHT_PLAN is handled locally (file → DSL lines)
                if (input.toUpperCase().startsWith(Protocol.IMPORT_FLIGHT_PLAN)) {
                    handleImport(input, in, out);
                    continue;
                }

                out.println(input);
                readResponse(in);

                if (input.toUpperCase().startsWith(Protocol.LOGOUT)) break;
            }
        }

        System.out.println("[PILOT-CLIENT] Connection closed.");
    }

    // ── IMPORT_FLIGHT_PLAN local handler ──────────────────────────────────────

    /**
     * Reads the local .fp file and streams it to the server.
     *
     * <pre>
     *   Client → IMPORT_FLIGHT_PLAN
     *   Server → OK Ready - send DSL lines then END_FILE
     *   Client → &lt;DSL lines&gt;
     *   Client → END_FILE
     *   Server → OK Flight plan(s) imported: ...  (or ERROR)
     *            END
     * </pre>
     */
    private static void handleImport(final String input, final BufferedReader in,
                                     final PrintWriter out) throws IOException {
        final String[] parts = input.split(" ", 2);
        if (parts.length < 2) {
            System.out.println("[PILOT-CLIENT] Usage: IMPORT_FLIGHT_PLAN <localFilePath.fp>");
            return;
        }
        final File file = new File(parts[1].trim());
        if (!file.exists() || !file.isFile()) {
            System.out.println("[PILOT-CLIENT] File not found: " + parts[1]);
            return;
        }

        // Tell the server we want to import
        out.println(Protocol.IMPORT_FLIGHT_PLAN);

        // Wait for OK Ready
        final String ack = in.readLine();
        System.out.println("[SERVER] " + ack);
        if (ack == null || !ack.startsWith(Protocol.OK)) {
            System.out.println("[PILOT-CLIENT] Server not ready.");
            return;
        }

        // Stream the file
        System.out.println("[PILOT-CLIENT] Sending: " + file.getName());
        try (final BufferedReader fr = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = fr.readLine()) != null) out.println(line);
        }
        out.println(Protocol.END_FILE);
        System.out.println("[PILOT-CLIENT] File sent. Waiting for result...");

        readResponse(in);
    }

    // ── Response reader ───────────────────────────────────────────────────────

    private static void readResponse(final BufferedReader in) throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            System.out.println("[SERVER] " + line);
            if (line.equals(Protocol.END) || line.equals(Protocol.BYE)) break;
        }
    }

    // ── Help ─────────────────────────────────────────────────────────────────

    private static void printHelp() {
        System.out.println("=== AlSafe Pilot Client — Available commands ===");
        System.out.println("  LOGIN <email> <password>");
        System.out.println("  LIST_DRAFT_PLANS");
        System.out.println("  LIST_ALL_PLANS");
        System.out.println("  IMPORT_FLIGHT_PLAN <localFilePath.fp>");
        System.out.println("  LIST_WEATHER_AREAS");
        System.out.println("  LIST_WEATHER_DATA <areaCode>");
        System.out.println("  INSERT_WEATHER <flightPlanId> <weatherDataId>");
        System.out.println("  TEST_FLIGHT_PLAN <flightPlanId>");
        System.out.println("  LOGOUT");
        System.out.println("================================================");
        System.out.println();
    }
}
