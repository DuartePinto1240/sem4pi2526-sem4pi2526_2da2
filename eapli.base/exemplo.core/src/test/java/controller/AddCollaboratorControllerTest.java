package controller;

import domain.AirControlArea;
import domain.AirTransportCompany;
import domain.GeographicBoundary;
import dto.CustomerDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistence.impl.inmemory.InMemoryCustomerRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for US061 – Add a Customer's Collaborator.
 */
class AddCollaboratorControllerTest {

    private static final GeographicBoundary BOUNDARY =
            new GeographicBoundary(36.0, 42.0, -10.0, -6.0);

    private InMemoryCustomerRepository customerRepo;

    @BeforeEach
    void setUp() {
        customerRepo = new InMemoryCustomerRepository();
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

    // ── getAvailableCustomers() ───────────────────────────────────

    @Test
    void ensureAvailableCustomersReturnsRegisteredCompany() {
        savedCompany();
        assertTrue(customerRepo.findAllAirTransportCompanies().iterator().hasNext());
    }

    @Test
    void ensureAvailableCustomersReturnsRegisteredArea() {
        savedArea();
        assertTrue(customerRepo.findAllAirControlAreas().iterator().hasNext());
    }

    @Test
    void ensureAvailableCustomersIsEmptyWhenNoneRegistered() {
        assertFalse(customerRepo.findAllAirTransportCompanies().iterator().hasNext());
        assertFalse(customerRepo.findAllAirControlAreas().iterator().hasNext());
    }

    // ── addCollaborator() to company ──────────────────────────────

    @Test
    void ensureCollaboratorIsAddedToCompany() {
        // Arrange
        final AirTransportCompany company = savedCompany();

        // Act
        company.addCollaborator("joao@tap.pt", "João Silva", "Manager");
        customerRepo.save(company);

        // Assert
        final AirTransportCompany loaded =
                (AirTransportCompany) customerRepo.findById("TP/TAP",
                        CustomerDTO.CustomerType.AIR_TRANSPORT_COMPANY).get();
        assertTrue(loaded.findCollaboratorByEmail("joao@tap.pt").isPresent());
    }

    @Test
    void ensureNewCollaboratorIsActive() {
        // Arrange
        final AirTransportCompany company = savedCompany();

        // Act
        company.addCollaborator("joao@tap.pt", "João Silva", "Manager");

        // Assert — US061.5
        assertTrue(company.findCollaboratorByEmail("joao@tap.pt").get().isActive());
    }

    @Test
    void ensureDuplicateEmailWithinSameCompanyThrows() {
        // Arrange
        final AirTransportCompany company = savedCompany();
        company.addCollaborator("dup@tap.pt", "First", "Manager");

        // Act & Assert — US061.3
        assertThrows(IllegalArgumentException.class,
                () -> company.addCollaborator("dup@tap.pt", "Second", "Analyst"));
    }

    // ── addCollaborator() to area ─────────────────────────────────

    @Test
    void ensureCollaboratorIsAddedToArea() {
        // Arrange
        final AirControlArea area = savedArea();

        // Act
        area.addCollaborator("fco@lppc.pt", "Maria Santos", "Senior FCO");
        customerRepo.save(area);

        // Assert
        final AirControlArea loaded =
                (AirControlArea) customerRepo.findById("LPPC",
                        CustomerDTO.CustomerType.AIR_CONTROL_AREA).get();
        assertTrue(loaded.findCollaboratorByEmail("fco@lppc.pt").isPresent());
    }

    @Test
    void ensureDuplicateEmailWithinSameAreaThrows() {
        // Arrange
        final AirControlArea area = savedArea();
        area.addCollaborator("fco@lppc.pt", "Maria", "Senior FCO");

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> area.addCollaborator("fco@lppc.pt", "Other", "Junior FCO"));
    }

    // ── findById routing ──────────────────────────────────────────

    @Test
    void ensureFindByIdReturnsCorrectCompany() {
        // Arrange
        savedCompany();

        // Act
        final var result = customerRepo.findById("TP/TAP",
                CustomerDTO.CustomerType.AIR_TRANSPORT_COMPANY);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("TAP Air Portugal", ((AirTransportCompany) result.get()).name());
    }

    @Test
    void ensureFindByIdReturnsCorrectArea() {
        // Arrange
        savedArea();

        // Act
        final var result = customerRepo.findById("LPPC",
                CustomerDTO.CustomerType.AIR_CONTROL_AREA);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Lisbon ACC", ((AirControlArea) result.get()).name());
    }

    @Test
    void ensureFindByIdReturnsEmptyForUnknownCustomer() {
        assertTrue(customerRepo.findById("XX/XXX",
                CustomerDTO.CustomerType.AIR_TRANSPORT_COMPANY).isEmpty());
    }
}