package rcomp;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ATCCClient {

    private static final String DEFAULT_HOST = "localhost";
    private static final int    DEFAULT_PORT = 2225;

    public static void main(final String[] args) throws IOException {
        final String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        final int    port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        System.out.printf("[CLIENT] Connecting to %s:%d%n", host, port);

        try (
                final Socket       socket = new Socket(host, port);
                final BufferedReader in   = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                final PrintWriter    out  = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream()), true);
                final Scanner        kbd  = new Scanner(System.in)
        ) {
            System.out.println("[CLIENT] Connected. Type commands:");
            System.out.println("  LOGIN <email> <password>");
            System.out.println("  LIST_AIRPORTS");
            System.out.println("  LIST_MODELS");
            System.out.println("  ADD_AIRCRAFT <reg> <country> <iata> <modelId> <eco> <bus> <first> <crew>");
            System.out.println("  LIST_FLEET <iata>");
            System.out.println("  LIST_FLEET_MODEL <iata> <modelId>");
            System.out.println("  LIST_FLEET_MAKER <iata> <maker>");
            System.out.println("  LIST_FLEET_CAP <iata> <minSeats>");
            System.out.println("  LIST_FLEET_AGE <iata>");
            System.out.println("  DECOMMISSION <reg>");
            System.out.println("  CREATE_ROUTE <designator> <companyIata> <origin> <dest> REGULAR <day> <dep> <arr>");
            System.out.println("  CREATE_ROUTE <designator> <companyIata> <origin> <dest> CHARTER <date1> <time1> <date2> <time2>");
            System.out.println("  LOGOUT");
            System.out.println();

            while (kbd.hasNextLine()) {
                final String command = kbd.nextLine().trim();
                if (command.isEmpty()) continue;

                out.println(command);

                // read response — may be multi-line until END or single line
                final StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line).append("\n");
                    if (line.equals(Protocol.END)
                            || line.startsWith(Protocol.OK)
                            || line.startsWith(Protocol.ERROR)
                            || line.equals(Protocol.BYE)) {
                        break;
                    }
                }

                System.out.print("[SERVER] " + response);

                if (response.toString().trim().equals(Protocol.BYE)) {
                    System.out.println("[CLIENT] Disconnected.");
                    break;
                }
            }
        }
    }
}