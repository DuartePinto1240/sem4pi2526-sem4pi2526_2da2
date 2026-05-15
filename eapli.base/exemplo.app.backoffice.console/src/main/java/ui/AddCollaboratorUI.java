package ui;

import controller.AddCollaboratorController;
import dto.CustomerDTO;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.List;

public class AddCollaboratorUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final AddCollaboratorController ctrl = new AddCollaboratorController();

        System.out.println("\n--- Add Customer's Collaborator ---");

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

        final String email    = Console.readLine("Email:");
        final String name     = Console.readLine("Full name:");
        final String phone    = Console.readLine("Phone (9 digits):");
        final String staffNo  = Console.readLine("Staff number:");
        final String position = Console.readLine("Position:");
        final String password = Console.readLine("Initial password:");

        try {
            ctrl.addCollaborator(
                    selected.identity, selected.type,
                    email, name, phone, staffNo, position, password);
            System.out.println("\n>> Collaborator added successfully.");
        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() { return "Add Customer's Collaborator"; }
}