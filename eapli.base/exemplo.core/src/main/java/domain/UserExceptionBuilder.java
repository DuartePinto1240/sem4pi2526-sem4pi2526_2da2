package domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder that accumulates {@link User} validation errors and throws an
 * {@link IllegalArgumentException} if any are found.
 *
 * <p>The purpose of this class is to keep all validation/exception logic
 * <em>outside</em> the {@link User} class — {@code User} simply calls
 * {@link #validate()} and stays free of inline guard clauses.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * UserExceptionBuilder.of(email, name, phoneNumber, staffNumber, securityClearanceExpiry)
 *         .validate();
 * }</pre>
 *
 * <p>Each {@code check*()} method adds a violation message to an internal list.
 * {@link #validate()} throws an {@link IllegalArgumentException} whose message
 * lists every violation found, separated by {@code " | "}, or returns silently
 * if there are none.</p>
 */
public final class UserExceptionBuilder {

    private final List<String> violations = new ArrayList<>();

    private UserExceptionBuilder() { }

    /** Entry point — returns a builder ready to receive validation checks. */
    public static UserExceptionBuilder builder() {
        return new UserExceptionBuilder();
    }

    // -----------------------------------------------------------------------
    // Individual checks — each returns {@code this} for chaining
    // -----------------------------------------------------------------------

    public UserExceptionBuilder checkNotNull(final Object value, final String fieldName) {
        if (value == null) {
            violations.add(fieldName + " is required.");
        }
        return this;
    }

    public UserExceptionBuilder checkNotBlank(final String value, final String fieldName) {
        if (value != null && value.isBlank()) {
            violations.add(fieldName + " must not be blank.");
        }
        return this;
    }

    public UserExceptionBuilder checkPhone(final String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.matches("\\d{9}")) {
            violations.add("Phone number must contain exactly 9 digits (got: \"" + phoneNumber + "\").");
        }
        return this;
    }

    public UserExceptionBuilder checkFutureDate(final LocalDate date, final String fieldName) {
        if (date != null && !date.isAfter(LocalDate.now())) {
            violations.add(fieldName + " must be a future date (got: " + date + ").");
        }
        return this;
    }

    public UserExceptionBuilder checkRole(final String roleName) {
        if (roleName != null && roleName.isBlank()) {
            violations.add("Role must not be blank.");
        }
        return this;
    }

    // -----------------------------------------------------------------------
    // Convenience: run all standard User-construction checks at once
    // -----------------------------------------------------------------------

    /**
     * Runs every validation rule required to construct a {@link User}.
     *
     * @param email                   email address value object (already validated at domain level)
     * @param name                    full name
     * @param phoneNumber             contact phone
     * @param staffNumber             unique staff identifier
     * @param securityClearanceExpiry future expiry date
     * @return this builder (for chaining or direct {@link #validate()} call)
     */
    public static UserExceptionBuilder of(final EmailAddress email,
                                          final String name,
                                          final String phoneNumber,
                                          final String staffNumber,
                                          final LocalDate securityClearanceExpiry) {
        return builder()
                .checkNotNull(email,                   "Email")
                .checkNotNull(name,                    "Name")
                .checkNotNull(phoneNumber,             "Phone number")
                .checkNotNull(staffNumber,             "Staff number")
                .checkNotNull(securityClearanceExpiry, "Security clearance expiry date")
                .checkNotBlank(name,                   "Name")
                .checkNotBlank(staffNumber,            "Staff number")
                .checkPhone(phoneNumber)
                .checkFutureDate(securityClearanceExpiry, "Security clearance expiry date");
    }

    // -----------------------------------------------------------------------
    // Terminal operation
    // -----------------------------------------------------------------------

    /**
     * Throws an {@link IllegalArgumentException} listing all accumulated
     * violations, or returns silently if there are none.
     *
     * @throws IllegalArgumentException if one or more validation rules were violated
     */
    public void validate() {
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(String.join(" | ", violations));
        }
    }
}
