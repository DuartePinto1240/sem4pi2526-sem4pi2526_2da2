package controller;

import domain.AlSafePasswordPolicy;
import domain.User;
import domain.ValidEmailDomains;
import dto.UserDTO;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistence.PersistenceContext;
import persistence.impl.inmemory.InMemoryUserRepository;
import repositories.UserRepository;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link ListUsersController} — US033.
 *
 * <p>Tests user listing behaviour:
 * returns all users (active and inactive), alphabetical sorting,
 * correct status/role/clearance/assessment display, and DTO mapping.</p>
 *
 * <p>Uses {@link InMemoryUserRepository} for full domain isolation.
 * Returns {@link UserDTO} objects — UI never accesses domain entities directly.</p>
 *
 * <p>Follows the AAA (Arrange – Act – Assert) convention.</p>
 */
class ListUsersControllerTest {

    private static final LocalDate FUTURE           = LocalDate.now().plusYears(2);
    private static final String    PERSISTENCE_UNIT = "eapli.base.AlSafePU";

    @BeforeAll
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void setUpFramework() {
        try {
            AuthzRegistry.configure(
                    new JpaAutoTxUserRepository(PERSISTENCE_UNIT, new HashMap<>()),
                    new AlSafePasswordPolicy(),
                    new PlainTextEncoder()
            );
        } catch (final Exception e) {
            // already configured — ignore
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        final Field instanceField = PersistenceContext.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        final PersistenceContext ctx = PersistenceContext.getInstance();
        final Field repoField = PersistenceContext.class.getDeclaredField("userRepository");
        repoField.setAccessible(true);
        repoField.set(ctx, new InMemoryUserRepository());

        final Field domainsField = ValidEmailDomains.class.getDeclaredField("DOMAINS");
        domainsField.setAccessible(true);
        ((java.util.Set<?>) domainsField.get(null)).clear();
        ValidEmailDomains.register("alsafe.com");

        AuthzRegistry.authorizationService().clearSession();
    }

    // -------------------------------------------------------------------------
    // US033.1 — List all users
    // -------------------------------------------------------------------------

    @Test
    void ensureListReturnsAllUsersIncludingInactive() {
        // Arrange
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        repo.save(new User(domain.EmailAddress.valueOf("act1@alsafe.com"),
                "Active One",   "910000001", "S-400", FUTURE));
        repo.save(new User(domain.EmailAddress.valueOf("act2@alsafe.com"),
                "Active Two",   "910000002", "S-401", FUTURE));
        final User inactive = new User(domain.EmailAddress.valueOf("inact@alsafe.com"),
                "Inactive One", "910000003", "S-402", FUTURE);
        inactive.deactivate();
        repo.save(inactive);

        // Act
        int count = 0;
        for (UserDTO ignored : new ListUsersController().allUsers()) count++;

        // Assert
        assertEquals(3, count,
                "List must include both active and inactive users");
    }

    @Test
    void ensureListIsEmptyWhenNoUsersExist() {
        // Arrange — empty repository

        // Act
        int count = 0;
        for (UserDTO ignored : new ListUsersController().allUsers()) count++;

        // Assert
        assertEquals(0, count,
                "List must be empty when no users are registered");
    }

    // -------------------------------------------------------------------------
    // US033.4 — Alphabetical sorting
    // -------------------------------------------------------------------------

    @Test
    void ensureListIsSortedAlphabeticallyByName() {
        // Arrange
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        repo.save(new User(domain.EmailAddress.valueOf("z@alsafe.com"),
                "Zara",  "910000001", "S-403", FUTURE));
        repo.save(new User(domain.EmailAddress.valueOf("a@alsafe.com"),
                "Alice", "910000002", "S-404", FUTURE));
        repo.save(new User(domain.EmailAddress.valueOf("m@alsafe.com"),
                "Maria", "910000003", "S-405", FUTURE));

        // Act
        final List<String> names = new ArrayList<>();
        for (UserDTO u : new ListUsersController().allUsers()) names.add(u.getName());

        // Assert
        assertEquals("Alice", names.get(0));
        assertEquals("Maria", names.get(1));
        assertEquals("Zara",  names.get(2));
    }

    // -------------------------------------------------------------------------
    // US033.2 / US033.3 — Status, role, clearance, assessment
    // -------------------------------------------------------------------------

    @Test
    void ensureListShowsCorrectStatusForEachUser() {
        // Arrange
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        repo.save(new User(domain.EmailAddress.valueOf("active@alsafe.com"),
                "Active User", "910000001", "S-406", FUTURE));
        final User inactiveUser = new User(
                domain.EmailAddress.valueOf("inactive@alsafe.com"),
                "Inactive User", "910000002", "S-407", FUTURE);
        inactiveUser.deactivate();
        repo.save(inactiveUser);

        // Act
        final List<UserDTO> users = new ArrayList<>();
        for (UserDTO u : new ListUsersController().allUsers()) users.add(u);

        // Assert
        assertTrue(users.stream().anyMatch(u ->
                u.getName().equals("Active User") && u.isActive()));
        assertTrue(users.stream().anyMatch(u ->
                u.getName().equals("Inactive User") && !u.isActive()));
    }

    @Test
    void ensureListShowsRoleForEachUser() {
        // Arrange
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        final User admin = new User(
                domain.EmailAddress.valueOf("adm@alsafe.com"),
                "Admin User", "910000000", "S-ADM", LocalDate.now().plusYears(10));
        admin.markAsAdmin();
        repo.save(admin);
        repo.save(new User(domain.EmailAddress.valueOf("usr@alsafe.com"),
                "Regular User", "910000001", "S-USR", FUTURE));

        // Act
        final List<UserDTO> users = new ArrayList<>();
        for (UserDTO u : new ListUsersController().allUsers()) users.add(u);

        // Assert
        assertTrue(users.stream().anyMatch(u -> u.getRole().equals("ADMIN")));
        assertTrue(users.stream().anyMatch(u -> u.getRole().equals("BACKOFFICE_OPERATOR")));
    }

    @Test
    void ensureListShowsSecurityClearanceDate() {
        // Arrange
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        final LocalDate clearance = LocalDate.now().plusYears(3);
        repo.save(new User(domain.EmailAddress.valueOf("clr@alsafe.com"),
                "Clearance User", "910000001", "S-408", clearance));

        // Act
        final List<UserDTO> users = new ArrayList<>();
        for (UserDTO u : new ListUsersController().allUsers()) users.add(u);

        // Assert
        assertTrue(users.stream().anyMatch(u ->
                clearance.equals(u.getSecurityClearanceExpiry())));
    }

    @Test
    void ensureListShowsSkillsAssessmentDate() {
        // Arrange
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        repo.save(new User(domain.EmailAddress.valueOf("ass@alsafe.com"),
                "Assessment User", "910000001", "S-409", FUTURE));

        // Act
        final List<UserDTO> users = new ArrayList<>();
        for (UserDTO u : new ListUsersController().allUsers()) users.add(u);

        // Assert
        assertTrue(users.stream().allMatch(u ->
                u.getNextSkillsAssessmentDate() != null));
    }

    // -------------------------------------------------------------------------
    // US033 — DTO pattern
    // -------------------------------------------------------------------------

    @Test
    void ensureUserDTOCorrectlyMapsAllDomainFields() {
        // Arrange
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        final LocalDate clearance = LocalDate.now().plusYears(2);
        repo.save(new User(
                domain.EmailAddress.valueOf("dto@alsafe.com"),
                "DTO User", "910000001", "S-DTO", clearance));

        // Act
        final List<UserDTO> users = new ArrayList<>();
        for (UserDTO u : new ListUsersController().allUsers()) users.add(u);
        final UserDTO dto = users.get(0);

        // Assert — every field must be correctly mapped
        assertEquals("dto@alsafe.com",      dto.getEmail());
        assertEquals("DTO User",            dto.getName());
        assertEquals("BACKOFFICE_OPERATOR", dto.getRole());
        assertEquals("910000001",           dto.getPhoneNumber());
        assertEquals("S-DTO",               dto.getStaffNumber());
        assertTrue(dto.isActive());
        assertEquals(clearance,             dto.getSecurityClearanceExpiry());
        assertNotNull(dto.getNextSkillsAssessmentDate());
    }
}
