package controller;

import domain.AirTransportCompany;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistence.PersistenceContext;
import persistence.impl.inmemory.InMemoryAirTransportCompanyRepository;
import repositories.AirTransportCompanyRepository;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class RegisterAirTransportCompanyControllerTest {

    private RegisterAirTransportCompanyController ctrl;

    @BeforeEach
    void setUp() throws Exception {
        // Reset PersistenceContext singleton
        final Field instanceField = PersistenceContext.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        // Inject fresh InMemoryAirTransportCompanyRepository
        final PersistenceContext ctx = PersistenceContext.getInstance();
        final Field repoField = PersistenceContext.class
                .getDeclaredField("airTransportCompanyRepository");
        repoField.setAccessible(true);
        repoField.set(ctx, new InMemoryAirTransportCompanyRepository());

        ctrl = new RegisterAirTransportCompanyController();
    }

    // ─────────────────────────────────────────
    // Valid registration
    // ─────────────────────────────────────────

    @Test
    void registerValidCompanyReturnsCompany() {
        final AirTransportCompany company =
                ctrl.registerCompany("TAP Air Portugal", "TP", "TAP");

        assertNotNull(company);
        assertEquals("TAP Air Portugal", company.name());
        assertEquals("TP", company.companyCode().iata());
        assertEquals("TAP", company.companyCode().icao());
    }

    @Test
    void registerMultipleCompaniesWithDifferentCodesSucceeds() {
        ctrl.registerCompany("TAP Air Portugal", "TP", "TAP");
        final AirTransportCompany second =
                ctrl.registerCompany("Ryanair", "FR", "RYR");

        assertNotNull(second);
        assertEquals("Ryanair", second.name());
    }

    // ─────────────────────────────────────────
    // Duplicate IATA
    // ─────────────────────────────────────────



    @Test
    void duplicateIataExceptionMessageContainsCode() {
        ctrl.registerCompany("TAP Air Portugal", "TP", "TAP");

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () ->
                        ctrl.registerCompany("TAP Express", "TP", "TPX"));

        assertTrue(ex.getMessage().contains("TP"));
    }

    // ─────────────────────────────────────────
    // Duplicate ICAO
    // ─────────────────────────────────────────

    @Test
    void registerCompanyWithDuplicateIcaoThrowsException() {
        ctrl.registerCompany("TAP Air Portugal", "TP", "TAP");

        assertThrows(IllegalArgumentException.class, () ->
                ctrl.registerCompany("TAP Express", "TX", "TAP"));
    }

    @Test
    void duplicateIcaoExceptionMessageContainsCode() {
        ctrl.registerCompany("TAP Air Portugal", "TP", "TAP");

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () ->
                        ctrl.registerCompany("TAP Express", "TX", "TAP"));

        assertTrue(ex.getMessage().contains("TAP"));
    }

    // ─────────────────────────────────────────
    // Invalid name
    // ─────────────────────────────────────────

    @Test
    void registerCompanyWithEmptyNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.registerCompany("", "TP", "TAP"));
    }

    @Test
    void registerCompanyWithNullNameThrowsException() {
        assertThrows(Exception.class, () ->
                ctrl.registerCompany(null, "TP", "TAP"));
    }

    // ─────────────────────────────────────────
    // Invalid IATA
    // ─────────────────────────────────────────

    @Test
    void registerCompanyWithOneLetterIataThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.registerCompany("TAP", "T", "TAP"));
    }

    @Test
    void registerCompanyWithThreeLetterIataThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.registerCompany("TAP", "TAP", "TAP"));
    }

    @Test
    void registerCompanyWithNumericIataThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.registerCompany("TAP", "12", "TAP"));
    }

    // ─────────────────────────────────────────
    // Invalid ICAO
    // ─────────────────────────────────────────

    @Test
    void registerCompanyWithOneLetterIcaoThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.registerCompany("TAP", "TP", "T"));
    }

    @Test
    void registerCompanyWithFourLetterIcaoThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.registerCompany("TAP", "TP", "TAPP"));
    }

    @Test
    void registerCompanyWithNumericIcaoThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.registerCompany("TAP", "TP", "123"));
    }
}