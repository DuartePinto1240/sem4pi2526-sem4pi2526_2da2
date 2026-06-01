package ui;

import controller.ConsultWeatherDataController;
import domain.AirControlArea;
import dto.WeatherDataDTO;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Console UI for US043 — Consult Weather Data.
 *
 * <p>Interaction flow:</p>
 * <ol>
 *   <li>List all air control areas.</li>
 *   <li>User selects an area by code.</li>
 *   <li>User enters a date to filter records.</li>
 *   <li>Display matching records (or inform if none exist).</li>
 * </ol>
 */
public class ConsultWeatherDataUI extends AbstractUI {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    protected boolean doShow() {
        final ConsultWeatherDataController ctrl = new ConsultWeatherDataController();

        // list available areas
        System.out.println("Available Air Control Areas:");
        boolean hasAreas = false;
        for (final AirControlArea area : ctrl.availableAreas()) {
            hasAreas = true;
            System.out.printf("  %-6s | %-20s | Lat[%.2f, %.2f]  Lon[%.2f, %.2f]%n",
                    area.identity().code(), area.name(),
                    area.boundary().minLat(), area.boundary().maxLat(),
                    area.boundary().minLon(), area.boundary().maxLon());
        }
        if (!hasAreas) {
            System.out.println("  No air control areas registered.");
            return false;
        }

        // choose area
        final String areaCode = Console.readLine("\nArea code: ").trim().toUpperCase();

        // enter date
        final LocalDate date = readDate("Date to consult (yyyy-MM-dd): ");
        if (date == null) return false;

        // query and display
        try {
            final List<WeatherDataDTO> records =
                    ctrl.weatherDataByAreaAndDate(areaCode, date);

            if (records.isEmpty()) {
                System.out.printf(
                        "%nNo weather data found for area '%s' on %s.%n", areaCode, date);
            } else {
                System.out.printf("%nWeather data for area '%s' on %s (%d record(s)):%n%n", areaCode, date, records.size());
                System.out.printf("  %-4s | %-6s | %-28s | %-22s | %-20s | %s%n",
                        "ID", "Area", "Sub-Area (Lat/Lon)", "Altitude (m)",
                        "Observation Period", "Wind");
                System.out.println("  " + "-".repeat(110));
                for (final WeatherDataDTO dto : records) {
                    System.out.printf("  %-4d | %-6s | Lat[%.2f;%.2f] Lon[%.2f;%.2f] "
                                    + "| Alt[%.0f, %.0f] | %s → %s | %.1f° / %.2f m/s%n",
                            dto.id, dto.areaCode, dto.subMinLat, dto.subMaxLat,
                            dto.subMinLon, dto.subMaxLon, dto.minAltitude, dto.maxAltitude,
                            dto.startDateTime, dto.endDateTime, dto.windDirection, dto.windSpeed);
                }
            }
        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Consult Weather Data";
    }

    private LocalDate readDate(final String prompt) {
        final String raw = Console.readLine(prompt);
        try {
            return LocalDate.parse(raw.trim(), DATE_FMT);
        } catch (final DateTimeParseException e) {
            System.out.println(">> ERROR: Invalid format. Expected yyyy-MM-dd");
            return null;
        }
    }
}
