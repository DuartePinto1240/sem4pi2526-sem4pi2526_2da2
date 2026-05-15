package controller;

import domain.AirControlArea;
import domain.AirTransportCompany;
import domain.GeographicBoundary;
import dto.CustomerDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistence.PersistenceContext;
import persistence.impl.inmemory.InMemoryCustomerRepository;
import persistence.impl.inmemory.InMemoryUserRepository;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for US063 – Edit a Customer's Collaborator.
 */
class EditCollaboratorControllerTest {

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

    // ── updateCollaboratorEmail() on company ──────────────────────

    @Test
    void ensureCompanyCollaboratorEmailIsUpdated() {
        // Arrange
        final AirTransportCompany company = savedCompanyWithCollaborator();

        // Act — US063
        company.updateCollaboratorEmail("joao@tap.pt", "joao.new@tap.pt");
        customerRepo.save(company);

        // Assert
        final AirTransportCompany loaded =
                (AirTransportCompany) customerRepo.findById("TP/TAP",
                        CustomerDTO.CustomerType.AIR_TRANSPORT_COMPANY).get();
        assertFalse(loaded.findCollaboratorByEmail("joao@tap.pt").isPresent());
        assertTrue(loaded.findCollaboratorByEmail("joao.new@tap.pt").isPresent());
    }

    @Test
    void ensureNameIsImmutableAfterEmailUpdate() {
        // Arrange
        final AirTransportCompany company = savedCompanyWithCollaborator();

        // Act
        company.updateCollaboratorEmail("joao@tap.pt", "joao.new@tap.pt");

        // Assert — US063.1
        assertEquals("João Silva",
                company.findCollaboratorByEmail("joao.new@tap.pt").get().name());
    }

    @Test
    void ensurePositionIsImmutableAfterEmailUpdate() {
        // Arrange
        final AirTransportCompany company = savedCompanyWithCollaborator();

        // Act
        company.updateCollaboratorEmail("joao@tap.pt", "joao.new@tap.pt");

        // Assert — US063.1
        assertEquals("Manager",
                company.findCollaboratorByEmail("joao.new@tap.pt").get().position());
    }

    @Test
    void ensureUpdateEmailForNonExistentCollaboratorThrows() {
        // Arrange
        final AirTransportCompany company = savedCompanyWithCollaborator();

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> company.updateCollaboratorEmail("ghost@tap.pt", "new@tap.pt"));
    }

    @Test
    void ensureUpdateEmailToSameValueKeepsCollaborator() {
        // Arrange
        final AirTransportCompany company = savedCompanyWithCollaborator();

        // Act — updating email to the same value
        company.updateCollaboratorEmail("joao@tap.pt", "joao@tap.pt");

        // Assert
        assertTrue(company.findCollaboratorByEmail("joao@tap.pt").isPresent());
    }

    // ── updateCollaboratorEmail() on area ─────────────────────────

    @Test
    void ensureAreaCollaboratorEmailIsUpdated() {
        // Arrange
        final AirControlArea area = savedAreaWithCollaborator();

        // Act — US063
        area.updateCollaboratorEmail("fco@lppc.pt", "fco.new@lppc.pt");
        customerRepo.save(area);

        // Assert
        final AirControlArea loaded =
                (AirControlArea) customerRepo.findById("LPPC",
                        CustomerDTO.CustomerType.AIR_CONTROL_AREA).get();
        assertFalse(loaded.findCollaboratorByEmail("fco@lppc.pt").isPresent());
        assertTrue(loaded.findCollaboratorByEmail("fco.new@lppc.pt").isPresent());
    }

    @Test
    void ensureAreaNameIsImmutableAfterEmailUpdate() {
        // Arrange
        final AirControlArea area = savedAreaWithCollaborator();

        // Act
        area.updateCollaboratorEmail("fco@lppc.pt", "fco.new@lppc.pt");

        // Assert — US063.1
        assertEquals("Maria Santos",
                area.findCollaboratorByEmail("fco.new@lppc.pt").get().name());
    }
}