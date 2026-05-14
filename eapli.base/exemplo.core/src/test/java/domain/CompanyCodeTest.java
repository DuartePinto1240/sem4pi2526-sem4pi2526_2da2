package domain;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para o Value Object CompanyCode.
 * Cobre as regras de formato IATA (2 letras) e ICAO (2-3 letras).
 */
class CompanyCodeTest {

    // ----------------------------------------------------------------
    // Criação válida
    // ----------------------------------------------------------------

    @Test
    void validCompanyCodeIsCreated() {
        CompanyCode code = new CompanyCode("TP", "TAP");
        assertEquals("TP", code.iata());
        assertEquals("TAP", code.icao());
    }

    @Test
    void validIcaoWithTwoLettersIsAccepted() {
        assertDoesNotThrow(() -> new CompanyCode("TP", "TA"));
    }

    @Test
    void validIcaoWithThreeLettersIsAccepted() {
        assertDoesNotThrow(() -> new CompanyCode("TP", "TAP"));
    }

    @Test
    void lowercaseInputIsConvertedToUppercase() {
        CompanyCode code = new CompanyCode("tp", "tap");
        assertEquals("TP", code.iata());
        assertEquals("TAP", code.icao());
    }

    @Test
    void mixedCaseInputIsConvertedToUppercase() {
        CompanyCode code = new CompanyCode("Tp", "TaP");
        assertEquals("TP", code.iata());
        assertEquals("TAP", code.icao());
    }

    // ----------------------------------------------------------------
    // IATA inválido
    // ----------------------------------------------------------------

