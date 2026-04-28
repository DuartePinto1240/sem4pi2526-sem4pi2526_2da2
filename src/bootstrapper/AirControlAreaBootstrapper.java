package bootstrapper;

import controller.RegisterAirControlAreaController;
import eapli.framework.actions.Action;

/**
 * Bootstrap action that pre-loads a set of well-known air control areas (US050).
 *
 * <p>Executed once at application startup via {@link Bootstrap}.
 * Areas that already exist (e.g. from a previous run) are silently skipped.</p>
 *
 * <p><b>Must run before {@code AirportBootstrapper}</b> (US052), as airports
 * require an existing air control area to be registered against.</p>
 */
public class AirControlAreaBootstrapper implements Action {

    @Override
    public boolean execute() {
        final RegisterAirControlAreaController ctrl =
                new RegisterAirControlAreaController();

        // Portugal / Lisbon ACC  — roughly covers mainland Portugal
        register(ctrl, "LPPC", "Lisbon ACC",
                36.0, 42.2, -10.0, -6.0);

        // Portugal / Santa Maria ACC — covers the Azores FIR
        register(ctrl, "LPPO", "Santa Maria ACC",
                27.0, 43.0, -35.0, -15.0);

        // Spain / Madrid ACC
        register(ctrl, "LECM", "Madrid ACC",
                36.0, 44.0, -9.5, 4.5);

        // France / Paris ACC
        register(ctrl, "LFFF", "Paris ACC",
                43.0, 51.5, -5.0, 8.5);

        return true;
    }

    /**
     * Attempts to register a single area, silently ignoring duplicates.
     */
    private void register(final RegisterAirControlAreaController ctrl,
                          final String code,
                          final String name,
                          final double minLat, final double maxLat,
                          final double minLon, final double maxLon) {
        try {
            ctrl.registerAirControlArea(code, name, minLat, maxLat, minLon, maxLon);
        } catch (final Exception e) {
            // Already exists or validation failed — skip silently
        }
    }
}
