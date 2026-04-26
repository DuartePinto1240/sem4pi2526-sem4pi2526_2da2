package ui;

import controller.ListUsersController;
import controller.ToggleUserController;
import domain.User;
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
        System.out.println("|    Enable / Disable User                 |");
        System.out.println("+------------------------------------------+");

        printUserList();

        final String email = Console.readLine("\nUser email to change (press ENTER to cancel):");
        if (email == null || email.isBlank()) {
            System.out.println("Operation cancelled.");
            return false;
        }

        System.out.println("\n  1. Enable user");
        System.out.println("  2. Disable user");
        final int option = Console.readInteger("Choice");

        try {
            switch (option) {
                case 1:
                    toggleController.activateUser(email);
                    System.out.println(">> User enabled successfully.");
                    break;
                case 2:
                    toggleController.deactivateUser(email);
                    System.out.println(">> User disabled successfully.");
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
     * Prints a formatted table of all registered users.
     */
    private void printUserList() {
        System.out.printf("%n%-20s | %-25s | %-15s | %-9s | %s%n",
                "Name", "Email", "Staff No.", "Phone", "Status");
        System.out.println("-".repeat(88));

        boolean hasUsers = false;
        for (User u : listController.allUsers()) {
            System.out.println(u);
            hasUsers = true;
        }
        if (!hasUsers) {
            System.out.println("  (no users registered)");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return the title of this UI panel
     */
    @Override
    public String headline() {
        return "Enable/Disable User";
    }
}
