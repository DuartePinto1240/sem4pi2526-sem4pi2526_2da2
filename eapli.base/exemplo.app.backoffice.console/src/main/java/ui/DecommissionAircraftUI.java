package ui;

import controller.DecommissionAircraftController;
import domain.aircraft.Aircraft;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

public class DecommissionAircraftUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final DecommissionAircraftController ctrl =
                new DecommissionAircraftController();

        System.out.println("\n--- Decommission Aircraft ---");

        // TODO: obter companyIata do utilizador autenticado quando a sessão estiver disponível
        final String companyIata = Console.readLine("Company IATA code:");

        final List<Aircraft> active = new ArrayList<>();
        ctrl.activeAircraftOfCompany(companyIata).forEach(active::add);

        if (active.isEmpty()) {
            System.out.println("\n>> No active aircraft found for company '" + companyIata + "'.");
            return false;
        }

        System.out.println("\nActive aircraft:");
        for (int i = 0; i < active.size(); i++) {
            final Aircraft a = active.get(i);
            System.out.printf("  %d. %s | %s | registered: %s%n",
                    i + 1,
                    a.registration(),
                    a.cabinConfiguration(),
                    a.registrationDate());
        }

        final int choice = Console.readInteger("Select aircraft to decommission (number):");
        if (choice < 1 || choice > active.size()) {
            System.out.println(">> Invalid option. Operation cancelled.");
            return false;
        }

        final Aircraft selected = active.get(choice - 1);

        final String confirm = Console.readLine(
                "Are you sure you want to decommission '"
                        + selected.registration() + "'? (yes/no):");
        if (!"yes".equalsIgnoreCase(confirm.trim())) {
            System.out.println(">> Operation cancelled.");
            return false;
        }

        try {
            final Aircraft decommissioned = ctrl.decommissionAircraft(
                    selected.registration(), companyIata);
            System.out.printf("%n>> Aircraft '%s' has been decommissioned successfully.%n",
                    decommissioned.registration());
            System.out.printf("   Status: %s%n", decommissioned.operationalStatus());
        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Decommission Aircraft";
    }
}