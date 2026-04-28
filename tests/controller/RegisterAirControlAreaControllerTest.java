package controller;

import domain.AirControlArea;
import domain.AirControlAreaCode;
import org.junit.Before;
import org.junit.Test;
import persistence.impl.inmemory.InMemoryAirControlAreaRepository;
import repositories.AirControlAreaRepository;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link RegisterAirControlAreaController} (US050).
 *
 * <p>Uses an {@link InMemoryAirControlAreaRepository} injected via reflection
 * to keep tests fast and independent of JPA/database concerns.</p>
 *
 * <p>Follows the AAA (Arrange-Act-Assert) convention as required by NFR03.
 * Coverage targets: all three business rules (unique code, unique name,
 * no boundary overlap), coordinate validation, and happy-path persistence.</p>
 */
public class RegisterAirControlAreaControllerTest {

    private RegisterAirControlAreaController controller;
    private AirControlAreaRepository repo;

    @Before
    public void setUp() throws Exception {
        repo = new InMemoryAirControlAreaRepository();
        controller = new RegisterAirControlAreaController();
        // Inject in-memory repo to bypass PersistenceContext
        final Field field = RegisterAirControlAreaController.class.getDeclaredField("repo");
        field.setAccessible(true);
        field.set(controller, repo);
    }

    // =========================================================================
    // Happy path
    // =========================================================================

    @Test
    public void ensureValidAreaIsRegisteredAndReturned() {
        // Arrange — empty repo

        // Act
        final AirControlArea area = controller.registerAirControlArea(
                "LPPC", "Lisbon ACC", 36.0, 42.0, -10.0, -6.0);

        // Assert
        assertNotNull(area);
        assertEquals("LPPC", area.identity().code());
        assertEquals("Lisbon ACC", area.name());
    }

    @Test
    public void ensureRegisteredAreaIsPersisted() {
        // Arrange
        controller.registerAirControlArea(
                "LPPC", "Lisbon ACC", 36.0, 42.0, -10.0, -6.0);

        // Act
        final boolean exists = repo.containsOfIdentity(new AirControlAreaCode("LPPC"));

        // Assert
        assertTrue(exists);
    }

    @Test
    public void ensureTwoNonOverlappingAreasCanBeRegistered() {
        // Arrange — two areas that do not overlap
        controller.registerAirControlArea(
                "LPPC", "Lisbon ACC",  36.0, 42.0, -10.0, -6.0);

        // Act — entirely to the right — no shared interior
        final AirControlArea second = controller.registerAirControlArea(
                "LECM", "Madrid ACC", 36.0, 44.0, -6.0, 4.5);

        // Assert — adjacent areas share a lon edge which is allowed
        assertNotNull(second);
        assertEquals(2, repo.count());
    }

    @Test
    public void ensureAdjacentAreasSharingOnlyAnEdgeAreAllowed() {
        // Arrange — LPPC ends at maxLon = -6.0; LECM starts at minLon = -6.0
        controller.registerAirControlArea(
                "LPPC", "Lisbon ACC", 36.0, 42.0, -10.0, -6.0);

        // Act — sharing the lon=-6 edge is valid (they do not share interior)
        final AirControlArea adjacent = controller.registerAirControlArea(
                "LECM", "Madrid ACC", 36.0, 42.0, -6.0, 0.0);

        // Assert
        assertNotNull(adjacent);
    }

