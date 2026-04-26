package ui;

import controller.RegisterUserController;
import domain.EmailAddress;
import domain.User;
import domain.ValidEmailDomains;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import persistence.PersistenceContext;
import repositories.UserRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Console UI for US031 — Register a backoffice user.
 *
 * <p>Each field is validated individually with immediate retry on error.
 * Uniqueness constraints (email, phone, staff number) and domain validation
 * are checked against the database during input so the user never has to
 * restart the whole form.</p>
 *
 * <p>Only accessible from the administrator's menu.</p>
 */
public class RegisterUserUI extends AbstractUI {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RegisterUserController theController = new RegisterUserController();
    private final UserRepository repo = PersistenceContext.getInstance().userRepository();

    /**
     * Displays the registration form with per-field validation and retry.
     *
     * @return {@code false} to return to the admin menu
     */
    @Override
    protected boolean doShow() {
        System.out.println("\n+--------------------------------------------+");
        System.out.println("|     Register Backoffice User               |");
        System.out.println("+--------------------------------------------+");

        if (!ValidEmailDomains.isEmpty()) {
            System.out.println("  Valid email domains: " + ValidEmailDomains.getAll());
        }

        final String    name        = readName();
        final String    email       = readEmail();
        final String    phoneNumber = readPhone();
        final String    staffNumber = readStaffNumber();
        final String    password    = readPassword();
        final LocalDate clearance   = readClearanceDate();

        try {
            final User u = theController.registerUser(
                    email, name, phoneNumber, password, staffNumber, clearance);
            System.out.println("\n  >> User registered successfully!");
            System.out.printf("     Name: %s%n", u.getName());
            System.out.printf("     Email: %s%n", u.identity());
            System.out.printf("     Role: %s%n", u.getRole());
            System.out.printf("     Staff No.: %s%n", u.getStaffNumber());
            System.out.printf("     Clearance Expiry: %s%n", u.getSecurityClearanceExpiry());
            System.out.printf("     Next Assessment: %s%n", u.getNextSkillsAssessmentDate());
            System.out.println("     Status: ACTIVE");
        } catch (IllegalArgumentException e) {
            System.out.println("\n  >> ERROR: " + e.getMessage());
        }

        return false;
    }

    // -------------------------------------------------------------------------
    // Per-field readers
    // -------------------------------------------------------------------------

    private String readName() {
        while (true) {
            final String v = Console.readLine("  Full Name:");
            if (v == null || v.isBlank()) {
                System.out.println("  ! Name must not be blank. Please try again.");
            } else {
                return v.trim();
            }
        }
    }

    /**
     * Reads and validates email: format, domain, and uniqueness.
     *
     * @return valid, unique email in an accepted domain
     */
    private String readEmail() {
        while (true) {
            final String v = Console.readLine("  Email:");
            if (v == null || v.isBlank()) {
                System.out.println("  ! Email must not be blank. Please try again.");
                continue;
            }
            final String trimmed = v.trim();

            // Format check
            try {
                EmailAddress.valueOf(trimmed);
            } catch (Exception e) {
                System.out.println("  ! Invalid email format (e.g. user@alsafe.com). Please try again.");
                continue;
            }

            // Domain check
            if (!ValidEmailDomains.isEmpty() && !ValidEmailDomains.isValid(trimmed)) {
                System.out.println("  ! Email domain is not accepted. Valid domains: "
                        + ValidEmailDomains.getAll() + ". Please try again.");
                continue;
            }

            // Uniqueness check
            if (repo.ofIdentity(EmailAddress.valueOf(trimmed)).isPresent()) {
                System.out.println("  ! This email is already registered. Please use a different one.");
                continue;
            }

            return trimmed;
        }
    }

    /**
     * Reads and validates phone: 9 digits and unique.
     *
     * @return valid, unique 9-digit phone number
     */
    private String readPhone() {
        while (true) {
            final String v = Console.readLine("  Phone Number (9 digits):");
            if (v == null || v.isBlank()) {
                System.out.println("  ! Phone number must not be blank. Please try again.");
                continue;
            }
            final String digits = v.trim();
            if (!digits.matches("\\d{9}")) {
                System.out.println("  ! Must be exactly 9 digits (e.g. 912345678). Please try again.");
                continue;
            }
            boolean inUse = false;
            for (User u : repo.findAll()) {
                if (u.getPhoneNumber().equals(digits)) { inUse = true; break; }
            }
            if (inUse) {
                System.out.println("  ! This phone number is already registered. Please use a different one.");
                continue;
            }
            return digits;
        }
    }

    /**
     * Reads and validates staff number: not blank and unique.
     *
     * @return valid, unique staff number
     */
    private String readStaffNumber() {
        while (true) {
            final String v = Console.readLine("  Staff Number:");
            if (v == null || v.isBlank()) {
                System.out.println("  ! Staff number must not be blank. Please try again.");
                continue;
            }
            final String trimmed = v.trim();
            boolean inUse = false;
            for (User u : repo.findAll()) {
                if (u.getStaffNumber().equals(trimmed)) { inUse = true; break; }
            }
            if (inUse) {
                System.out.println("  ! This staff number is already registered. Please use a different one.");
                continue;
            }
            return trimmed;
        }
    }

    private String readPassword() {
        while (true) {
            final String v = Console.readLine("  Password (min. 4 chars):");
            if (v == null || v.length() < 4) {
                System.out.println("  ! Password must be at least 4 characters. Please try again.");
            } else {
                return v;
            }
        }
    }

    /**
     * Reads and validates the security clearance expiry date.
     * Format: YYYY-MM-DD. Must be a future date.
     *
     * @return valid future {@link LocalDate}
     */
    private LocalDate readClearanceDate() {
        while (true) {
            final String v = Console.readLine("  Security Clearance Expiry (YYYY-MM-DD):");
            if (v == null || v.isBlank()) {
                System.out.println("  ! Date must not be blank. Please try again.");
                continue;
            }
            try {
                final LocalDate date = LocalDate.parse(v.trim(), DATE_FMT);
                if (!date.isAfter(LocalDate.now())) {
                    System.out.println("  ! Date must be in the future. Please try again.");
                    continue;
                }
                return date;
            } catch (DateTimeParseException e) {
                System.out.println("  ! Invalid date format. Use YYYY-MM-DD (e.g. 2028-12-31). Please try again.");
            }
        }
    }

    @Override
    public String headline() {
        return "Register User";
    }
}
