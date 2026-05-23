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
 */
@Entity
@Table(name = "ALSAFE_USER")
public final class User implements AggregateRoot<EmailAddress> {

    @EmbeddedId
    private EmailAddress email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String staffNumber;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(nullable = false)
    private boolean active;

    /**
     * Display role label (e.g. {@code "ADMIN"}, {@code "PILOT"}).
     * Mirrors the role assigned in the EAPLI Framework's {@code SystemUser}.
     */
    @Column(nullable = false)
    private String role;

    @Column
    private LocalDate securityClearanceExpiry;

    @Column
    private LocalDate nextSkillsAssessmentDate;

    protected User() {
    }

    /**
     * Creates a new active backoffice user with domain-specific data.
     * Role defaults to {@code "BACKOFFICE_OPERATOR"} — use {@link #setRole(String)}
     * or {@link #markAsAdmin()} to change it after construction.
     *
     * @param email                   unique email — also the SystemUser username
     * @param name                    full name, must not be blank
     * @param phoneNumber             contact phone — exactly 9 digits, unique
     * @param staffNumber             unique staff identifier, must not be blank
     * @param securityClearanceExpiry clearance expiry — must be a future date
     */
    public User(final EmailAddress email, final String name, final String phoneNumber,
                final String staffNumber, final LocalDate securityClearanceExpiry) {
        // All validation is delegated to UserExceptionBuilder so exception
        // definitions stay outside this class (see UserException / UserExceptionBuilder).
        UserExceptionBuilder.of(email, name, phoneNumber, staffNumber, securityClearanceExpiry)
                .validate();

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
     * Sets the display role label to match the role assigned in the framework.
     * Called by {@link controller.RegisterUserController} after selecting a role.
     *
     * @param roleName the role name string (e.g. {@code "PILOT"}, {@code "WEATHER_PERSON"})
     */
    public void setRole(final String roleName) {
        UserExceptionBuilder.builder().checkNotNull(roleName, "Role").checkRole(roleName).validate();
        this.role = roleName;
    }

    /** Sets display role to {@code "ADMIN"}. Used by bootstrap only. */
    public void markAsAdmin() {
        this.role = "ADMIN";
    }

    /** Mirrors deactivation from the framework's SystemUser. */
    public void deactivate() {
        this.active = false;
    }

    /** Mirrors activation from the framework's SystemUser. */
    public void activate() {
        this.active = true;
    }

    public void updateSecurityClearance(final LocalDate date) {
        UserExceptionBuilder.builder()
                .checkNotNull(date, "Security clearance expiry date")
                .checkFutureDate(date, "Security clearance expiry date")
                .validate();
        this.securityClearanceExpiry = date;
    }

    public void updateSkillsAssessment(final LocalDate date) {
        UserExceptionBuilder.builder()
                .checkNotNull(date, "Skills assessment date")
                .validate();
        this.nextSkillsAssessmentDate = date;
    }

    /** Updates the phone number. Used by EditCollaboratorController. */
    public void updatePhoneNumber(final String phone) {
        UserExceptionBuilder.builder()
                .checkNotNull(phone, "Phone number")
                .checkPhone(phone)
                .validate();
        this.phoneNumber = phone;
    }

    public boolean   isActive()                    { return active; }
    public String    getRole()                     { return role; }
    public String    getName()                     { return name; }
    public String    getPhoneNumber()              { return phoneNumber; }
    public String    getStaffNumber()              { return staffNumber; }
    public LocalDate getSecurityClearanceExpiry()  { return securityClearanceExpiry; }
    public LocalDate getNextSkillsAssessmentDate() { return nextSkillsAssessmentDate; }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof User)) return false;
        return this.email.equals(((User) other).email);
    }

    @Override
    public EmailAddress identity() { return this.email; }

    @Override
    public String toString() {
        return String.format("%-22s | %-28s | %-22s | %-9s | %-8s | %-10s | %s",
                name, email.toString(), role, phoneNumber,
                active ? "ACTIVE" : "INACTIVE",
                securityClearanceExpiry  != null ? securityClearanceExpiry.toString()  : "N/A",
                nextSkillsAssessmentDate != null ? nextSkillsAssessmentDate.toString() : "N/A");
    }
}