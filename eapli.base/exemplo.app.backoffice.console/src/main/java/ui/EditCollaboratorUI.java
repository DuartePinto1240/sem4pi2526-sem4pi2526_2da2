package ui;

import controller.EditCollaboratorController;
import dto.CollaboratorDTO;
import dto.CustomerDTO;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.List;

public class EditCollaboratorUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final EditCollaboratorController ctrl = new EditCollaboratorController();

        System.out.println("\n--- Edit Customer's Collaborator (US063) ---");
        System.out.println("  Note: only email and phone number may be changed.");

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
        final int ci = Console.readInteger("Select collaborator to edit:") - 1;
        if (ci < 0 || ci >= collaborators.size()) {
            System.out.println(">> Invalid selection.");
            return false;
        }
        final CollaboratorDTO target = collaborators.get(ci);

        System.out.printf("%nCurrent email: %s%n", target.email);
        final String newEmail = Console.readLine("New email (Enter to keep current):");
        final String newPhone = Console.readLine("New phone (9 digits):");

        final String resolvedEmail = (newEmail == null || newEmail.isBlank())
                ? target.email : newEmail.trim();

        try {
            ctrl.editCollaborator(
                    selected.identity, selected.type,
                    target.email, resolvedEmail, newPhone);
            System.out.println("\n>> Collaborator updated successfully.");
        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() { return "US063 - Edit Customer's Collaborator"; }
}