// ─── ui/ExportWeatherDataUI.java ─────────────────────────────────────────

package ui;

import controller.ExportWeatherDataController;
import dto.WeatherDataDTO;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.List;

public class ExportWeatherDataUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final ExportWeatherDataController ctrl =
                new ExportWeatherDataController();

        System.out.println("\n--- Export Weather Data for Simulation (US110) ---");

        final List<WeatherDataDTO> records = ctrl.availableWeatherData();

        if (records.isEmpty()) {
            System.out.println("\n>> No weather data found.");
            System.out.println("   Import weather data first (option 1 or 2).");
            return false;
        }

        System.out.printf("%n  %d weather record(s) available for export.%n",
                records.size());

        final String defaultPath = "../Scomp/CW_wind.csv";
        System.out.println("  Default export path: " + defaultPath);
        final String input = Console.readLine(
                "Export path (Enter for default):");
        final String path = (input == null || input.isBlank())
                ? defaultPath : input.trim();

        try {
            final String exported = ctrl.exportToCsv(path);
            System.out.printf("%n>> Weather data exported successfully to '%s'.%n",
                    exported);
            System.out.println(
                    "   The simulation will load this file automatically.");
        } catch (final IllegalStateException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "US110 - Export Weather Data for Simulation";
    }
}