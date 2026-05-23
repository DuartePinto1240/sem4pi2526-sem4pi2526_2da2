package ui;

import controller.ListUsersController;
import dto.UserDTO;
import eapli.framework.presentation.console.AbstractUI;

/**
 * Console UI for US033 — List backoffice users.
 *
 * <p>Displays a formatted table of all registered users sorted alphabetically
 * by name, including name, email, role, phone, status, security clearance
 * expiry and next skills assessment date.</p>
 *
 * <p>Uses {@link UserDTO} to decouple the UI from the domain model.</p>
 */
public class ListUsersUI extends AbstractUI {

    private final ListUsersController theController = new ListUsersController();

    /**
     * Fetches all users as DTOs and renders them as a formatted table.
     *
     * @return {@code false} to return to the admin menu
     */
    @Override
    protected boolean doShow() {
        System.out.println("\n+--------------------------------------------+");
        System.out.println("|        BackOffice Operator List            |");
        System.out.println("+--------------------------------------------+");

        final String header = String.format("%-22s | %-28s | %-22s | %-12s | %-9s | %-8s | %-10s | %s",
                "Name", "Email", "Role", "Staff No.", "Phone", "Status",
                "Clearance", "Next Assessment");
        System.out.println(header);
        System.out.println("-".repeat(header.length() + 5));

        int total = 0, active = 0;
        for (UserDTO u : theController.allUsers()) {
            System.out.println(u);
            total++;
            if (u.isActive()) active++;
        }

        if (total == 0) {
            System.out.println("  (no BackOffice Operators registered)");
        } else {
            System.out.println("-".repeat(header.length() + 5));
            System.out.printf("Total: %d user(s)  |  Active: %d  |  Inactive: %d%n",
                    total, active, total - active);
        }

        return false;
    }

    @Override
    public String headline() {
        return "List BackOffice Operators";
    }
}
