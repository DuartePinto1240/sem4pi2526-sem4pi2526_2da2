package rcomp;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * TCP client for US44 — Weather Person remote access.
 *
 * <p>Run from the command line:</p>
 * <pre>
 *   java -cp &lt;fat-jar&gt; rcomp.WeatherClient [host] [port]
 * </pre>
 * <p>Defaults: host = localhost, port = 2226.</p>
 *
 * <p>Available commands after login:</p>
 * <pre>
 *   LOGIN &lt;email&gt; &lt;password&gt;
 *   LIST_AREAS
 *   REGISTER_WEATHER &lt;areaCode&gt; &lt;subMinLat&gt; &lt;subMaxLat&gt; &lt;subMinLon&gt; &lt;subMaxLon&gt;
 *                    &lt;minAlt&gt; &lt;maxAlt&gt; &lt;startISO&gt; &lt;endISO&gt; &lt;windDir&gt; &lt;windSpeed&gt;
 *   IMPORT_WEATHER &lt;localFilePath&gt;
 *   CONSULT_WEATHER &lt;areaCode&gt; &lt;yyyy-MM-dd&gt;
 *   LOGOUT
 * </pre>
 */
public class WeatherClient {

    private static final String DEFAULT_HOST = "localhost";
    private static final int    DEFAULT_PORT = 2226;

    public static void main(final String[] args) throws IOException {
        final String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        final int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        System.out.printf("[CLIENT] Connecting to %s:%d%n", host, port);

        try (
                final Socket socket = new Socket(host, port);
                final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                final Scanner kbd = new Scanner(System.in)
        ) {
            System.out.println("[CLIENT] Connected.");
            printHelp();

            while (kbd.hasNextLine()) {
                final String input = kbd.nextLine().trim();
                if (input.isEmpty()) continue;

                // IMPORT_WEATHER is handled locally
                if (input.toUpperCase().startsWith("IMPORT_WEATHER")) {
                    handleImport(input, in, out);
                    continue;
                }

                // All other commands: send as-is, read response
                out.println(input);
                readResponse(in);

                // If server said BYE, stop
                // (readResponse already printed it)
                if (input.toUpperCase().startsWith("LOGOUT")) break;
            }
        }

        System.out.println("[CLIENT] Connection closed.");
    }

    // Import helper
    /** Handles the local IMPORT_WEATHER command:
     * 1. Reads the file at the given path.
     * 2. Sends IMPORT_WEATHER to the server.
     * 3. Waits for the server's "OK Ready" acknowledgement.
     * 4. Sends each line of the file.
     * 5. Sends END_FILE.
     * 6. Reads the server's summary response.
     */
    private static void handleImport(final String input, final BufferedReader in, final PrintWriter out) throws IOException {
        final String[] parts = input.split(" ", 2);
        if (parts.length < 2) {
            System.out.println("[CLIENT] Usage: IMPORT_WEATHER <localFilePath>");
            return;
        }
        final String filePath = parts[1].trim();
        final File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            System.out.println("[CLIENT] File not found: " + filePath);
            return;
        }

        // Tell the server we want to import
        out.println(Protocol.IMPORT_WEATHER);

        // Read the server's "OK Ready" acknowledgement
        final String ack = in.readLine();
        System.out.println("[SERVER] " + ack);
        if (ack == null || !ack.startsWith(Protocol.OK)) {
            System.out.println("[CLIENT] Server not ready for import.");
            return;
        }

        // Stream the file to the server line by line
        System.out.println("[CLIENT] Sending file: " + filePath);
        long linesSent = 0;
        try (final BufferedReader fileReader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = fileReader.readLine()) != null) {
                out.println(line);
                linesSent++;
            }
        }
        out.println(Protocol.END_FILE);
        System.out.println("[CLIENT] File sent (" + linesSent + " lines). Waiting for result...");

        // Read the summary (OK imported=N rejected=M + optional ERROR lines + END)
        readResponse(in);
    }

    // Response reader
    /**
     * Reads lines from the server until it receives a terminal line
     * (END, OK, ERROR without more data, or BYE).
     */
    private static void readResponse(final BufferedReader in) throws IOException {
        final StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line).append("\n");
            System.out.println("[SERVER] " + line);
            if (line.equals(Protocol.END) || line.equals(Protocol.BYE) || (line.startsWith(Protocol.OK) && !multiLineFollows(line))
                    || (line.startsWith(Protocol.ERROR) && !multiLineFollows(line))) {
                break;
            }
        }
    }

    /**
     * Returns true if the line is the start of a multi-line response
     * (i.e. the next line should be read too).
     * For our protocol, OK/ERROR that precede a list end with END, so if
     * the line itself is not END/BYE we keep reading until END/BYE appears.
     */
    private static boolean multiLineFollows(final String line) {
        // A standalone OK <message> or ERROR <message> is single-line.
        // The multi-line cases are LIST_AREAS, CONSULT_WEATHER, and IMPORT summary,all of which send END at the end.
        // We detect them by checking whether the response already contains END — but since we process line by line
        // the simplest rule is: keep reading until we see END or BYE.
        return false; // handled by the loop condition above
    }

    // Help text
    private static void printHelp() {
        System.out.println("Available commands:");
        System.out.println("  LOGIN <email> <password>");
        System.out.println("  LIST_AREAS");
        System.out.println("  REGISTER_WEATHER <areaCode> <subMinLat> <subMaxLat> <subMinLon> <subMaxLon>");
        System.out.println("                   <minAlt> <maxAlt> <startISO> <endISO> <windDir> <windSpeed>");
        System.out.println("    (ISO format: yyyy-MM-ddTHH:mm  e.g. 2026-06-05T10:00)");
        System.out.println("  IMPORT_WEATHER <localFilePath.csv>");
        System.out.println("  CONSULT_WEATHER <areaCode> <yyyy-MM-dd>");
        System.out.println("  LOGOUT");
        System.out.println();
    }
}
