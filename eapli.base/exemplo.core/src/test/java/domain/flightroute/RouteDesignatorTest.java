package domain.flightroute;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;

class RouteDesignatorTest {

    @Test
    void ensureValidDesignatorIsCreated() {
        final RouteDesignator rd = new RouteDesignator("TP123");
        assertEquals("TP123", rd.value());
    }

    @Test
    void ensureLowercaseIsConvertedToUppercase() {
        final RouteDesignator rd = new RouteDesignator("tp1");
        assertEquals("TP1", rd.value());
    }

    @Test
    void ensureMinimumOneDigitIsAccepted() {
        assertDoesNotThrow(() -> new RouteDesignator("TP1"));
    }

    @Test
    void ensureMaximumFourDigitsIsAccepted() {
        assertDoesNotThrow(() -> new RouteDesignator("TP9999"));
    }

    @Test
    void ensureFiveDigitsIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new RouteDesignator("TP12345"));
    }

    @Test
    void ensureOneLetterIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new RouteDesignator("T123"));
    }

    @Test
    void ensureThreeLettersIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new RouteDesignator("TAP123"));
    }

    @Test
    void ensureNoDigitsIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new RouteDesignator("TP"));
    }

    @Test
    void ensureEmptyIsRejected() {
        assertThrows(Exception.class,
                () -> new RouteDesignator(""));
    }

    @Test
    void ensureNullIsRejected() {
        assertThrows(Exception.class,
                () -> new RouteDesignator(null));
    }

    @Test
    void ensureEqualsReturnsTrueForSameValue() {
        assertEquals(new RouteDesignator("TP123"), new RouteDesignator("TP123"));
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentValue() {
        assertNotEquals(new RouteDesignator("TP123"), new RouteDesignator("FR456"));
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final RouteDesignator rd = new RouteDesignator("TP123");
        assertEquals(rd, rd);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        assertNotEquals(null, new RouteDesignator("TP123"));
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        assertNotEquals(new RouteDesignator("TP123"), "TP123");
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        assertEquals(
                new RouteDesignator("TP123").hashCode(),
                new RouteDesignator("TP123").hashCode());
    }

    @Test
    void ensureCompareToReturnsZeroForEqual() {
        assertEquals(0,
                new RouteDesignator("TP123").compareTo(new RouteDesignator("TP123")));
    }

    @Test
    void ensureCompareToReturnsNegativeForSmaller() {
        assertTrue(
                new RouteDesignator("FR001").compareTo(new RouteDesignator("TP123")) < 0);
    }

    @Test
    void ensureCompareToReturnsPositiveForLarger() {
        assertTrue(
                new RouteDesignator("TP123").compareTo(new RouteDesignator("FR001")) > 0);
    }

    @Test
    void ensureToStringReturnsValue() {
        assertEquals("TP123", new RouteDesignator("TP123").toString());
    }

    @Test
    void ensureProtectedNoArgConstructorExistsForJpa() throws Exception {
        final Constructor<RouteDesignator> ctor =
                RouteDesignator.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final RouteDesignator instance = ctor.newInstance();
        assertNotNull(instance);
    }
}