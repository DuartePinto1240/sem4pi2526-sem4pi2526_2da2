package persistence.impl.inmemory;

import domain.aircraftmodel.AircraftModel;
import domain.aircraftmodel.AircraftType;
import domain.aircraftmodel.FlightCharacteristics;
import domain.enginemodel.EngineModel;
import domain.enginemodel.MotorizationType;
import domain.enginemodel.Thrust;
import domain.enginemodel.TSFC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryAircraftModelRepositoryTest {

    private InMemoryAircraftModelRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryAircraftModelRepository();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private EngineModel turbofanEngine() {
        return new EngineModel("LEAP-1B", "CFM International",
                MotorizationType.TURBOFAN,
                new Thrust(101.8, 121.4), new TSFC(0.0157));
    }

    private FlightCharacteristics validCharacteristics() {
        return new FlightCharacteristics(
                41413.0, 65317.0, 79016.0, 26020.0,
                12500.0, 230.0, 5500.0, 125.08, 0.0265, 1.45);
    }

    private AircraftModel boeing737Max() {
        return new AircraftModel("737 MAX", "Boeing",
                AircraftType.PASSENGER, validCharacteristics(),
                List.of(turbofanEngine()));
    }

    private AircraftModel airbusA320() {
        return new AircraftModel("A320", "Airbus",
                AircraftType.PASSENGER, validCharacteristics(),
                List.of(turbofanEngine()));
    }

    private <T> List<T> toList(final Iterable<T> iterable) {
        final List<T> list = new ArrayList<>();
        iterable.forEach(list::add);
        return list;
    }

    // ── save ─────────────────────────────────────────────────────────────────

    @Test
    void ensureSaveReturnsTheSavedModel() {
        // Arrange
        final AircraftModel model = boeing737Max();
        // Act
        final AircraftModel saved = repo.save(model);
        // Assert
        assertSame(model, saved);
    }

    @Test
    void ensureCountIncreasesAfterSave() {
        // Arrange
        assertEquals(0, repo.count());
        // Act
        repo.save(boeing737Max());
        // Assert
        assertEquals(1, repo.count());
    }

    // ── existsByNameAndManufacturer ───────────────────────────────────────────

    @Test
    void ensureExistsByNameAndManufacturerReturnsTrueWhenFound() {
        // Arrange
        repo.save(boeing737Max());
        // Act + Assert
        assertTrue(repo.existsByNameAndManufacturer("737 MAX", "Boeing"));
    }

    @Test
    void ensureExistsByNameAndManufacturerIsCaseInsensitive() {
        // Arrange
        repo.save(boeing737Max());
        // Act + Assert
        assertTrue(repo.existsByNameAndManufacturer("737 max", "boeing"));
    }

    @Test
    void ensureExistsByNameAndManufacturerReturnsFalseWhenNotFound() {
        // Arrange + Act + Assert
        assertFalse(repo.existsByNameAndManufacturer("737 MAX", "Boeing"));
    }

    @Test
    void ensureExistsByNameAndManufacturerReturnsFalseForPartialMatch() {
        // Arrange
        repo.save(boeing737Max());
        // Act + Assert
        assertFalse(repo.existsByNameAndManufacturer("737 MAX", "Airbus"));
    }

    // ── findByNameAndManufacturer ─────────────────────────────────────────────

    @Test
    void ensureFindByNameAndManufacturerReturnsModelWhenFound() {
        // Arrange
        final AircraftModel model = boeing737Max();
        repo.save(model);
        // Act
        final var result = repo.findByNameAndManufacturer("737 MAX", "Boeing");
        // Assert
        assertTrue(result.isPresent());
        assertSame(model, result.get());
    }

    @Test
    void ensureFindByNameAndManufacturerIsCaseInsensitive() {
        // Arrange
        repo.save(boeing737Max());
        // Act + Assert
        assertTrue(repo.findByNameAndManufacturer("737 max", "boeing").isPresent());
    }

    @Test
    void ensureFindByNameAndManufacturerReturnsEmptyWhenNotFound() {
        // Arrange + Act
        final var result = repo.findByNameAndManufacturer("737 MAX", "Boeing");
        // Assert
        assertTrue(result.isEmpty());
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void ensureFindAllReturnsEmptyWhenRepositoryIsEmpty() {
        assertTrue(toList(repo.findAll()).isEmpty());
    }

    @Test
    void ensureFindAllReturnsAllSavedModels() {
        // Arrange
        repo.save(boeing737Max());
        repo.save(airbusA320());
        // Act + Assert
        assertEquals(2, toList(repo.findAll()).size());
    }

    // ── ofIdentity ────────────────────────────────────────────────────────────

    @Test
    void ensureOfIdentityReturnsEmptyForUnknownId() {
        // Arrange
        repo.save(boeing737Max());
        // Act + Assert
        assertTrue(repo.ofIdentity(999L).isEmpty());
    }

    // ── containsOfIdentity ────────────────────────────────────────────────────

    @Test
    void ensureContainsOfIdentityReturnsFalseForUnknownId() {
        // Arrange
        repo.save(boeing737Max());
        // Act + Assert
        assertFalse(repo.containsOfIdentity(999L));
    }

    // ── count ─────────────────────────────────────────────────────────────────

    @Test
    void ensureCountReturnsZeroOnEmptyRepository() {
        assertEquals(0, repo.count());
    }

    @Test
    void ensureCountReturnsCorrectNumberAfterMultipleSaves() {
        // Arrange
        repo.save(boeing737Max());
        repo.save(airbusA320());
        // Act + Assert
        assertEquals(2, repo.count());
    }
}