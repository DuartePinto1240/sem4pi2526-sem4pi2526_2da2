package ui;

import controller.RegisterWeatherDataController;
import domain.AirControlArea;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Console UI for US041 — Register Weather Data.
 *
 * <p>Interaction flow:</p>
 * <ol>
 *   <li>List all existing air control areas with their boundaries.</li>
 *   <li>User chooses one area by code.</li>
 *   <li>User enters the sub-area bounding box (must be inside the chosen area).</li>
 *   <li>User enters start and end date/time of the observation period.</li>
 *   <li>User enters wind direction (0–359°) and wind speed (≥ 0 m/s).</li>
 *   <li>Delegate to {@link RegisterWeatherDataController} and report outcome.</li>
 * </ol>
 *
 * <p>The area's boundary coordinates are shown so the user knows what
 * values are valid for the sub-area — this is the mechanism used to guide
 * users and avoid "gaps" in coverage (the professor recommended that
 * sub-areas cover the whole air control area).</p>
 */
public class RegisterWeatherDataUI extends AbstractUI {

    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    protected boolean doShow() {
        final RegisterWeatherDataController ctrl = new RegisterWeatherDataController();

        System.out.println("\n--- Register Weather Data (US041) ---");

        // Step 1 — list areas
        System.out.println("\nAvailable Air Control Areas:");
        boolean hasAreas = false;
        for (final AirControlArea area : ctrl.allAreas()) {
            hasAreas = true;
            System.out.printf(
                    "  %-6s | %-20s | Lat[%.2f, %.2f]  Lon[%.2f, %.2f]%n",
                    area.identity().code(),
                    area.name(),
                    area.boundary().minLat(),
                    area.boundary().maxLat(),
                    area.boundary().minLon(),
                    area.boundary().maxLon());
        }
        if (!hasAreas) {
            System.out.println("  No air control areas registered. Please register one first.");
            return false;
        }

        // Step 2 — choose area
        final String areaCode = Console.readLine("\nArea code:");

        // Step 3 — sub-area
        System.out.println("\nSub-area bounding box (must lie inside the chosen area):");
        final double subMinLat = Console.readDouble("  Sub-area min latitude:");
        final double subMaxLat = Console.readDouble("  Sub-area max latitude:");
        final double subMinLon = Console.readDouble("  Sub-area min longitude:");
        final double subMaxLon = Console.readDouble("  Sub-area max longitude:");

        // Step 4 — time period
        System.out.println("\nObservation period (format: yyyy-MM-dd HH:mm):");
        final LocalDateTime start = readDateTime("  Start date/time:");
        if (start == null) return false;
        final LocalDateTime end = readDateTime("  End date/time:");
        if (end == null) return false;

        // Step 5 — wind
        System.out.println("\nWind conditions:");
        final double direction = Console.readDouble("  Wind direction (0–359 degrees):");
        final double speed     = Console.readDouble("  Wind speed (m/s, >= 0):");

        // Step 6 — delegate
        try {
            ctrl.registerWeatherData(
                    areaCode,
                    subMinLat, subMaxLat, subMinLon, subMaxLon,
                    start, end,
                    direction, speed);
            System.out.println("\n>> Weather data registered successfully.");
        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "US041 - Register Weather Data";
    }

    /**
     * Reads a date/time from the console and parses it.
     *
     * @param prompt the prompt to display
     * @return the parsed {@link LocalDateTime}, or {@code null} on parse error
     */
    private LocalDateTime readDateTime(final String prompt) {
        final String raw = Console.readLine(prompt);
        try {
            return LocalDateTime.parse(raw.trim(), DATETIME_FMT);
        } catch (final DateTimeParseException e) {
            System.out.println(
                    ">> ERROR: Invalid date/time format. Expected yyyy-MM-dd HH:mm");
            return null;
        }
    }
}