    @Test
    void iataWithOneLetterIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CompanyCode("T", "TAP"));
    }

    @Test
    void iataWithThreeLettersIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CompanyCode("TAP", "TAP"));
    }

    @Test
    void iataWithDigitsIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CompanyCode("T1", "TAP"));
    }

    @Test
    void emptyIataIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CompanyCode("", "TAP"));
    }

    @Test
    void nullIataIsRejected() {
        assertThrows(Exception.class,
                () -> new CompanyCode(null, "TAP"));
    }

    // ----------------------------------------------------------------
    // ICAO inválido
    // ----------------------------------------------------------------

    @Test
    void icaoWithOneLetterIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CompanyCode("TP", "T"));
    }

    @Test
    void icaoWithFourLettersIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CompanyCode("TP", "TAPP"));
    }

    @Test
    void icaoWithDigitsIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CompanyCode("TP", "T1P"));
    }

    @Test
    void emptyIcaoIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CompanyCode("TP", ""));
    }

    @Test
    void nullIcaoIsRejected() {
        assertThrows(Exception.class,
                () -> new CompanyCode("TP", null));
    }

    // ----------------------------------------------------------------
    // Igualdade e comparação
    // ----------------------------------------------------------------

    @Test
    void twoCodesWithSameValuesAreEqual() {
        CompanyCode c1 = new CompanyCode("TP", "TAP");
        CompanyCode c2 = new CompanyCode("TP", "TAP");
        assertEquals(c1, c2);
    }

    @Test
    void twoCodesWithDifferentIataAreNotEqual() {
        CompanyCode c1 = new CompanyCode("TP", "TAP");
        CompanyCode c2 = new CompanyCode("FR", "TAP");
        assertNotEquals(c1, c2);
    }

    @Test
    void twoCodesWithDifferentIcaoAreNotEqual() {
        CompanyCode c1 = new CompanyCode("TP", "TAP");
        CompanyCode c2 = new CompanyCode("TP", "RYR");
        assertNotEquals(c1, c2);
    }

    @Test
    void hashCodeIsConsistentWithEquals() {
        CompanyCode c1 = new CompanyCode("TP", "TAP");
        CompanyCode c2 = new CompanyCode("TP", "TAP");
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void compareToReturnsZeroForEqualCodes() {
        CompanyCode c1 = new CompanyCode("TP", "TAP");
        CompanyCode c2 = new CompanyCode("TP", "TAP");
        assertEquals(0, c1.compareTo(c2));
    }

    @Test
    void compareToReturnsNegativeForLexicographicallySmaller() {
        CompanyCode c1 = new CompanyCode("AF", "AFR");
        CompanyCode c2 = new CompanyCode("TP", "TAP");
        assertTrue(c1.compareTo(c2) < 0);
    }

    @Test
    void compareToReturnsPositiveForLexicographicallyLarger() {
        CompanyCode c1 = new CompanyCode("TP", "TAP");
        CompanyCode c2 = new CompanyCode("AF", "AFR");
        assertTrue(c1.compareTo(c2) > 0);
    }

    @Test
    void toStringContainsIataAndIcao() {
        CompanyCode code = new CompanyCode("TP", "TAP");
        String result = code.toString();
        assertTrue(result.contains("TP"));
        assertTrue(result.contains("TAP"));
    }
    @Test
    void iataGetterReturnsCorrectValue() {

        CompanyCode code = new CompanyCode("TP", "TAP");

        assertEquals("TP", code.iata());
    }

    @Test
    void icaoGetterReturnsCorrectValue() {

        CompanyCode code = new CompanyCode("TP", "TAP");

        assertEquals("TAP", code.icao());
    }

    @Test
    void equalsReturnsTrueForSameInstance() {

        CompanyCode code = new CompanyCode("TP", "TAP");

        assertEquals(code, code);
    }

    @Test
    void equalsReturnsFalseForNull() {

        CompanyCode code = new CompanyCode("TP", "TAP");

        assertNotEquals(null, code);
    }

    @Test
    void equalsReturnsFalseForDifferentType() {

        CompanyCode code = new CompanyCode("TP", "TAP");

        assertNotEquals(code, "not a code");
    }

    @Test
    void compareToReturnsNegativeWhenIcaoIsSmaller() {

        CompanyCode c1 = new CompanyCode("TP", "AAA");
        CompanyCode c2 = new CompanyCode("TP", "BBB");

        assertTrue(c1.compareTo(c2) < 0);
    }

    @Test
    void compareToReturnsPositiveWhenIcaoIsLarger() {

        CompanyCode c1 = new CompanyCode("TP", "ZZZ");
        CompanyCode c2 = new CompanyCode("TP", "AAA");

        assertTrue(c1.compareTo(c2) > 0);
    }

    @Test
    void toStringIsNotBlank() {

        CompanyCode code = new CompanyCode("TP", "TAP");

        assertFalse(code.toString().isBlank());
    }

    @Test
    void iataWithSpecialCharactersIsRejected() {

        assertThrows(IllegalArgumentException.class,
                () -> new CompanyCode("T@", "TAP"));
    }

    @Test
    void icaoWithSpecialCharactersIsRejected() {

        assertThrows(IllegalArgumentException.class,
                () -> new CompanyCode("TP", "TA@"));
    }

    @Test
    void iataWithSpacesIsRejected() {

        assertThrows(IllegalArgumentException.class,
                () -> new CompanyCode(" T", "TAP"));
    }

    @Test
    void icaoWithSpacesIsRejected() {

        assertThrows(IllegalArgumentException.class,
                () -> new CompanyCode("TP", " TA"));
    }

    @Test
    void compareToWithEqualIataUsesIcao() {

        CompanyCode c1 = new CompanyCode("TP", "AAA");
        CompanyCode c2 = new CompanyCode("TP", "BBB");

        assertNotEquals(0, c1.compareTo(c2));
    }

    @Test
    void hashCodeIsStable() {

        CompanyCode code = new CompanyCode("TP", "TAP");

        int first = code.hashCode();
        int second = code.hashCode();

        assertEquals(first, second);
    }

    @Test
    void ensureProtectedNoArgConstructorExistsForJpa() throws Exception {
        final Constructor<CompanyCode> ctor =
                CompanyCode.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final CompanyCode instance = ctor.newInstance();
        assertNotNull(instance);
    }
}
