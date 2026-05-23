package ui;

import controller.ListUsersController;
import controller.ToggleUserController;
import dto.UserDTO;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * Console UI for US032 — Enable/Disable a backoffice user.
 *
 * <p>Displays the current user list so the administrator can choose
 * which account to activate or deactivate, then delegates the action
 * to {@link ToggleUserController}.</p>
 *
 * <p>Only accessible when logged in as an administrator.</p>
 */
public class ToggleUserUI extends AbstractUI {

    /** Controller that handles activate/deactivate logic. */
    private final ToggleUserController toggleController = new ToggleUserController();

    /** Controller used to display the current user list. */
    private final ListUsersController  listController   = new ListUsersController();

    /**
     * Displays the user list, prompts for an email and action, then executes.
     *
     * @return {@code false} to return to the calling menu
     */
    @Override
    protected boolean doShow() {
        System.out.println("\n+------------------------------------------+");
        System.out.println("|  Enable / Disable BackOffice Operator    |");
        System.out.println("+------------------------------------------+");

        printUserList();

        final String email = Console.readLine("\nOperator email to change (press ENTER to cancel):");
        if (email == null || email.isBlank()) {
            System.out.println("Operation cancelled.");
            return false;
        }

        System.out.println("\n  1. Enable operator");
        System.out.println("  2. Disable operator");
        final int option = Console.readInteger("Choice");

        try {
            switch (option) {
                case 1:
                    toggleController.activateUser(email);
                    System.out.println(">> Operator enabled successfully.");
                    break;
                case 2:
                    toggleController.deactivateUser(email);
                    System.out.println(">> Operator disabled successfully.");
                    break;
                default:
                    System.out.println(">> Invalid option.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println(">> ERROR: " + e.getMessage());
        }

        return false;
    }

    /**
     * Prints a formatted table of all BackOffice Operators.
     * Columns: Name | Email | Staff No. | Phone | Status
     */
    private void printUserList() {
        final String header = String.format("%-22s | %-28s | %-12s | %-9s | %s",
                "Name", "Email", "Staff No.", "Phone", "Status");
        System.out.println();
        System.out.println(header);
        System.out.println("-".repeat(header.length() + 5));

        boolean hasUsers = false;
        for (UserDTO u : listController.allUsers()) {
            System.out.printf("%-22s | %-28s | %-12s | %-9s | %s%n",
                    u.getName(), u.getEmail(), u.getStaffNumber(),
                    u.getPhoneNumber(), u.isActive() ? "ACTIVE" : "INACTIVE");
            hasUsers = true;
        }
        if (!hasUsers) {
            System.out.println("  (no BackOffice Operators registered)");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return the title of this UI panel
     */
    @Override
    public String headline() {
        return "Enable/Disable BackOffice Operator";
    }
}
