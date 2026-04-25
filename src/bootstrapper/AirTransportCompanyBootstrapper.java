package bootstrapper;

import controller.RegisterAirTransportCompanyController;
import eapli.framework.actions.Action;

public class AirTransportCompanyBootstrapper implements Action {

    private final RegisterAirTransportCompanyController ctrl =
            new RegisterAirTransportCompanyController();

    @Override
    public boolean execute() {
        register("TAP Air Portugal", "TP", "TAP");
        register("Ryanair", "FR", "RYR");
        register("EasyJet", "U2", "EZY");
        register("Lufthansa", "LH", "DLH");
        register("Air France", "AF", "AFR");
        return true;
    }

    private void register(final String name,
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