package bootstrapper;

import controller.RegisterAirTransportCompanyController;
import eapli.framework.actions.Action;

/**
 * Bootstrap action that pre-loads a set of well-known air transport companies.
 *
 * <p>Executed once at application startup. Companies that already exist
 * in the database (e.g. from a previous run) are silently skipped.</p>
 */
public class AirTransportCompanyBootstrapper implements Action {

    /**
     * Registers the default set of air transport companies.
     *
     * @return {@code true} always
     */
    @Override
    public boolean execute() {
        final RegisterAirTransportCompanyController ctrl =
                new RegisterAirTransportCompanyController();

        register(ctrl, "TAP Air Portugal", "TP", "TAP");
        register(ctrl, "Ryanair",          "FR", "RYR");
        register(ctrl, "EasyJet",          "U2", "EZY");
        register(ctrl, "Lufthansa",        "LH", "DLH");
        register(ctrl, "Air France",       "AF", "AFR");
        return true;
    }

    /**
     * Attempts to register a single company, silently ignoring duplicates.
     *
     * @param ctrl the registration controller
     * @param name company name
     * @param iata IATA code (2 letters)
     * @param icao ICAO code (2-3 letters)
     */
    private void register(final RegisterAirTransportCompanyController ctrl,
                          final String name,
                          final String iata,
                          final String icao) {
        try {
            ctrl.registerCompany(name, iata, icao);
        } catch (final Exception e) {
            // Company already exists or validation failed — skip silently
        }
    }
}
