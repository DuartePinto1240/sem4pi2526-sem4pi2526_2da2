package bootstrapper;

import controller.RegisterAirTransportCompanyController;
import eapli.framework.actions.Action;

public class AirTransportCompanyBootstrapper implements Action {

    @Override
    public boolean execute() {
        final RegisterAirTransportCompanyController ctrl =
                new RegisterAirTransportCompanyController();

        register(ctrl, "TAP Air Portugal", "TP", "TAP");
        register(ctrl, "Ryanair", "FR", "RYR");
        register(ctrl, "EasyJet", "U2", "EZY");
        register(ctrl, "Lufthansa", "LH", "DLH");
        register(ctrl, "Air France", "AF", "AFR");
        return true;
    }

    private void register(final RegisterAirTransportCompanyController ctrl,
                          final String name,
                          final String iata,
                          final String icao) {
        try {
            ctrl.registerCompany(name, iata, icao);
            System.out.printf("Bootstrapped company: %s%n", name);
        } catch (final Exception e) {
            System.out.printf("Skipped '%s': %s%n", name, e.getMessage());
        }
    }
}