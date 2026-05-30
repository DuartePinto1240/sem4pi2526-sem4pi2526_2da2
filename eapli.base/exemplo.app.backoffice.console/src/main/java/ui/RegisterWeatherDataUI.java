package ui;

import controller.RegisterWeatherDataController;
import domain.AirControlArea;
import domain.WeatherData;
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
 *   <li>List all air control areas with their bounding box coordinates.</li>
 *   <li>User chooses one by code.</li>
 *   <li>Show existing weather data sub-areas for that area so the user can
 *       see what is already covered and choose non-conflicting coordinates.</li>
 *   <li>User enters the sub-area bounding box (must lie inside the area).</li>
 *   <li>User enters altitude range (must lie inside the area).</li>
 *   <li>User enters observation period start and end (end must be after start).</li>
 *   <li>User enters wind direction (0–359°) and speed (≥ 0 m/s).</li>
 *   <li>Delegate to {@link RegisterWeatherDataController} and report outcome.</li>
 * </ol>
 */
public class RegisterWeatherDataUI extends AbstractUI {

    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    protected boolean doShow() {
        final RegisterWeatherDataController ctrl = new RegisterWeatherDataController();

        System.out.println("\n--- Register Weather Data ---\n");

        // list all areas
        System.out.println("Available Air Control Areas:");
        boolean hasAreas = false;
        for (final AirControlArea area : ctrl.allAreas()) {
            hasAreas = true;
            System.out.printf("  %-6s | %-20s | Lat[%.2f, %.2f]  Lon[%.2f, %.2f] | Alt: %.0f→%.0f m%n",
                    area.identity().code(),
                    area.name(),
                    area.boundary().minLat(),
                    area.boundary().maxLat(),
                    area.boundary().minLon(),
                    area.boundary().maxLon(),
                    area.boundary().minAltitude(),
                    area.boundary().maxAltitude());
        }
        if (!hasAreas) {
            System.out.println("  No air control areas registered. Register one first.");
            return false;
        }

        // choose area
        final String areaCode = Console.readLine("\nArea code: ").trim().toUpperCase();

        // sub-area bounding box
        System.out.println("\nSub-area bounding box (must lie inside the chosen area):");
        final double subMinLat = Console.readDouble("  Sub-area min latitude: ");
        final double subMaxLat = Console.readDouble("  Sub-area max latitude: ");
        final double subMinLon = Console.readDouble("  Sub-area min longitude: ");
        final double subMaxLon = Console.readDouble("  Sub-area max longitude: ");

        System.out.println("\nAltitude range (metres, 0–14000):");
        final double minAlt = Console.readDouble("  Min altitude (m): ");
        final double maxAlt = Console.readDouble("  Max altitude (m): ");

        // observation period
        System.out.println("\nObservation period (format: yyyy-MM-dd HH:mm):");
        final LocalDateTime start = readDateTime("  Start date/time: ");
        if (start == null) return false;
        final LocalDateTime end = readDateTime("  End date/time: ");
        if (end == null) return false;

        // wind conditions
        System.out.println("\nWind conditions:");
        final double direction = Console.readDouble("  Wind direction (0–359 degrees): ");
        final double speed     = Console.readDouble("  Wind speed (m/s, >= 0): ");

        // delegate and report
        try {
            final WeatherData result = ctrl.registerWeatherData(
                    areaCode,
                    subMinLat, subMaxLat, subMinLon, subMaxLon,
                    minAlt, maxAlt,
                    start, end,
                    direction, speed);
            System.out.printf(
                    "%n>> Weather data registered successfully.%n",
                    result.identity());
        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Register Weather Data";
    }

    private LocalDateTime readDateTime(final String prompt) {
        final String raw = Console.readLine(prompt);
        try {
            return LocalDateTime.parse(raw.trim(), DATETIME_FMT);
        } catch (final DateTimeParseException e) {
            System.out.println(">> ERROR: Invalid format. Expected yyyy-MM-dd HH:mm");
            return null;
        }
    }
}
