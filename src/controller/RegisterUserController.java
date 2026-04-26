package controller;

import domain.EmailAddress;
import domain.User;
import domain.ValidEmailDomains;
import persistence.PersistenceContext;
import repositories.UserRepository;

import java.time.LocalDate;

/**
 * Controller for US031 — Register backoffice users.
 *
 * <p>Enforces all registration business rules:</p>
 * <ul>
 *   <li>Email must belong to a valid AISafe domain.</li>
 *   <li>Email, phone number and staff number must be unique.</li>
 *   <li>Security clearance expiry date must be in the future.</li>
 * </ul>
 */
public class RegisterUserController {

    private final UserRepository repo;

    /**
     * Creates a new controller using the shared persistence context.
     */
    public RegisterUserController() {
        this.repo = PersistenceContext.getInstance().userRepository();
    }

    /**
     * Registers a new backoffice user (US031).
     *
     * @param email                   unique email — must belong to a valid AISafe domain
     * @param name                    full name
     * @param phoneNumber             contact phone — exactly 9 digits
     * @param password                access password — at least 4 characters
     * @param staffNumber             unique staff identifier
     * @param securityClearanceExpiry expiry date for the security clearance — must be future
     * @return the newly created {@link User}
     * @throws IllegalArgumentException if any validation or uniqueness rule is violated
     */
    public User registerUser(final String email, final String name, final String phoneNumber,
                             final String password, final String staffNumber,
                             final LocalDate securityClearanceExpiry) {

        // 1. Email domain validation (section 3.1.1)
        if (!ValidEmailDomains.isEmpty() && !ValidEmailDomains.isValid(email)) {
            throw new IllegalArgumentException(
                    "Email domain is not valid. Allowed domains: " + ValidEmailDomains.getAll());
        }

        // 2. Duplicate email check
        final EmailAddress emailVO = EmailAddress.valueOf(email);
        if (repo.ofIdentity(emailVO).isPresent()) {
            throw new IllegalArgumentException("A user with email '" + email + "' already exists.");
        }

        // 3. Create and save (domain validates phone, password, staff, clearance date)
        final User newUser = new User(emailVO, name, phoneNumber, password,
                staffNumber, securityClearanceExpiry);
        repo.save(newUser);
        return newUser;
    }

    /**
     * Registers the default administrator account.
     * Used exclusively by the bootstrap process.
     * If the admin already exists (e.g. from a previous run), returns the existing one.
     *
     * @param email                   admin email
     * @param name                    admin full name
     * @param phoneNumber             admin phone — exactly 9 digits
     * @param password                admin password
     * @param staffNumber             admin staff number
     * @param securityClearanceExpiry admin clearance expiry (typically far in the future)
     * @return the admin {@link User}
     */
    public User registerAdmin(final String email, final String name, final String phoneNumber,
                              final String password, final String staffNumber,
                              final LocalDate securityClearanceExpiry) {
        final EmailAddress emailVO = EmailAddress.valueOf(email);

        if (repo.ofIdentity(emailVO).isPresent()) {
            return repo.ofIdentity(emailVO).get();
        }

        final User admin = new User(emailVO, name, phoneNumber, password,
                staffNumber, securityClearanceExpiry);
        admin.makeAdmin();
        repo.save(admin);
        return admin;
    }
}