    // =========================================================================
    // Rule 1 — Unique code
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void ensureDuplicateCodeIsRejected() {
        // Arrange
        controller.registerAirControlArea(
                "LPPC", "Lisbon ACC", 36.0, 42.0, -10.0, -6.0);

        // Act — same code, different name and non-overlapping boundary
        controller.registerAirControlArea(
                "LPPC", "Other Area", 50.0, 55.0, 10.0, 20.0);

        // Assert: exception expected
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureDuplicateCodeIsCaseInsensitive() {
        // Arrange
        controller.registerAirControlArea(
                "LPPC", "Lisbon ACC", 36.0, 42.0, -10.0, -6.0);

        // Act — lowercase version of the same code
        controller.registerAirControlArea(
                "lppc", "Other Area", 50.0, 55.0, 10.0, 20.0);

        // Assert: exception expected
    }

    // =========================================================================
    // Rule 2 — Unique name
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void ensureDuplicateNameIsRejected() {
        // Arrange
        controller.registerAirControlArea(
                "LPPC", "Lisbon ACC", 36.0, 42.0, -10.0, -6.0);

        // Act — different code, same name, non-overlapping boundary
        controller.registerAirControlArea(
                "LECM", "Lisbon ACC", 50.0, 55.0, 10.0, 20.0);

        // Assert: exception expected
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureDuplicateNameIsCaseInsensitive() {
        // Arrange
        controller.registerAirControlArea(
                "LPPC", "Lisbon ACC", 36.0, 42.0, -10.0, -6.0);

        // Act — same name in different case, non-overlapping boundary
        controller.registerAirControlArea(
                "LECM", "lisbon acc", 50.0, 55.0, 10.0, 20.0);

        // Assert: exception expected
    }

    // =========================================================================
    // Rule 3 — No boundary overlap
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void ensureOverlappingBoundaryIsRejected() {
        // Arrange
        controller.registerAirControlArea(
                "LPPC", "Lisbon ACC", 36.0, 42.0, -10.0, -6.0);

        // Act — boundary overlaps the top-right corner of LPPC
        controller.registerAirControlArea(
                "LECM", "Madrid ACC", 40.0, 46.0, -8.0, -2.0);

        // Assert: exception expected
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureFullyContainedBoundaryIsRejected() {
        // Arrange — LECM entirely inside LPPC
        controller.registerAirControlArea(
                "LPPC", "Lisbon ACC", 30.0, 50.0, -15.0, 0.0);

        // Act
        controller.registerAirControlArea(
                "LECM", "Madrid ACC", 36.0, 42.0, -10.0, -6.0);

        // Assert: exception expected
    }

    // =========================================================================
    // Coordinate validation (delegated to GeographicBoundary)
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void ensureInvalidMinLatIsRejected() {
        controller.registerAirControlArea(
                "LPPC", "Lisbon ACC", -95.0, 42.0, -10.0, -6.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureInvalidMaxLatIsRejected() {
        controller.registerAirControlArea(
                "LPPC", "Lisbon ACC", 36.0, 95.0, -10.0, -6.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureMinLatGreaterThanMaxLatIsRejected() {
        controller.registerAirControlArea(
                "LPPC", "Lisbon ACC", 42.0, 36.0, -10.0, -6.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureInvalidMinLonIsRejected() {
        controller.registerAirControlArea(
                "LPPC", "Lisbon ACC", 36.0, 42.0, -190.0, -6.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureMinLonGreaterThanMaxLonIsRejected() {
        controller.registerAirControlArea(
                "LPPC", "Lisbon ACC", 36.0, 42.0, -6.0, -10.0);
    }

    // =========================================================================
    // Null / blank inputs
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void ensureNullCodeIsRejected() {
        controller.registerAirControlArea(
                null, "Lisbon ACC", 36.0, 42.0, -10.0, -6.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureBlankCodeIsRejected() {
        controller.registerAirControlArea(
                "   ", "Lisbon ACC", 36.0, 42.0, -10.0, -6.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureNullNameIsRejected() {
        controller.registerAirControlArea(
                "LPPC", null, 36.0, 42.0, -10.0, -6.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureBlankNameIsRejected() {
        controller.registerAirControlArea(
                "LPPC", "   ", 36.0, 42.0, -10.0, -6.0);
    }

    @Test
    public void ensureAllAreasReturnsEmptyWhenNoAreasExist() {
        // Arrange — empty repo

        // Act
        final Iterable<AirControlArea> areas = controller.allAreas();

        // Assert
        assertNotNull(areas);
        assertFalse(areas.iterator().hasNext());
    }

    @Test
    public void ensureAllAreasReturnsRegisteredAreas() {
        // Arrange
        controller.registerAirControlArea(
                "LPPC", "Lisbon ACC", 36.0, 42.0, -10.0, -6.0);

        controller.registerAirControlArea(
                "LECM", "Madrid ACC", 36.0, 42.0, -6.0, 0.0);

        // Act
        final Iterable<AirControlArea> areas = controller.allAreas();

        int count = 0;
        for (final AirControlArea area : areas) {
            count++;
        }

        // Assert
        assertEquals(2, count);
    }
}
