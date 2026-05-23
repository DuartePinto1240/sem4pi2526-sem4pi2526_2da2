package ui;

import controller.AddCollaboratorController;
import domain.EmailAddress;
import domain.ValidEmailDomains;
import dto.CustomerDTO;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import persistence.PersistenceContext;
import repositories.UserRepository;

import java.util.List;

/**
 * Console UI for US061 — Add a Customer's Collaborator.
 *
 * <p>Validates each field inline (re-prompts on error) before submitting,
 * following the same pattern as {@link RegisterUserUI}.</p>
 */
public class AddCollaboratorUI extends AbstractUI {

    private final AddCollaboratorController ctrl   = new AddCollaboratorController();
    private final UserRepository            repo   = PersistenceContext.getInstance().userRepository();

    @Override
    protected boolean doShow() {
        System.out.println("\n--- Add Customer's Collaborator ---");

        // --- Select customer ---
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

        // --- Collect and validate fields inline ---
        final String email    = readEmail();
        final String name     = readName();
        final String phone    = readPhone();
        final String staffNo  = readStaffNumber();
        final String position = readPosition();
        final String password = readPassword();

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

    private String readEmail() {
        while (true) {
            final String v = Console.readLine("  Email:");
            if (v == null || v.isBlank()) {
                System.out.println("  ! Email must not be blank. Please try again.");
                continue;
            }
            final String trimmed = v.trim();
            try {
                EmailAddress.valueOf(trimmed);
            } catch (final Exception e) {
                System.out.println("  ! Invalid email format. Please try again.");
                continue;
            }
            if (!ValidEmailDomains.isEmpty() && !ValidEmailDomains.isValid(trimmed)) {
                System.out.println("  ! Email domain not accepted. Valid: "
                        + ValidEmailDomains.getAll());
                continue;
            }
            if (repo.ofIdentity(EmailAddress.valueOf(trimmed)).isPresent()) {
                System.out.println("  ! Email already registered. Please use a different one.");
                continue;
            }
            return trimmed;
        }
    }

    private String readName() {
        while (true) {
            final String v = Console.readLine("  Full name:");
            if (v == null || v.isBlank()) {
                System.out.println("  ! Name must not be blank. Please try again.");
            } else {
                return v.trim();
            }
        }
    }

    private String readPhone() {
        while (true) {
            final String v = Console.readLine("  Phone (9 digits):");
            if (v == null || v.isBlank()) {
                System.out.println("  ! Phone must not be blank. Please try again.");
                continue;
            }
            final String digits = v.trim();
            if (!digits.matches("\\d{9}")) {
                System.out.println("  ! Must be exactly 9 digits. Please try again.");
                continue;
            }
            if (repo.existsByPhone(digits)) {
                System.out.println("  ! Phone number already registered. Please use a different one.");
                continue;
            }
            return digits;
        }
    }

    private String readStaffNumber() {
        while (true) {
            final String v = Console.readLine("  Staff number:");
            if (v == null || v.isBlank()) {
                System.out.println("  ! Staff number must not be blank. Please try again.");
                continue;
            }
            final String trimmed = v.trim();
            if (repo.existsByStaffNumber(trimmed)) {
                System.out.println("  ! Staff number already registered. Please use a different one.");
                continue;
            }
            return trimmed;
        }
    }

    private String readPosition() {
        while (true) {
            final String v = Console.readLine("  Position:");
            if (v == null || v.isBlank()) {
                System.out.println("  ! Position must not be blank. Please try again.");
            } else {
                return v.trim();
            }
        }
    }

    private String readPassword() {
        while (true) {
            final String v = Console.readLine("  Initial password (min. 4 chars):");
            if (v == null || v.length() < 4) {
                System.out.println("  ! Password must be at least 4 characters. Please try again.");
            } else {
                return v;
            }
        }
    }

    @Override
    public String headline() { return "Add Customer's Collaborator"; }
}
