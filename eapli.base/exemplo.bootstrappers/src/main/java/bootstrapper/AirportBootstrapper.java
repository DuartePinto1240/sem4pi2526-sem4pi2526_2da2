package bootstrapper;

import controller.CreateAirportController;
import eapli.framework.actions.Action;

/**
 * Bootstrap action that pre-loads a set of well-known airports (US052).
 *
 * <p>Executed once at application startup via {@link Bootstrap}.
 * Airports that already exist (e.g. from a previous run) are silently skipped.</p>
 *
 * <p><b>Must run after {@code AirControlAreaBootstrapper}</b> (US050), as
 * every airport requires an existing air control area to be registered against.</p>
 */
public class AirportBootstrapper implements Action {

    @Override
    public boolean execute() {
        final CreateAirportController ctrl = new CreateAirportController();

        // Humberto Delgado Airport — Lisbon, Portugal (LPPC area)
        register(ctrl, "LPPT", "LIS", "Humberto Delgado Airport",
                "Lisbon", "Portugal",
                38.7756, -9.1354, 113.0, "LPPC");

        // Francisco Sá Carneiro Airport — Porto, Portugal (LPPC area)
        register(ctrl, "LPPR", "OPO", "Francisco Sa Carneiro Airport",
                "Porto", "Portugal",
                41.2481, -8.6814, 69.0, "LPPC");

        // Faro Airport — Algarve, Portugal (LPPC area)
        register(ctrl, "LPFR", "FAO", "Faro Airport",
                "Faro", "Portugal",
                37.0144, -7.9659, 24.0, "LPPC");

        // Madrid Barajas — Spain (LECM area)
        register(ctrl, "LEMD", "MAD", "Adolfo Suarez Madrid-Barajas Airport",
                "Madrid", "Spain",
                40.4936, -3.5668, 610.0, "LECM");

        // Paris Charles de Gaulle — France (LFFF area)
        register(ctrl, "LFPG", "CDG", "Paris Charles de Gaulle Airport",
                "Paris", "France",
                49.0097, 2.5479, 119.0, "LFFF");

        return true;
    }

    /**
     * Attempts to register a single airport, silently ignoring duplicates
     * or missing areas (which would indicate a bootstrap ordering issue).
     */
    private void register(final CreateAirportController ctrl,
                           final String icao, final String iata,
                           final String name, final String town, final String country,
                           final double lat, final double lon, final double elevation,
                           final String areaCode) {
        try {
            ctrl.createAirport(icao, iata, name, town, country,
                    lat, lon, elevation, areaCode);
        } catch (final Exception e) {
            // Already exists or area missing — skip silently
        }
    }
}
