package persistence.impl.inmemory;

import domain.enginemodel.EngineModel;
import domain.enginemodel.MotorizationType;
import domain.enginemodel.Thrust;
import domain.enginemodel.TSFC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryEngineModelRepositoryTest {

    private InMemoryEngineModelRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryEngineModelRepository();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private EngineModel leap1B() {
        return new EngineModel(
                "LEAP-1B", "CFM International",
                MotorizationType.TURBOFAN,
                new Thrust(101.8, 121.4),
                new TSFC(0.0157));
    }

    private EngineModel trent900() {
        return new EngineModel(
                "Trent 900", "Rolls-Royce",
                MotorizationType.TURBOFAN,
                new Thrust(311.0, 340.0),
                new TSFC(0.0152));
    }

    private EngineModel pw127() {
        return new EngineModel(
                "PW127", "Pratt & Whitney Canada",
                MotorizationType.TURBOPROP,
                new Thrust(15.0, 20.5),
                new TSFC(0.0300));
    }

    // ── save ─────────────────────────────────────────────────────────────────

    @Test
    void ensureSaveReturnsTheSavedModel() {
        // Arrange
        final EngineModel model = leap1B();
        // Act
        final EngineModel saved = repo.save(model);
        // Assert
        assertSame(model, saved);
    }

    @Test
    void ensureCountIncreasesAfterSave() {
        // Arrange
        assertEquals(0, repo.count());
        // Act
        repo.save(leap1B());
        // Assert
        assertEquals(1, repo.count());
    }

    // ── existsByNameAndManufacturer ───────────────────────────────────────────

    @Test
    void ensureExistsByNameAndManufacturerReturnsTrueWhenFound() {
        // Arrange
        repo.save(leap1B());
        // Act + Assert
        assertTrue(repo.existsByNameAndManufacturer("LEAP-1B", "CFM International"));
    }

    @Test
    void ensureExistsByNameAndManufacturerIsCaseInsensitive() {
        // Arrange
        repo.save(leap1B());
        // Act + Assert
        assertTrue(repo.existsByNameAndManufacturer("leap-1b", "cfm international"));
    }

    @Test
    void ensureExistsByNameAndManufacturerReturnsFalseWhenNotFound() {
        // Arrange + Act + Assert
        assertFalse(repo.existsByNameAndManufacturer("LEAP-1B", "CFM International"));
    }

    @Test
    void ensureExistsByNameAndManufacturerReturnsFalseForPartialMatch() {
        // Arrange
        repo.save(leap1B());
        // Act + Assert — same name, different manufacturer
        assertFalse(repo.existsByNameAndManufacturer("LEAP-1B", "Other Manufacturer"));
    }

    // ── findByNameAndManufacturer ─────────────────────────────────────────────

    @Test
    void ensureFindByNameAndManufacturerReturnsModelWhenFound() {
        // Arrange
        final EngineModel model = leap1B();
        repo.save(model);
        // Act
        final var result = repo.findByNameAndManufacturer("LEAP-1B", "CFM International");
        // Assert
        assertTrue(result.isPresent());
        assertSame(model, result.get());
    }

    @Test
    void ensureFindByNameAndManufacturerIsCaseInsensitive() {
        // Arrange
        final EngineModel model = leap1B();
        repo.save(model);
        // Act
        final var result = repo.findByNameAndManufacturer("leap-1b", "cfm international");
        // Assert
        assertTrue(result.isPresent());
    }

    @Test
    void ensureFindByNameAndManufacturerReturnsEmptyWhenNotFound() {
        // Arrange + Act
        final var result = repo.findByNameAndManufacturer("LEAP-1B", "CFM International");
        // Assert
        assertTrue(result.isEmpty());
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void ensureFindAllReturnsEmptyWhenRepositoryIsEmpty() {
        // Arrange + Act
        final List<EngineModel> all = toList(repo.findAll());
        // Assert
        assertTrue(all.isEmpty());
    }

    @Test
    void ensureFindAllReturnsAllSavedModels() {
        // Arrange
        repo.save(leap1B());
        repo.save(trent900());
        // Act
        final List<EngineModel> all = toList(repo.findAll());
        // Assert
        assertEquals(2, all.size());
    }

    // ── findByMotorizationType ────────────────────────────────────────────────

    @Test
    void ensureFindByMotorizationTypeReturnsOnlyMatchingEngines() {
        // Arrange
        repo.save(leap1B());    // TURBOFAN
        repo.save(trent900());  // TURBOFAN
        repo.save(pw127());     // TURBOPROP
        // Act
        final List<EngineModel> turbofans =
                toList(repo.findByMotorizationType(MotorizationType.TURBOFAN));
        // Assert
        assertEquals(2, turbofans.size());
        turbofans.forEach(e ->
                assertEquals(MotorizationType.TURBOFAN, e.motorizationType()));
    }

    @Test
    void ensureFindByMotorizationTypeReturnsEmptyWhenNoneMatch() {
        // Arrange
        repo.save(leap1B()); // TURBOFAN only
        // Act
        final List<EngineModel> ramjets =
                toList(repo.findByMotorizationType(MotorizationType.RAMJET));
        // Assert
        assertTrue(ramjets.isEmpty());
    }

    // ── ofIdentity ────────────────────────────────────────────────────────────

    @Test
    void ensureOfIdentityReturnsEmptyForUnknownId() {
        // Arrange
        repo.save(leap1B());
        // Act + Assert — id is null before persistence; 999L is unknown
        assertTrue(repo.ofIdentity(999L).isEmpty());
    }

    // ── containsOfIdentity ────────────────────────────────────────────────────

    @Test
    void ensureContainsOfIdentityReturnsFalseForUnknownId() {
        // Arrange
        repo.save(leap1B());
        // Act + Assert
        assertFalse(repo.containsOfIdentity(999L));
    }

    // ── count ─────────────────────────────────────────────────────────────────

    @Test
    void ensureCountReturnsZeroOnEmptyRepository() {
        // Arrange + Act + Assert
        assertEquals(0, repo.count());
    }

    @Test
    void ensureCountReturnsCorrectNumberAfterMultipleSaves() {
        // Arrange
        repo.save(leap1B());
        repo.save(trent900());
        repo.save(pw127());
        // Act + Assert
        assertEquals(3, repo.count());
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private <T> List<T> toList(final Iterable<T> iterable) {
        final List<T> list = new ArrayList<>();
        iterable.forEach(list::add);
        return list;
    }
}