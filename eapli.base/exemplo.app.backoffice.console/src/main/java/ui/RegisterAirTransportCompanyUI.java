package ui;

import controller.RegisterAirTransportCompanyController;
import domain.AirTransportCompany;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class RegisterAirTransportCompanyUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        // instancia o controller AQUI dentro, após o login
        final RegisterAirTransportCompanyController ctrl =
                new RegisterAirTransportCompanyController();

        System.out.println("\n--- Register Air Transport Company ---");

        final String name = Console.readLine("Company name:");
        final String iata = Console.readLine("IATA code (2 letters):");
        final String icao = Console.readLine("ICAO code (2-3 letters):");

        try {
            final AirTransportCompany company =
                    ctrl.registerCompany(name, iata, icao);
            System.out.printf("%n>> Company '%s' registered successfully.%n",
                    company.name());
        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "US060 - Register Air Transport Company";
    }
}