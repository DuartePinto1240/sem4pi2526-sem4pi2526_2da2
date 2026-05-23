package dto;

import java.time.LocalDate;

/**
 * Data Transfer Object for backoffice user data (US033).
 *
 * <p>Decouples the UI layer from the domain {@link domain.User} aggregate,
 * following the DTO pattern recommended for query use cases.</p>
 *
 * <p>Contains only the data needed for display in the user list — no
 * domain logic, no JPA annotations, no framework dependencies.</p>
 */
public final class UserDTO {

    private final String    email;
    private final String    name;
    private final String    role;
    private final String    phoneNumber;
    private final String    staffNumber;
    private final boolean   active;
    private final LocalDate securityClearanceExpiry;
    private final LocalDate nextSkillsAssessmentDate;

    /**
     * Creates a DTO from individual field values.
     *
     * @param email                   user's email address
     * @param name                    user's full name
     * @param role                    display role label
     * @param phoneNumber             contact phone number
     * @param staffNumber             staff identifier
     * @param active                  whether the account is active
     * @param securityClearanceExpiry security clearance expiry date
     * @param nextSkillsAssessmentDate next skills assessment date
     */
    public UserDTO(final String email, final String name, final String role,
                   final String phoneNumber, final String staffNumber,
                   final boolean active,
                   final LocalDate securityClearanceExpiry,
                   final LocalDate nextSkillsAssessmentDate) {
        this.email                    = email;
        this.name                     = name;
        this.role                     = role;
        this.phoneNumber              = phoneNumber;
        this.staffNumber              = staffNumber;
        this.active                   = active;
        this.securityClearanceExpiry  = securityClearanceExpiry;
        this.nextSkillsAssessmentDate = nextSkillsAssessmentDate;
    }

    public String    getEmail()                    { return email; }
    public String    getName()                     { return name; }
    public String    getRole()                     { return role; }
    public String    getPhoneNumber()              { return phoneNumber; }
    public String    getStaffNumber()              { return staffNumber; }
    public boolean   isActive()                    { return active; }
    public LocalDate getSecurityClearanceExpiry()  { return securityClearanceExpiry; }
    public LocalDate getNextSkillsAssessmentDate() { return nextSkillsAssessmentDate; }

    /**
     * Returns a formatted string for display in the user list table.
     *
     * @return formatted user summary row
     */
    @Override
    public String toString() {
        return String.format("%-22s | %-28s | %-22s | %-12s | %-9s | %-8s | %-10s | %s",
                name, email, role, staffNumber, phoneNumber,
                active ? "ACTIVE" : "INACTIVE",
                securityClearanceExpiry  != null ? securityClearanceExpiry.toString()  : "N/A",
                nextSkillsAssessmentDate != null ? nextSkillsAssessmentDate.toString() : "N/A");
    }
}
