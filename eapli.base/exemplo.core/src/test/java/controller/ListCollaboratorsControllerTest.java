package controller;

import domain.AirControlArea;
import domain.AirTransportCompany;
import domain.GeographicBoundary;
import dto.CollaboratorDTO;
import dto.CustomerDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistence.PersistenceContext;
import persistence.impl.inmemory.InMemoryCustomerRepository;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for US062 – List Customer's Collaborators.
 */
class ListCollaboratorsControllerTest {

    private static final GeographicBoundary BOUNDARY =
            new GeographicBoundary(36.0, 42.0, -10.0, -6.0);

    private InMemoryCustomerRepository customerRepo;

    @BeforeEach
    void setUp() throws Exception {
        final Field instanceField = PersistenceContext.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        final PersistenceContext ctx = PersistenceContext.getInstance();
        customerRepo = new InMemoryCustomerRepository();
        final Field crepoField = PersistenceContext.class.getDeclaredField("customerRepository");
        crepoField.setAccessible(true);
        crepoField.set(ctx, customerRepo);
    }

    private AirTransportCompany savedCompany() {
        final AirTransportCompany c = new AirTransportCompany("TAP Air Portugal", "TP", "TAP");
        customerRepo.save(c);
        return c;
    }

    private AirControlArea savedArea() {
        final AirControlArea a = new AirControlArea("LPPC", "Lisbon ACC", BOUNDARY);
        customerRepo.save(a);
        return a;
    }

    // ── listCollaborators() for company ───────────────────────────

    @Test
    void ensureActiveCompanyCollaboratorsAreReturned() {
        // Arrange
        final AirTransportCompany company = savedCompany();
        company.addCollaborator("joao@tap.pt", "João", "Manager");
        customerRepo.save(company);

        // Act
        final AirTransportCompany loaded =
                (AirTransportCompany) customerRepo.findById("TP/TAP",
                        CustomerDTO.CustomerType.AIR_TRANSPORT_COMPANY).get();
        final List<CollaboratorDTO> result = loaded.listActiveCollaborators();

        // Assert — US062.2
        assertEquals(1, result.size());
        assertEquals("joao@tap.pt", result.get(0).email);
    }

    @Test
    void ensureDisabledCompanyCollaboratorsAreExcluded() {
        // Arrange
        final AirTransportCompany company = savedCompany();
        company.addCollaborator("active@tap.pt",   "Active",   "Manager");
        company.addCollaborator("disabled@tap.pt", "Disabled", "Analyst");
        company.disableCollaborator("disabled@tap.pt");
        customerRepo.save(company);

        // Act
        final AirTransportCompany loaded =
                (AirTransportCompany) customerRepo.findById("TP/TAP",
                        CustomerDTO.CustomerType.AIR_TRANSPORT_COMPANY).get();
        final List<CollaboratorDTO> result = loaded.listActiveCollaborators();

        // Assert — US062.3
        assertEquals(1, result.size());
        assertEquals("active@tap.pt", result.get(0).email);
    }

    @Test
    void ensureEmptyListWhenNoActiveCompanyCollaborators() {
        // Arrange
        final AirTransportCompany company = savedCompany();
        company.addCollaborator("only@tap.pt", "Only", "Manager");
        company.disableCollaborator("only@tap.pt");
        customerRepo.save(company);

        // Act
        final AirTransportCompany loaded =
                (AirTransportCompany) customerRepo.findById("TP/TAP",
                        CustomerDTO.CustomerType.AIR_TRANSPORT_COMPANY).get();

        // Assert
        assertTrue(loaded.listActiveCollaborators().isEmpty());
    }

    @Test
    void ensureListIsScopedToSelectedCompany() {
        // Arrange
        final AirTransportCompany tap     = new AirTransportCompany("TAP",     "TP", "TAP");
        final AirTransportCompany ryanair = new AirTransportCompany("Ryanair", "FR", "RYR");
        tap.addCollaborator("collab@tap.pt", "TAP Person", "Manager");
        ryanair.addCollaborator("collab@fr.com", "FR Person", "Analyst");
        customerRepo.save(tap);
        customerRepo.save(ryanair);

        // Act
        final AirTransportCompany loaded =
                (AirTransportCompany) customerRepo.findById("TP/TAP",
                        CustomerDTO.CustomerType.AIR_TRANSPORT_COMPANY).get();
        final List<CollaboratorDTO> result = loaded.listActiveCollaborators();

        // Assert — US062.1
        assertEquals(1, result.size());
        assertEquals("collab@tap.pt", result.get(0).email);
    }

    // ── listCollaborators() for area ──────────────────────────────

    @Test
    void ensureActiveAreaCollaboratorsAreReturned() {
        // Arrange
        final AirControlArea area = savedArea();
        area.addCollaborator("fco@lppc.pt", "Maria", "Senior FCO");
        customerRepo.save(area);

        // Act
        final AirControlArea loaded =
                (AirControlArea) customerRepo.findById("LPPC",
                        CustomerDTO.CustomerType.AIR_CONTROL_AREA).get();
        final List<CollaboratorDTO> result = loaded.listActiveCollaborators();

        // Assert
        assertEquals(1, result.size());
        assertEquals("fco@lppc.pt", result.get(0).email);
    }

    @Test
    void ensureDisabledAreaCollaboratorsAreExcluded() {
        // Arrange
        final AirControlArea area = savedArea();
        area.addCollaborator("active@lppc.pt",   "Active",   "Senior FCO");
        area.addCollaborator("disabled@lppc.pt", "Disabled", "Junior FCO");
        area.disableCollaborator("disabled@lppc.pt");
        customerRepo.save(area);

        // Act
        final AirControlArea loaded =
                (AirControlArea) customerRepo.findById("LPPC",
                        CustomerDTO.CustomerType.AIR_CONTROL_AREA).get();
        final List<CollaboratorDTO> result = loaded.listActiveCollaborators();

        // Assert — US062.3
        assertEquals(1, result.size());
        assertEquals("active@lppc.pt", result.get(0).email);
    }
}