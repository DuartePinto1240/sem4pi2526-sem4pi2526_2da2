package ui;

import controller.CreateAirportController;
import domain.AirControlArea;
import domain.Country;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

/**
 * Console UI for US052 — Create an Airport.
 *
 * <p>Flow:</p>
 * <ol>
 *   <li>Lists all existing air control areas with their bounding boxes.</li>
 *   <li>Operator selects one area by number.</li>
 *   <li>The selected area's bounding box is displayed as a coordinate guide.</li>
 *   <li>Operator enters ICAO code, IATA code, name, and town (free text).</li>
 *   <li>Operator selects country from the bootstrap-loaded list.</li>
 *   <li>Operator enters latitude, longitude (must be within the area boundary),
 *       and elevation.</li>
 *   <li>Controller validates all rules and persists the airport.</li>
 * </ol>
 */
public class CreateAirportUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final CreateAirportController ctrl = new CreateAirportController();

        System.out.println("\n--- Create Airport ---");

        // select air control area
        System.out.println("\nAvailable Air Control Areas:");

        final List<AirControlArea> areas = new ArrayList<>();
        for (final AirControlArea area : ctrl.allAreas()) {
            areas.add(area);
        }

        if (areas.isEmpty()) {
            System.out.println("  No air control areas registered yet.");
            System.out.println("  Please register an air control area first (US050).");
            return false;
        }

        for (int i = 0; i < areas.size(); i++) {
            final AirControlArea a = areas.get(i);
            System.out.printf("  %2d. [%s] %s%n", i + 1, a.identity(), a.name());
            System.out.printf("       Boundary → Lat [%.2f, %.2f]  Lon [%.2f, %.2f]%n",
                    a.boundary().minLat(), a.boundary().maxLat(),
                    a.boundary().minLon(), a.boundary().maxLon());
        }

        int areaChoice = -1;
        while (areaChoice < 1 || areaChoice > areas.size()) {
            areaChoice = Console.readInteger("\nSelect area number (1-" + areas.size() + "):");
            if (areaChoice < 1 || areaChoice > areas.size()) {
                System.out.println("  >> Invalid selection. Please try again.");
            }
        }

        final AirControlArea selectedArea = areas.get(areaChoice - 1);
        final String areaCode = selectedArea.identity().code();

        System.out.printf("%nSelected: [%s] %s%n", areaCode, selectedArea.name());
        System.out.printf("  Coordinates must be within: Lat [%.2f, %.2f]  Lon [%.2f, %.2f]%n",
                selectedArea.boundary().minLat(), selectedArea.boundary().maxLat(),
                selectedArea.boundary().minLon(), selectedArea.boundary().maxLon());

        // enter basic airport data
        System.out.println("\nAirport details:");
        final String icao = Console.readLine("  ICAO code (4 letters, e.g. LPPT):");
        final String iata = Console.readLine("  IATA code (3 letters, e.g. LIS):");
        final String name = Console.readLine("  Full name (e.g. Humberto Delgado Airport):");
        final String town = Console.readLine("  Town/City:");

        // select country from list
        System.out.println("\nAvailable Countries:");

        final List<Country> countries = new ArrayList<>();
        for (final Country c : ctrl.allCountries()) {
            countries.add(c);
        }

        if (countries.isEmpty()) {
            System.out.println("  No countries loaded. Please check the bootstrap.");
            return false;
        }

        // Print in columns of 4 for readability
        for (int i = 0; i < countries.size(); i++) {
            System.out.printf("  %3d. %-30s", i + 1, countries.get(i).name());
            if ((i + 1) % 4 == 0) System.out.println();
        }
        System.out.println();

        int countryChoice = -1;
        while (countryChoice < 1 || countryChoice > countries.size()) {
            countryChoice = Console.readInteger("Select country number (1-" + countries.size() + "):");
            if (countryChoice < 1 || countryChoice > countries.size()) {
                System.out.println("  >> Invalid selection. Please try again.");
            }
        }

        final String countryName = countries.get(countryChoice - 1).name();
        System.out.println("  Selected: " + countryName);

        // enter coordinates and elevation
        System.out.println("\nCoordinates (must be within the selected area boundary):");
        final double latitude  = Console.readDouble("  Latitude  (decimal degrees):");
        final double longitude = Console.readDouble("  Longitude (decimal degrees):");
        final double elevationM = Console.readDouble("\n  Elevation in metres above sea level (may be negative):");

        // delegate to controller
        try {
            final domain.Airport airport = ctrl.createAirport(
                    icao, iata, name, town, countryName,
                    latitude, longitude, elevationM, areaCode);

            System.out.printf("%n>> Airport '%s (%s)' registered successfully in area '%s'.%n",
                    airport.identity(), airport.iataCode(), airport.airControlAreaCode());

        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Create Airport";
    }
}
