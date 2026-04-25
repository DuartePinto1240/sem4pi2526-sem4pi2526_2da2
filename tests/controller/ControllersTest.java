package controller;

import domain.EmailAddress;
import domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistence.PersistenceContext;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração dos controllers.
 * Usa repositório in-memory via PersistenceContext.
 *
 * NOTA: O PersistenceContext é singleton, por isso cada teste reseta a instância
 * via reflexão para garantir isolamento.
 */
class ControllersTest {

    @BeforeEach
    void resetPersistenceContext() throws Exception {
        // Reseta o singleton entre testes para isolamento
        Field instanceField = PersistenceContext.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    // ----------------------------------------------------------------
    // RegisterUserController (US031)
    // ----------------------------------------------------------------

    @Test
    void registerUserSuccessfully() {
        RegisterUserController ctrl = new RegisterUserController();
        User u = ctrl.registerUser("novo@alsafe.com", "Novo User", "910000001", "pass1234", "S-100");
        assertNotNull(u);
        assertEquals("Novo User", u.getName());
        assertTrue(u.isActive());
    }

    @Test
    void registerUserFailsOnDuplicateEmail() {
        RegisterUserController ctrl = new RegisterUserController();
        ctrl.registerUser("dup@alsafe.com", "User A", "910000001", "pass1234", "S-101");

        assertThrows(IllegalArgumentException.class, () ->
                ctrl.registerUser("dup@alsafe.com", "User B", "910000002", "pass5678", "S-102"));
    }

    @Test
    void registerAdminCreatesAdminUser() {
        RegisterUserController ctrl = new RegisterUserController();
        User admin = ctrl.registerAdmin("admin@alsafe.com", "Admin", "910000000", "Admin1234", "S-ADMIN");
        assertNotNull(admin);
        assertTrue(admin.isAdmin());
    }

    @Test
    void registerAdminIsIdempotent() {
        RegisterUserController ctrl = new RegisterUserController();
        User first  = ctrl.registerAdmin("admin@alsafe.com", "Admin", "910000000", "Admin1234", "S-ADMIN");
        User second = ctrl.registerAdmin("admin@alsafe.com", "Admin", "910000000", "Admin1234", "S-ADMIN");
        // Deve devolver o mesmo (não lança exceção nem cria duplicado)
        assertEquals(first.identity(), second.identity());
    }

    // ----------------------------------------------------------------
    // LoginController (US030)
    // ----------------------------------------------------------------

    @Test
    void loginWithCorrectCredentialsReturnsUser() {
        RegisterUserController reg = new RegisterUserController();
        reg.registerUser("login@alsafe.com", "Login User", "910000001", "pass1234", "S-200");

        LoginController login = new LoginController();
        Optional<User> result = login.login("login@alsafe.com", "pass1234");

        assertTrue(result.isPresent());
        assertEquals("Login User", result.get().getName());
    }

    @Test
    void loginWithWrongPasswordReturnsEmpty() {
        RegisterUserController reg = new RegisterUserController();
        reg.registerUser("wrong@alsafe.com", "Wrong Pass", "910000001", "pass1234", "S-201");

        LoginController login = new LoginController();
        Optional<User> result = login.login("wrong@alsafe.com", "errada");

        assertTrue(result.isEmpty());
    }

    @Test
    void loginWithUnknownEmailReturnsEmpty() {
        LoginController login = new LoginController();
        Optional<User> result = login.login("naoexiste@alsafe.com", "qualquer");
        assertTrue(result.isEmpty());
    }

    @Test
    void loginWithInactiveUserReturnsEmpty() {
        RegisterUserController reg  = new RegisterUserController();
        ToggleUserController   tog  = new ToggleUserController();
        LoginController        login = new LoginController();

        reg.registerUser("inactive@alsafe.com", "Inactive", "910000001", "pass1234", "S-202");
        tog.deactivateUser("inactive@alsafe.com");

        Optional<User> result = login.login("inactive@alsafe.com", "pass1234");
        assertTrue(result.isEmpty());
    }

    // ----------------------------------------------------------------
    // ToggleUserController (US032)
    // ----------------------------------------------------------------

    @Test
    void deactivateUserMakesUserInactive() {
        RegisterUserController reg = new RegisterUserController();
        ToggleUserController   tog = new ToggleUserController();

        reg.registerUser("deact@alsafe.com", "Deactivate Me", "910000001", "pass1234", "S-300");
        tog.deactivateUser("deact@alsafe.com");

        LoginController login = new LoginController();
        assertTrue(login.login("deact@alsafe.com", "pass1234").isEmpty());
    }

    @Test
    void activateUserMakesUserActive() {
        RegisterUserController reg = new RegisterUserController();
        ToggleUserController   tog = new ToggleUserController();

        reg.registerUser("react@alsafe.com", "Reactivate Me", "910000001", "pass1234", "S-301");
        tog.deactivateUser("react@alsafe.com");
        tog.activateUser("react@alsafe.com");

        LoginController login = new LoginController();
        assertTrue(login.login("react@alsafe.com", "pass1234").isPresent());
    }

    @Test
    void deactivateNonExistentUserThrowsException() {
        ToggleUserController tog = new ToggleUserController();
        assertThrows(IllegalArgumentException.class, () ->
                tog.deactivateUser("naoexiste@alsafe.com"));
    }

    @Test
    void deactivateAlreadyInactiveUserThrowsException() {
        RegisterUserController reg = new RegisterUserController();
        ToggleUserController   tog = new ToggleUserController();

        reg.registerUser("already@alsafe.com", "Already Inactive", "910000001", "pass1234", "S-302");
        tog.deactivateUser("already@alsafe.com");

        assertThrows(IllegalArgumentException.class, () ->
                tog.deactivateUser("already@alsafe.com"));
    }

    @Test
    void deactivateAdminThrowsException() {
        RegisterUserController reg = new RegisterUserController();
        ToggleUserController   tog = new ToggleUserController();

        reg.registerAdmin("admin2@alsafe.com", "Admin", "910000000", "Admin1234", "S-ADMIN2");

        assertThrows(IllegalArgumentException.class, () ->
                tog.deactivateUser("admin2@alsafe.com"));
    }

    @Test
    void activateAlreadyActiveUserThrowsException() {
        RegisterUserController reg = new RegisterUserController();
        ToggleUserController   tog = new ToggleUserController();

        reg.registerUser("actv@alsafe.com", "Active", "910000001", "pass1234", "S-303");

        assertThrows(IllegalArgumentException.class, () ->
                tog.activateUser("actv@alsafe.com"));
    }

    // ----------------------------------------------------------------
    // ListUsersController (US033)
    // ----------------------------------------------------------------

    @Test
    void listUsersReturnsAllRegisteredUsers() {
        RegisterUserController reg  = new RegisterUserController();
        ListUsersController    list = new ListUsersController();

        reg.registerUser("list1@alsafe.com", "User 1", "910000001", "pass1234", "S-400");
        reg.registerUser("list2@alsafe.com", "User 2", "910000002", "pass1234", "S-401");

        int count = 0;
        for (User u : list.allUsers()) count++;

        assertEquals(2, count);
    }

    @Test
    void listUsersReturnsEmptyWhenNoUsersExist() {
        ListUsersController list = new ListUsersController();
        int count = 0;
        for (User u : list.allUsers()) count++;
        assertEquals(0, count);
    }
}
