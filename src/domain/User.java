package domain;

import eapli.framework.domain.model.AggregateRoot;
import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Aggregate root representing a backoffice user of AlSafe (US031, US032, US033).
 *
 * <p>This entity holds <em>domain-specific</em> data for each user.
 * Authentication credentials (password, active status, roles) are managed
 * by the EAPLI Framework's {@code SystemUser} entity via {@code AuthzRegistry}.
 * The link between this entity and {@code SystemUser} is the email address,
 * which is used as the {@code SystemUser} username.</p>
 *
 * <p>Domain invariants enforced by this class:</p>
 * <ul>
 *   <li>Name must not be blank.</li>
 *   <li>Phone number must contain exactly 9 digits (unique).</li>
 *   <li>Staff number must not be blank (unique).</li>
 *   <li>Security clearance expiry date must be in the future.</li>
 *   <li>Skills assessment date is auto-calculated as today + 5 years (section 3.1.1).</li>
 * </ul>
 */
@Entity
@Table(name = "ALSAFE_USER")
public final class User implements AggregateRoot<EmailAddress> {

    /** Unique identifier — also the SystemUser username in the framework. */
    @EmbeddedId
    private EmailAddress email;

    /** Full name of the user. */
    @Column(nullable = false)
    private String name;

    /** Unique staff identification number. */
    @Column(nullable = false, unique = true)
    private String staffNumber;

    /** Contact phone number — must be exactly 9 digits and unique. */
    @Column(nullable = false, unique = true)
    private String phoneNumber;

    /**
     * Local mirror of the active status from the framework's SystemUser.
     * Kept in sync by {@link controller.ToggleUserController}.
     */
    @Column(nullable = false)
    private boolean active;

    /**
     * Display role label (e.g. {@code "ADMIN"}, {@code "BACKOFFICE_OPERATOR"}).
     * The actual role enforcement is done by the EAPLI Framework via BaseRoles.
     */
    @Column(nullable = false)
    private String role;

    /**
     * Date on which this user's security clearance expires (section 3.1.1).
     * Must be renewed by an Admin or Backoffice Operator.
     */
    @Column
    private LocalDate securityClearanceExpiry;

    /**
     * Date of the next mandatory skills assessment (section 3.1.1).
     * Auto-calculated as today + 5 years on creation.
     */
    @Column
    private LocalDate nextSkillsAssessmentDate;

    /** No-arg constructor required by JPA. */
    protected User() {
    }

    /**
     * Creates a new active backoffice user with domain-specific data.
     *
     * @param email                   unique email — also the SystemUser username
     * @param name                    full name, must not be blank
     * @param phoneNumber             contact phone — exactly 9 digits, unique
     * @param staffNumber             unique staff identifier, must not be blank
     * @param securityClearanceExpiry clearance expiry — must be a future date
     * @throws IllegalArgumentException if any validation rule is violated
     */
    public User(final EmailAddress email, final String name, final String phoneNumber,
                final String staffNumber, final LocalDate securityClearanceExpiry) {
        if (email == null || name == null || staffNumber == null
                || phoneNumber == null || securityClearanceExpiry == null) {
            throw new IllegalArgumentException("All fields are required.");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank.");
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
        this.staffNumber              = staffNumber;
        this.active                   = true;
        this.role                     = "BACKOFFICE_OPERATOR";
        this.securityClearanceExpiry  = securityClearanceExpiry;
        this.nextSkillsAssessmentDate = LocalDate.now().plusYears(5);
    }

    /**
     * Sets this user's display role to {@code "ADMIN"}.
     * Called during bootstrap for the default administrator account.
     */
    public void markAsAdmin() {
        this.role = "ADMIN";
    }

    /**
     * Mirrors the deactivation from the framework's SystemUser.
     * Called by {@link controller.ToggleUserController} after deactivating SystemUser.
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Mirrors the activation from the framework's SystemUser.
     * Called by {@link controller.ToggleUserController} after activating SystemUser.
     */
    public void activate() {
        this.active = true;
    }

    /**
     * Updates the security clearance expiry date.
     * Only an Admin or Backoffice Operator may call this.
     *
     * @param date the new expiry date, must be in the future
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
     * @param date the new assessment date, must not be null
     */
    public void updateSkillsAssessment(final LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Skills assessment date is required.");
        }
        this.nextSkillsAssessmentDate = date;
    }

    /** @return {@code true} if this account is currently active (mirrored from SystemUser) */
    public boolean isActive() { return this.active; }

    /** @return display role label */
    public String getRole() { return role; }

    /** @return the user's full name */
    public String getName() { return name; }

    /** @return the user's 9-digit phone number */
    public String getPhoneNumber() { return phoneNumber; }

    /** @return the user's unique staff number */
    public String getStaffNumber() { return staffNumber; }

    /** @return the security clearance expiry date */
    public LocalDate getSecurityClearanceExpiry() { return securityClearanceExpiry; }

    /** @return the next skills assessment date */
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
     * Returns a formatted string with all user fields and current status.
     *
     * @return formatted user summary
     */
    @Override
    public String toString() {
        return String.format("%-22s | %-28s | %-22s | %-9s | %-8s | %-10s | %s",
                name, email.toString(), role, phoneNumber,
                active ? "ACTIVE" : "INACTIVE",
                securityClearanceExpiry  != null ? securityClearanceExpiry.toString()  : "N/A",
                nextSkillsAssessmentDate != null ? nextSkillsAssessmentDate.toString() : "N/A");
    }
}
