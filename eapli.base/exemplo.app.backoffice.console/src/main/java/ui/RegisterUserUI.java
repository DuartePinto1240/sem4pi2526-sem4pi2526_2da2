package ui;

import controller.RegisterUserController;
import domain.EmailAddress;
import domain.User;
import domain.ValidEmailDomains;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import persistence.PersistenceContext;
import repositories.UserRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class RegisterUserUI extends AbstractUI {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RegisterUserController theController =
            new RegisterUserController();
    private final UserRepository repo =
            PersistenceContext.getInstance().userRepository();

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
        final Role      role        = readRole();

        try {
            final User u = theController.registerUserWithRole(
                    email, name, phoneNumber, password,
                    staffNumber, clearance, role);
            System.out.println("\n  >> User registered successfully!");
            System.out.printf("     Name  : %s%n", u.getName());
            System.out.printf("     Email : %s%n", u.identity());
            System.out.printf("     Role  : %s%n", role);
        } catch (IllegalArgumentException e) {
            System.out.println("\n  >> ERROR: " + e.getMessage());
        }

        return false;
    }

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
            } catch (Exception e) {
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

    private String readPhone() {
        while (true) {
            final String v = Console.readLine("  Phone Number (9 digits):");
            if (v == null || v.isBlank()) {
                System.out.println("  ! Phone must not be blank. Please try again.");
                continue;
            }
            final String digits = v.trim();
            if (!digits.matches("\\d{9}")) {
                System.out.println("  ! Must be exactly 9 digits. Please try again.");
                continue;
            }
            boolean inUse = false;
            for (User u : repo.findAll()) {
                if (u.getPhoneNumber().equals(digits)) { inUse = true; break; }
            }
            if (inUse) {
                System.out.println("  ! Phone already registered. Please use a different one.");
                continue;
            }
            return digits;
        }
    }

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
                System.out.println("  ! Staff number already registered. Please use a different one.");
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
                System.out.println("  ! Invalid format. Use YYYY-MM-DD. Please try again.");
            }
        }
    }

    private Role readRole() {
        final Role[] roles = theController.availableRoles();
        System.out.println("\n  Available roles:");
        for (int i = 0; i < roles.length; i++) {
            System.out.printf("    %d. %s%n", i + 1, roles[i].toString());
        }
        while (true) {
            final int choice = Console.readInteger("  Select role:");
            if (choice >= 1 && choice <= roles.length) {
                return roles[choice - 1];
            }
            System.out.println("  ! Invalid option. Please try again.");
        }
    }

    @Override
    public String headline() {
        return "Register User";
    }
}