package ui;

import controller.RegisterAirControlAreaController;
import domain.AirControlArea;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * Console UI for US050 — Register an Air Control Area.
 *
 * <p>Reads area code, name, and geographic bounding box from the user,
 * delegates to {@link RegisterAirControlAreaController}, and reports
 * the outcome. Follows the same UI pattern used across the project.</p>
 */
public class RegisterAirControlAreaUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final RegisterAirControlAreaController ctrl =
                new RegisterAirControlAreaController();

        System.out.println("\n--- Register Air Control Area ---");

        System.out.println("\nExisting Air Control Areas:");

        final Iterable<AirControlArea> existingAreas = ctrl.allAreas();

        boolean hasAreas = false;

        for (final AirControlArea area : existingAreas) {
            hasAreas = true;
            System.out.printf(
                    " - %s | %s [ minLat: %.1f | maxLat: %.1f | minLon: %.1f | maxLat: %.1f]%n",
                    area.identity(),
                    area.name(),
                    area.boundary().minLat(),
                    area.boundary().maxLat(),
                    area.boundary().minLon(),
                    area.boundary().maxLon()

            );
        }

        if (!hasAreas) {
            System.out.println(" No air control areas registered yet.");
        }

        System.out.println();
        final String code = Console.readLine("Area code (e.g. LPPC):");
        final String name = Console.readLine("Area name (e.g. Lisbon ACC):");

        System.out.println("\nGeographic bounding box:");
        final double minLat = Console.readDouble("  Min latitude  (e.g. 36.0):");
        final double maxLat = Console.readDouble("  Max latitude  (e.g. 42.0):");
        final double minLon = Console.readDouble("  Min longitude (e.g. -10.0):");
        final double maxLon = Console.readDouble("  Max longitude (e.g. -6.0):");

        try {
            final AirControlArea area =
                    ctrl.registerAirControlArea(code, name, minLat, maxLat, minLon, maxLon);
            System.out.printf("%n>> Air control area '%s — %s' registered successfully.%n",
                    area.identity(), area.name());
        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "US050 - Register Air Control Area";
    }
}
