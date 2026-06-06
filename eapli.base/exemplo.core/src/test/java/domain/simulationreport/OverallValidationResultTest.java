// ─── domain/simulationreport/OverallValidationResultTest.java ─────────────

package domain.simulationreport;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;

class OverallValidationResultTest {

    @Test
    void ensurePassIsPassed() {
        assertTrue(OverallValidationResult.pass().isPassed());
    }

    @Test
    void ensureFailIsNotPassed() {
        assertFalse(OverallValidationResult.fail().isPassed());
    }

    @Test
    void ensureTwoPassesAreEqual() {
        assertEquals(OverallValidationResult.pass(),
                OverallValidationResult.pass());
    }

    @Test
    void ensureTwoFailsAreEqual() {
        assertEquals(OverallValidationResult.fail(),
                OverallValidationResult.fail());
    }

    @Test
    void ensurePassAndFailAreNotEqual() {
        assertNotEquals(OverallValidationResult.pass(),
                OverallValidationResult.fail());
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final OverallValidationResult r = OverallValidationResult.pass();
        assertEquals(r, r);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        assertNotEquals(null, OverallValidationResult.pass());
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        assertNotEquals(OverallValidationResult.pass(), "PASS");
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        assertEquals(OverallValidationResult.pass().hashCode(),
                OverallValidationResult.pass().hashCode());
    }

    @Test
    void ensureToStringReturnsPass() {
        assertEquals("PASS", OverallValidationResult.pass().toString());
    }

    @Test
    void ensureToStringReturnsFail() {
        assertEquals("FAIL", OverallValidationResult.fail().toString());
    }

    @Test
    void ensureProtectedNoArgConstructorExistsForJpa() throws Exception {
        final Constructor<OverallValidationResult> ctor =
                OverallValidationResult.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());
    }
}