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
import persistence.impl.inmemory.InMemoryUserRepository;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for US064 – Disable a Customer's Collaborator.
 */
class DisableCollaboratorControllerTest {

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

        final Field urepoField = PersistenceContext.class.getDeclaredField("userRepository");
        urepoField.setAccessible(true);
        urepoField.set(ctx, new InMemoryUserRepository());
    }

    private AirTransportCompany savedCompanyWithCollaborator() {
        final AirTransportCompany c = new AirTransportCompany("TAP Air Portugal", "TP", "TAP");
        c.addCollaborator("joao@tap.pt", "João Silva", "Manager");
        customerRepo.save(c);
        return c;
    }

    private AirControlArea savedAreaWithCollaborator() {
        final AirControlArea a = new AirControlArea("LPPC", "Lisbon ACC", BOUNDARY);
        a.addCollaborator("fco@lppc.pt", "Maria Santos", "Senior FCO");
        customerRepo.save(a);
        return a;
    }

    // ── disableCollaborator() on company ──────────────────────────

    @Test
    void ensureCompanyCollaboratorIsDisabled() {
        // Arrange
        final AirTransportCompany company = savedCompanyWithCollaborator();

        // Act — US064
        company.disableCollaborator("joao@tap.pt");
        customerRepo.save(company);

        // Assert
        final AirTransportCompany loaded =
                (AirTransportCompany) customerRepo.findById("TP/TAP",
                        CustomerDTO.CustomerType.AIR_TRANSPORT_COMPANY).get();
        assertFalse(loaded.findCollaboratorByEmail("joao@tap.pt").get().isActive());
    }

    @Test
    void ensureDisabledCompanyCollaboratorDoesNotAppearInActiveList() {
        // Arrange
        final AirTransportCompany company = savedCompanyWithCollaborator();

        // Act
        company.disableCollaborator("joao@tap.pt");

        // Assert — US062 + US064 integration
        assertTrue(company.listActiveCollaborators().isEmpty());
    }

    @Test
    void ensureCompanyCollaboratorRecordRetainedAfterDisable() {
        // Arrange
        final AirTransportCompany company = savedCompanyWithCollaborator();

        // Act — US064.3 soft delete
        company.disableCollaborator("joao@tap.pt");

        // Assert: record still exists, just inactive
        assertTrue(company.findCollaboratorByEmail("joao@tap.pt").isPresent());
        assertFalse(company.findCollaboratorByEmail("joao@tap.pt").get().isActive());
    }

    @Test
    void ensureDisableCompanyCollaboratorIsIdempotent() {
        // Arrange
        final AirTransportCompany company = savedCompanyWithCollaborator();
        company.disableCollaborator("joao@tap.pt");

        // Act & Assert — US064.5
        assertDoesNotThrow(() -> company.disableCollaborator("joao@tap.pt"));
        assertFalse(company.findCollaboratorByEmail("joao@tap.pt").get().isActive());
    }

    @Test
    void ensureDisableNonExistentCompanyCollaboratorThrows() {
        // Arrange
        final AirTransportCompany company = savedCompanyWithCollaborator();

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> company.disableCollaborator("ghost@tap.pt"));
    }

    @Test
    void ensureOnlyTargetCollaboratorIsDisabled() {
        // Arrange
        final AirTransportCompany company = savedCompanyWithCollaborator();
        company.addCollaborator("maria@tap.pt", "Maria", "Analyst");

        // Act — disable only joao
        company.disableCollaborator("joao@tap.pt");

        // Assert: maria still active
        final List<CollaboratorDTO> active = company.listActiveCollaborators();
        assertEquals(1, active.size());
        assertEquals("maria@tap.pt", active.get(0).email);
    }

    // ── disableCollaborator() on area ─────────────────────────────

    @Test
    void ensureAreaCollaboratorIsDisabled() {
        // Arrange
        final AirControlArea area = savedAreaWithCollaborator();

        // Act — US064
        area.disableCollaborator("fco@lppc.pt");
        customerRepo.save(area);

        // Assert
        final AirControlArea loaded =
                (AirControlArea) customerRepo.findById("LPPC",
                        CustomerDTO.CustomerType.AIR_CONTROL_AREA).get();
        assertFalse(loaded.findCollaboratorByEmail("fco@lppc.pt").get().isActive());
    }

    @Test
    void ensureDisabledAreaCollaboratorDoesNotAppearInActiveList() {
        // Arrange
        final AirControlArea area = savedAreaWithCollaborator();

        // Act
        area.disableCollaborator("fco@lppc.pt");

        // Assert — US062 + US064 integration
        assertTrue(area.listActiveCollaborators().isEmpty());
    }

    @Test
    void ensureDisableAreaCollaboratorIsIdempotent() {
        // Arrange
        final AirControlArea area = savedAreaWithCollaborator();
        area.disableCollaborator("fco@lppc.pt");

        // Act & Assert — US064.5
        assertDoesNotThrow(() -> area.disableCollaborator("fco@lppc.pt"));
    }

    @Test
    void ensureAreaCollaboratorRecordRetainedAfterDisable() {
        // Arrange
        final AirControlArea area = savedAreaWithCollaborator();

        // Act — soft delete
        area.disableCollaborator("fco@lppc.pt");

        // Assert — US064.3
        assertTrue(area.findCollaboratorByEmail("fco@lppc.pt").isPresent());
        assertFalse(area.findCollaboratorByEmail("fco@lppc.pt").get().isActive());
    }
}