package domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para o Value Object EmailAddress.
 */
class EmailAddressTest {

    @Test
    void validEmailCreatesObject() {
        EmailAddress e = EmailAddress.valueOf("user@alsafe.com");
        assertEquals("user@alsafe.com", e.toString());
    }

    @Test
    void emailWithSubdomainIsValid() {
        assertDoesNotThrow(() -> EmailAddress.valueOf("user@mail.alsafe.com"));
    }

    @Test
    void emptyEmailThrowsException() {
        assertThrows(Exception.class, () -> EmailAddress.valueOf(""));
    }

    @Test
    void nullEmailThrowsException() {
        assertThrows(Exception.class, () -> EmailAddress.valueOf(null));
    }

    @Test
    void emailWithoutAtThrowsException() {
        assertThrows(Exception.class, () -> EmailAddress.valueOf("invalido"));
    }

    @Test
    void emailWithoutDomainThrowsException() {
        assertThrows(Exception.class, () -> EmailAddress.valueOf("invalido@"));
    }

    @Test
    void twoEmailsWithSameValueAreEqual() {
        EmailAddress e1 = EmailAddress.valueOf("a@b.com");
        EmailAddress e2 = EmailAddress.valueOf("a@b.com");
        assertEquals(e1, e2);
    }

    @Test
    void twoEmailsWithDifferentValuesAreNotEqual() {
        EmailAddress e1 = EmailAddress.valueOf("a@b.com");
        EmailAddress e2 = EmailAddress.valueOf("c@b.com");
        assertNotEquals(e1, e2);
    }

    @Test
    void compareToReturnsNegativeForLexicographicallySmaller() {
        EmailAddress e1 = EmailAddress.valueOf("a@b.com");
        EmailAddress e2 = EmailAddress.valueOf("z@b.com");
        assertTrue(e1.compareTo(e2) < 0);
    }

    @Test
    void hashCodeIsConsistentWithEquals() {
        EmailAddress e1 = EmailAddress.valueOf("x@y.com");
        EmailAddress e2 = EmailAddress.valueOf("x@y.com");
        assertEquals(e1.hashCode(), e2.hashCode());
    }
}