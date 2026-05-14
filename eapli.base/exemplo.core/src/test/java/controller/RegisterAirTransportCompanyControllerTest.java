package controller;

import domain.AirTransportCompany;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistence.PersistenceContext;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração para RegisterAirTransportCompanyController.
 * Cobre US060 - Registar companhia de transporte aéreo.
 */
class RegisterAirTransportCompanyControllerTest {

    @BeforeEach
    void resetPersistenceContext() throws Exception {
        Field instanceField = PersistenceContext.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    // ----------------------------------------------------------------
    // Registo válido
    // ----------------------------------------------------------------

    @Test
    void registerCompanySuccessfully() {
        RegisterAirTransportCompanyController ctrl =
                new RegisterAirTransportCompanyController();

        AirTransportCompany company =
                ctrl.registerCompany("TAP Air Portugal", "TP", "TAP");

        assertNotNull(company);
        assertEquals("TAP Air Portugal", company.name());
        assertEquals("TP", company.identity().iata());
        assertEquals("TAP", company.identity().icao());
    }

    @Test
    void registerMultipleDistinctCompaniesSuccessfully() {
        RegisterAirTransportCompanyController ctrl =
                new RegisterAirTransportCompanyController();

        AirTransportCompany tap =
                ctrl.registerCompany("TAP Air Portugal", "TP", "TAP");
        AirTransportCompany ryanair =
                ctrl.registerCompany("Ryanair", "FR", "RYR");

        assertNotNull(tap);
        assertNotNull(ryanair);
        assertFalse(tap.sameAs(ryanair));
    }

    // ----------------------------------------------------------------
    // IATA duplicado
    // ----------------------------------------------------------------



    // ----------------------------------------------------------------
    // ICAO duplicado
    // ----------------------------------------------------------------

    @Test
    void registerCompanyWithDuplicateIcaoThrowsException() {
        RegisterAirTransportCompanyController ctrl =
                new RegisterAirTransportCompanyController();

        ctrl.registerCompany("TAP Air Portugal", "TP", "TAP");

        assertThrows(IllegalArgumentException.class, () ->
                ctrl.registerCompany("Outra Empresa", "OT", "TAP"));
    }

    // ----------------------------------------------------------------
    // Nome inválido
    // ----------------------------------------------------------------

    @Test
    void registerCompanyWithEmptyNameThrowsException() {
        RegisterAirTransportCompanyController ctrl =
                new RegisterAirTransportCompanyController();

        assertThrows(IllegalArgumentException.class, () ->
                ctrl.registerCompany("", "TP", "TAP"));
    }

    @Test
    void registerCompanyWithNullNameThrowsException() {
        RegisterAirTransportCompanyController ctrl =
                new RegisterAirTransportCompanyController();

        assertThrows(Exception.class, () ->
                ctrl.registerCompany(null, "TP", "TAP"));
    }

    // ----------------------------------------------------------------
    // IATA inválido
    // ----------------------------------------------------------------

    @Test
    void registerCompanyWithOneLetterIataThrowsException() {
        RegisterAirTransportCompanyController ctrl =
                new RegisterAirTransportCompanyController();

        assertThrows(IllegalArgumentException.class, () ->
                ctrl.registerCompany("TAP", "T", "TAP"));
    }

    @Test
    void registerCompanyWithThreeLetterIataThrowsException() {
        RegisterAirTransportCompanyController ctrl =
                new RegisterAirTransportCompanyController();

        assertThrows(IllegalArgumentException.class, () ->
                ctrl.registerCompany("TAP", "TAP", "TAP"));
    }

    // ----------------------------------------------------------------
    // ICAO inválido
    // ----------------------------------------------------------------

    @Test
    void registerCompanyWithOneLetterIcaoThrowsException() {
        RegisterAirTransportCompanyController ctrl =
                new RegisterAirTransportCompanyController();

        assertThrows(IllegalArgumentException.class, () ->
                ctrl.registerCompany("TAP", "TP", "T"));
    }

    @Test
    void registerCompanyWithFourLetterIcaoThrowsException() {
        RegisterAirTransportCompanyController ctrl =
                new RegisterAirTransportCompanyController();

        assertThrows(IllegalArgumentException.class, () ->
                ctrl.registerCompany("TAP", "TP", "TAPP"));
    }
}
