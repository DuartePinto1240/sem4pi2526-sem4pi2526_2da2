package persistence.impl.inmemory;

import lprog.FlightLeg;
import lprog.FlightPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryFlightPlanRepositoryTest {

    private InMemoryFlightPlanRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryFlightPlanRepository();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private FlightLeg validLeg() {
        return new FlightLeg("OPO", "2026-05-17", "14:00",
                "LIS", "15:00", 5000.0, "kg");
    }

    private FlightPlan tp001() {
        return new FlightPlan("TP001", "regular", List.of(validLeg()));
    }

    private FlightPlan tp002() {
        return new FlightPlan("TP002", "charter", List.of(validLeg()));
    }

    private <T> List<T> toList(final Iterable<T> iterable) {
        final List<T> list = new ArrayList<>();
        iterable.forEach(list::add);
        return list;
    }

    // ── save ─────────────────────────────────────────────────────────────────

    @Test
    void ensureSaveReturnsTheSavedFlightPlan() {
        // Arrange
        final FlightPlan fp = tp001();
        // Act
        final FlightPlan saved = repo.save(fp);
        // Assert
        assertSame(fp, saved);
    }

    @Test
    void ensureCountIncreasesAfterSave() {
        // Arrange
        assertEquals(0, repo.count());
        // Act
        repo.save(tp001());
        // Assert
        assertEquals(1, repo.count());
    }

    // ── existsByFlightId ──────────────────────────────────────────────────────

    @Test
    void ensureExistsByFlightIdReturnsTrueWhenFound() {
        // Arrange
        repo.save(tp001());
        // Act + Assert
        assertTrue(repo.existsByFlightId("TP001"));
    }

    @Test
    void ensureExistsByFlightIdIsCaseInsensitive() {
        // Arrange
        repo.save(tp001());
        // Act + Assert
        assertTrue(repo.existsByFlightId("tp001"));
    }

    @Test
    void ensureExistsByFlightIdReturnsFalseWhenNotFound() {
        // Arrange + Act + Assert
        assertFalse(repo.existsByFlightId("TP001"));
    }

    // ── findByFlightId ────────────────────────────────────────────────────────

    @Test
    void ensureFindByFlightIdReturnsFlightPlanWhenFound() {
        // Arrange
        final FlightPlan fp = tp001();
        repo.save(fp);
        // Act
        final var result = repo.findByFlightId("TP001");
        // Assert
        assertTrue(result.isPresent());
        assertSame(fp, result.get());
    }

    @Test
    void ensureFindByFlightIdIsCaseInsensitive() {
        // Arrange
        repo.save(tp001());
        // Act + Assert
        assertTrue(repo.findByFlightId("tp001").isPresent());
    }

    @Test
    void ensureFindByFlightIdReturnsEmptyWhenNotFound() {
        // Arrange + Act
        final var result = repo.findByFlightId("TP001");
        // Assert
        assertTrue(result.isEmpty());
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void ensureFindAllReturnsEmptyWhenRepositoryIsEmpty() {
        // Arrange + Act + Assert
        assertTrue(toList(repo.findAll()).isEmpty());
    }

    @Test
    void ensureFindAllReturnsAllSavedFlightPlans() {
        // Arrange
        repo.save(tp001());
        repo.save(tp002());
        // Act + Assert
        assertEquals(2, toList(repo.findAll()).size());
    }

    // ── ofIdentity ────────────────────────────────────────────────────────────

    @Test
    void ensureOfIdentityReturnsEmptyForUnknownId() {
        // Arrange
        repo.save(tp001());
        // Act + Assert — id is null before persistence
        assertTrue(repo.ofIdentity(999L).isEmpty());
    }

    // ── containsOfIdentity ────────────────────────────────────────────────────

    @Test
    void ensureContainsOfIdentityReturnsFalseForUnknownId() {
        // Arrange
        repo.save(tp001());
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
        repo.save(tp001());
        repo.save(tp002());
        // Act + Assert
        assertEquals(2, repo.count());
    }
}