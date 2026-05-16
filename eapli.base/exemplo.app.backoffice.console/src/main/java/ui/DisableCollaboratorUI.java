package ui;

import controller.DisableCollaboratorController;
import dto.CollaboratorDTO;
import dto.CustomerDTO;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.List;

public class DisableCollaboratorUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final DisableCollaboratorController ctrl = new DisableCollaboratorController();

        System.out.println("\n--- Disable Customer's Collaborator (US064) ---");

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

        final List<CollaboratorDTO> collaborators;
        try {
            collaborators = ctrl.listCollaborators(selected.identity, selected.type);
        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
            return false;
        }

        if (collaborators.isEmpty()) {
            System.out.println(">> No active collaborators found.");
            return false;
        }

        System.out.println("\nActive collaborators:");
        for (int i = 0; i < collaborators.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, collaborators.get(i));
        }
        final int ci = Console.readInteger("Select collaborator to disable:") - 1;
        if (ci < 0 || ci >= collaborators.size()) {
            System.out.println(">> Invalid selection.");
            return false;
        }
        final CollaboratorDTO target = collaborators.get(ci);

        final String confirm = Console.readLine(
                "\nDisable '" + target.name + "' (" + target.email + ")? [yes/no]:");
        if (!"yes".equalsIgnoreCase(confirm.trim())) {
            System.out.println(">> Operation cancelled.");
            return false;
        }

        try {
            ctrl.disableCollaborator(selected.identity, selected.type, target.email);
            System.out.println("\n>> Collaborator disabled successfully.");
        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() { return "US064 - Disable Customer's Collaborator"; }
}