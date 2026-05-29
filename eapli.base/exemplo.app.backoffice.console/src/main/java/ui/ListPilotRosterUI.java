package ui;

import controller.ListPilotRosterController;
import domain.pilot.Pilot;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class ListPilotRosterUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final ListPilotRosterController ctrl = new ListPilotRosterController();

        System.out.println("\n--- List Pilot Roster ---");

        final String iata = Console.readLine("Your company IATA code:");
        final String icao = Console.readLine("Your company ICAO code:");

        final Iterable<Pilot> pilots = ctrl.activePilotsOfMyCompany(iata, icao);

        printRoster(pilots);
        return false;
    }

    private void printRoster(final Iterable<Pilot> pilots) {
        System.out.println();
        System.out.println("┌──────────────────────────────┬───────────────────────────────┬─────────────────┐");
        System.out.println("│ Name                         │ Email                         │ Phone           │");
        System.out.println("├──────────────────────────────┼───────────────────────────────┼─────────────────┤");

        boolean empty = true;
        for (final Pilot p : pilots) {
            empty = false;
            System.out.printf("│ %-28s │ %-29s │ %-15s │%n",
                    p.name(), p.email(), p.phoneNumber());

            final StringBuilder models = new StringBuilder();
            p.certifiedModels().forEach(m ->
                    models.append(m.modelName()).append(" (").append(m.manufacturer()).append("), "));
            if (models.length() > 2)
                models.setLength(models.length() - 2);

            System.out.printf("│   Certified models: %-59s│%n", models);
            System.out.println("├──────────────────────────────┼───────────────────────────────┼─────────────────┤");
        }

        if (empty) {
            System.out.println("│              No active pilots found for this company.                        │");
            System.out.println("└──────────────────────────────┴───────────────────────────────┴─────────────────┘");
        } else {
            System.out.println("└──────────────────────────────┴───────────────────────────────┴─────────────────┘");
        }
    }

    @Override
    public String headline() {
        return "US076 - List Pilot Roster";
    }
}