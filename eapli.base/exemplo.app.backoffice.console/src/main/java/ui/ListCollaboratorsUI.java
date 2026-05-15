package ui;

import controller.ListCollaboratorsController;
import dto.CollaboratorDTO;
import dto.CustomerDTO;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.List;

public class ListCollaboratorsUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final ListCollaboratorsController ctrl = new ListCollaboratorsController();

        System.out.println("\n--- List Customer's Collaborators ---");

        final List<CustomerDTO> customers = ctrl.getAvailableCustomers();
        if (customers.isEmpty()) {
            System.out.println(">> No customers registered yet.");
            return false;
        }

        System.out.println("\nAvailable customers:");
        for (int i = 0; i < customers.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, customers.get(i));
        }
        final int idx = Console.readInteger("Select customer:") - 1;
        if (idx < 0 || idx >= customers.size()) {
            System.out.println(">> Invalid selection.");
            return false;
        }
        final CustomerDTO selected = customers.get(idx);

        try {
            final List<CollaboratorDTO> list =
                    ctrl.listCollaborators(selected.identity, selected.type);

            if (list.isEmpty()) {
                System.out.println("\n>> No active collaborators found.");
            } else {
                System.out.printf("%n%-30s | %-25s | %-20s | %-12s%n",
                        "EMAIL", "NAME", "POSITION", "PHONE");
                System.out.println("-".repeat(95));
                list.forEach(c -> System.out.printf(
                        "  %-30s | %-25s | %-20s | %-12s%n",
                        c.email, c.name, c.position, c.phoneNumber));
            }
        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() { return "US062 - List Customer's Collaborators"; }
}