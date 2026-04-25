package domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para a entidade User.
 * Cobre US031 (criação), US032 (ativar/desativar) e regras de negócio.
 */
class UserTest {

    private EmailAddress email(String s) {
        return EmailAddress.valueOf(s);
    }

    private User buildUser() {
        return new User(
                email("joao@alsafe.com"),
                "João Silva",
                "910000001",
                "pass1234",
                "STAFF-001"
        );
    }


    @Test
    void userIsCreatedActiveAndNotAdmin() {
        // Arrange / Act
        User u = buildUser();

        // Assert
        assertTrue(u.isActive(), "Novo utilizador deve estar ativo");
        assertFalse(u.isAdmin(), "Novo utilizador não deve ser admin");
    }

    @Test
    void userIdentityIsEmail() {
        User u = buildUser();
        assertEquals("joao@alsafe.com", u.identity().toString());
    }

    @Test
    void userCreationFailsOnNullEmail() {
        assertThrows(IllegalArgumentException.class, () ->
                new User(null, "Nome", "910000001", "pass1234", "STAFF-002"));
    }

    @Test
    void userCreationFailsOnBlankName() {
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "  ", "910000001", "pass1234", "STAFF-003"));
    }

    @Test
    void userCreationFailsOnShortPassword() {
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "Nome", "910000001", "ab", "STAFF-003"));
    }

    @Test
    void userCreationFailsOnNullStaffNumber() {
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "Nome", "910000001", "pass1234", null));
    }

    @Test
    void checkPasswordReturnsTrueOnCorrectPassword() {
        User u = buildUser();
        assertTrue(u.checkPassword("pass1234"));
    }

    @Test
    void checkPasswordReturnsFalseOnWrongPassword() {
        User u = buildUser();
        assertFalse(u.checkPassword("errada"));
    }

    @Test
    void deactivateUserMakesUserInactive() {
        User u = buildUser();
        u.deactivate();
        assertFalse(u.isActive());
    }

    @Test
    void activateUserMakesUserActive() {
        User u = buildUser();
        u.deactivate();
        u.activate();
        assertTrue(u.isActive());
    }


    @Test
    void makeAdminPromotesUser() {
        User u = buildUser();
        assertFalse(u.isAdmin());
        u.makeAdmin();
        assertTrue(u.isAdmin());
    }


    @Test
    void sameAsReturnsTrueForSameEmail() {
        User u1 = new User(email("x@alsafe.com"), "Nome A", "910000001", "pass1234", "S-001");
        User u2 = new User(email("x@alsafe.com"), "Nome B", "910000002", "pass5678", "S-002");
        assertTrue(u1.sameAs(u2));
    }

    @Test
    void sameAsReturnsFalseForDifferentEmail() {
        User u1 = buildUser();
        User u2 = new User(email("outro@alsafe.com"), "Outro", "910000002", "pass1234", "S-002");
        assertFalse(u1.sameAs(u2));
    }

    @Test
    void sameAsReturnsFalseForNonUser() {
        User u = buildUser();
        assertFalse(u.sameAs("not a user"));
    }

    @Test
    void gettersReturnCorrectValues() {
        User u = buildUser();
        assertEquals("João Silva", u.getName());
        assertEquals("910000001", u.getPhoneNumber());
        assertEquals("STAFF-001", u.getStaffNumber());
    }
}