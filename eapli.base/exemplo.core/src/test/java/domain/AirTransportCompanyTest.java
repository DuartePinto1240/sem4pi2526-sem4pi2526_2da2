package domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para o Aggregate Root AirTransportCompany.
 * Cobre US060 - Registar companhia de transporte aéreo.
 */
class AirTransportCompanyTest {

    private AirTransportCompany buildCompany() {
        return new AirTransportCompany("TAP Air Portugal", "TP", "TAP");
    }

    // ----------------------------------------------------------------
    // Criação válida
    // ----------------------------------------------------------------

    @Test
    void validCompanyIsCreated() {
        AirTransportCompany company = buildCompany();
        assertNotNull(company);
        assertEquals("TAP Air Portugal", company.name());
        assertEquals("TP", company.identity().iata());
        assertEquals("TAP", company.identity().icao());
    }



    @Test
    void lowercaseCodesAreAcceptedAndConverted() {
        AirTransportCompany company =
                new AirTransportCompany("TAP Air Portugal", "tp", "tap");
        assertEquals("TP", company.identity().iata());
        assertEquals("TAP", company.identity().icao());
    }

    // ----------------------------------------------------------------
    // Nome inválido
    // ----------------------------------------------------------------

    @Test
    void emptyNameIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirTransportCompany("", "TP", "TAP"));
    }

    @Test
    void nullNameIsRejected() {
        assertThrows(Exception.class,
                () -> new AirTransportCompany(null, "TP", "TAP"));
    }

    @Test
    void blankNameIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirTransportCompany("   ", "TP", "TAP"));
    }

    // ----------------------------------------------------------------
    // IATA inválido
    // ----------------------------------------------------------------

    @Test
    void iataWithOneLetterIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirTransportCompany("TAP", "T", "TAP"));
    }

    @Test
    void iataWithThreeLettersIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirTransportCompany("TAP", "TAP", "TAP"));
    }

    @Test
    void emptyIataIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirTransportCompany("TAP", "", "TAP"));
    }

    // ----------------------------------------------------------------
    // ICAO inválido
    // ----------------------------------------------------------------

    @Test
    void icaoWithOneLetterIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirTransportCompany("TAP", "TP", "T"));
    }

    @Test
    void icaoWithFourLettersIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirTransportCompany("TAP", "TP", "TAPP"));
    }

    @Test
    void emptyIcaoIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirTransportCompany("TAP", "TP", ""));
    }

    // ----------------------------------------------------------------
    // Identidade e igualdade
    // ----------------------------------------------------------------

    @Test
    void identityIsCompanyCode() {
        AirTransportCompany company = buildCompany();
        CompanyCode code = company.identity();
        assertEquals("TP", code.iata());
        assertEquals("TAP", code.icao());
    }

    @Test
    void sameAsReturnsTrueForSameCodes() {
        AirTransportCompany c1 = new AirTransportCompany("TAP Air Portugal", "TP", "TAP");
        AirTransportCompany c2 = new AirTransportCompany("TAP", "TP", "TAP");
        assertTrue(c1.sameAs(c2));
    }

    @Test
    void sameAsReturnsFalseForDifferentCodes() {
        AirTransportCompany c1 = new AirTransportCompany("TAP", "TP", "TAP");
        AirTransportCompany c2 = new AirTransportCompany("Ryanair", "FR", "RYR");
        assertFalse(c1.sameAs(c2));
    }

    @Test
    void sameAsReturnsFalseForNonCompany() {
        AirTransportCompany company = buildCompany();
        assertFalse(company.sameAs("not a company"));
    }

    @Test
    void toStringContainsNameAndCodes() {
        AirTransportCompany company = buildCompany();
        String result = company.toString();
        assertTrue(result.contains("TAP Air Portugal"));
        assertTrue(result.contains("TP"));
        assertTrue(result.contains("TAP"));
    }
}