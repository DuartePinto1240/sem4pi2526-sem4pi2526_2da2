package ui;

import controller.ListPilotRosterController;
import domain.AirTransportCompany;
import dto.PilotDTO;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

public class ListPilotRosterUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final ListPilotRosterController ctrl = new ListPilotRosterController();

        System.out.println("\n--- List Pilot Roster ---");

        final AirTransportCompany company = selectCompany(ctrl);
        if (company == null) return false;

        final List<PilotDTO> pilots = ctrl.activePilotsOfMyCompany(company);

        printRoster(pilots);
        return false;
    }

    private AirTransportCompany selectCompany(final ListPilotRosterController ctrl) {
        final List<AirTransportCompany> companies = new ArrayList<>();
        ctrl.availableCompanies().forEach(companies::add);

        if (companies.isEmpty()) {
            System.out.println("\n>> No air transport companies registered in the system.");
            return null;
        }

        System.out.println("\nAvailable companies:");
        for (int i = 0; i < companies.size(); i++) {
            final AirTransportCompany c = companies.get(i);
            System.out.printf("  %d. %s (%s)%n", i + 1, c.name(), c.companyCode());
        }

        final int choice = Console.readInteger("Select company:");
        if (choice < 1 || choice > companies.size()) {
            System.out.println(">> Invalid option. Operation cancelled.");
            return null;
        }

        return companies.get(choice - 1);
    }

    private void printRoster(final List<PilotDTO> pilots) {
        System.out.println();
        System.out.println("┌──────────────────────────────┬───────────────────────────────┬─────────────────┐");
        System.out.println("│ Name                         │ Email                         │ Phone           │");
        System.out.println("├──────────────────────────────┼───────────────────────────────┼─────────────────┤");

        if (pilots.isEmpty()) {
            System.out.println("│              No active pilots found for this company.                        │");
            System.out.println("└──────────────────────────────┴───────────────────────────────┴─────────────────┘");
            return;
        }

        for (final PilotDTO p : pilots) {
            System.out.printf("│ %-28s │ %-29s │ %-15s │%n",
                    p.name, p.email, p.phoneNumber);

            final String models = String.join(", ", p.certifiedModels);
            System.out.printf("│   Certified models: %-59s│%n", models);
            System.out.println("├──────────────────────────────┼───────────────────────────────┼─────────────────┤");
        }

        System.out.println("└──────────────────────────────┴───────────────────────────────┴─────────────────┘");
    }

    @Override
    public String headline() {
        return "US076 - List Pilot Roster";
    }
}