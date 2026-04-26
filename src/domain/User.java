package domain;

import eapli.framework.domain.model.AggregateRoot;
import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Aggregate root representing a system user of AlSafe (US031, US032, US033).
 *
 * <p>Business rules enforced by this class:</p>
 * <ul>
 *   <li>All fields are mandatory.</li>
 *   <li>Password must have at least 4 characters.</li>
 *   <li>Phone number must contain exactly 9 digits.</li>
 *   <li>Staff number must not be blank.</li>
 *   <li>Security clearance expiry date must be in the future.</li>
 *   <li>Skills assessment date is automatically set to 5 years after creation.</li>
 *   <li>A new user is always created as active and non-admin.</li>
 * </ul>
 */
@Entity
@Table(name = "ALSAFE_USER")
public final class User implements AggregateRoot<EmailAddress> {

    /** Unique identifier — the user's email address. */
    @EmbeddedId
    private EmailAddress email;

    /** Full name of the user. */
    @Column(nullable = false)
    private String name;

    /** Password used for authentication. */
    @Column(nullable = false)
    private String password;

    /** Unique staff identification number. */
    @Column(nullable = false, unique = true)
    private String staffNumber;

    /** Contact phone number — must be exactly 9 digits. */
    @Column(nullable = false, unique = true)
    private String phoneNumber;

    /** Whether this account is currently active. */
    @Column(nullable = false)
    private boolean active;

    /** Whether this user holds the administrator role. */
    @Column(nullable = false)
    private boolean admin;

    /**
     * Role label for display purposes.
     * Values: "ADMIN", "BACKOFFICE_OPERATOR".
     * Roles are not yet formally enforced by the system (section 3.1.1).
     */
    @Column(nullable = false)
    private String role;

    /**
     * Date on which this user's security clearance expires.
     * Must be renewed by an Admin or Backoffice Operator (section 3.1.1).
     */
    @Column
    private LocalDate securityClearanceExpiry;

    /**
     * Date of the next mandatory skills assessment.
     * Automatically calculated as 5 years after account creation (section 3.1.1).
     * Can be updated by an Admin or Backoffice Operator.
     */
    @Column
    private LocalDate nextSkillsAssessmentDate;

    /** Protected no-arg constructor required by JPA. */
    protected User() {
    }

    /**
     * Creates a new active, non-admin backoffice user.
     *
     * @param email                   unique email address (identity)
     * @param name                    full name, must not be blank
     * @param phoneNumber             contact phone — exactly 9 digits
     * @param password                access password — at least 4 characters
     * @param staffNumber             unique staff identifier, must not be blank
     * @param securityClearanceExpiry date on which the security clearance expires
     * @throws IllegalArgumentException if any validation rule is violated
     */
    public User(final EmailAddress email, final String name, final String phoneNumber,
                final String password, final String staffNumber,
                final LocalDate securityClearanceExpiry) {
        if (email == null || name == null || password == null
                || staffNumber == null || phoneNumber == null || securityClearanceExpiry == null) {
            throw new IllegalArgumentException("All fields are required.");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank.");
        }
        if (password.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters long.");
        }
        if (staffNumber.isBlank()) {
            throw new IllegalArgumentException("Staff number must not be blank.");
        }
        if (!phoneNumber.matches("\\d{9}")) {
            throw new IllegalArgumentException("Phone number must contain exactly 9 digits.");
        }
        if (!securityClearanceExpiry.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Security clearance expiry date must be in the future.");
        }
        this.email                    = email;
        this.name                     = name;
        this.phoneNumber              = phoneNumber;
        this.password                 = password;
        this.staffNumber              = staffNumber;
        this.active                   = true;
        this.admin                    = false;
        this.role                     = "USER";
        this.securityClearanceExpiry  = securityClearanceExpiry;
        this.nextSkillsAssessmentDate = LocalDate.now().plusYears(5);
    }

    /**
     * Promotes this user to the administrator role.
     * Intended for use by the bootstrap process only.
     */
    public void makeAdmin() {
        this.admin = true;
        this.role  = "ADMIN";
    }

    /**
     * Deactivates this user account (US032).
     * A deactivated user cannot log in.
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Activates this user account (US032).
     */
    public void activate() {
        this.active = true;
    }

    /**
     * Updates the security clearance expiry date.
     * Only an Admin or Backoffice Operator may call this.
     *
     * @param date the new expiry date, must be in the future
     * @throws IllegalArgumentException if the date is not in the future
     */
    public void updateSecurityClearance(final LocalDate date) {
        if (date == null || !date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Security clearance expiry date must be in the future.");
        }
        this.securityClearanceExpiry = date;
    }

    /**
     * Updates the next skills assessment date.
     * Only an Admin or Backoffice Operator may call this.
     *
     * @param date the new assessment date
     */
    public void updateSkillsAssessment(final LocalDate date) {
        if (date == null) throw new IllegalArgumentException("Skills assessment date is required.");
        this.nextSkillsAssessmentDate = date;
    }

    /** @return {@code true} if this account is currently active */
    public boolean isActive() { return this.active; }

    /** @return {@code true} if this user is an administrator */
    public boolean isAdmin()  { return this.admin;  }

    /**
     * Checks whether the given password matches this user's password.
     *
     * @param password the password to verify
     * @return {@code true} if it matches
     */
    public boolean checkPassword(final String password) {
        return this.password != null && this.password.equals(password);
    }

    /** @return the user's full name */
    public String getName()        { return name; }

    /** @return the user's 9-digit phone number */
    public String getPhoneNumber() { return phoneNumber; }

    /** @return the user's unique staff number */
    public String getStaffNumber() { return staffNumber; }

    /** @return the user's role label (ADMIN or BACKOFFICE_OPERATOR) */
    public String getRole()        { return role; }

    /** @return the security clearance expiry date, or {@code null} if not set */
    public LocalDate getSecurityClearanceExpiry()  { return securityClearanceExpiry; }

    /** @return the next skills assessment date, or {@code null} if not set */
    public LocalDate getNextSkillsAssessmentDate() { return nextSkillsAssessmentDate; }

    /** {@inheritDoc} */
    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof User)) return false;
        return this.email.equals(((User) other).email);
    }

    /** {@inheritDoc} */
    @Override
    public EmailAddress identity() { return this.email; }

    /**
     * Returns a compact formatted string for display in tables.
     *
     * @return formatted user summary
     */
    @Override
    public String toString() {
        return String.format("%-22s | %-28s | %-22s | %-9s | %-8s | %-10s | %s",
                name,
                email.toString(),
                role,
                phoneNumber,
                active ? "ACTIVE" : "INACTIVE",
                securityClearanceExpiry  != null ? securityClearanceExpiry.toString()  : "N/A",
                nextSkillsAssessmentDate != null ? nextSkillsAssessmentDate.toString() : "N/A");
    }
}
