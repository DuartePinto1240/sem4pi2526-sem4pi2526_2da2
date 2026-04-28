package repositories;

import domain.AirControlArea;
import domain.AirControlAreaCode;
import domain.GeographicBoundary;
import eapli.framework.domain.repositories.DomainRepository;

/**
 * Repository interface for {@link AirControlArea} aggregate roots (US050).
 *
 * <p>Extends the EAPLI Framework's {@link DomainRepository} which already
 * provides {@code save}, {@code ofIdentity}, {@code containsOfIdentity},
 * {@code delete}, {@code deleteOfIdentity}, and {@code count}.</p>
 */
public interface AirControlAreaRepository extends DomainRepository<AirControlAreaCode, AirControlArea> {

    /**
     * Returns all registered air control areas.
     *
     * @return iterable of all {@link AirControlArea} records
     */
    Iterable<AirControlArea> findAll();

    /**
     * Checks whether an air control area with the given name already exists.
     *
     * @param name the name to check (case-insensitive)
     * @return {@code true} if a area with that name already exists
     */
    boolean existsByName(String name);

    /**
     * Checks whether any existing area's boundary overlaps with the given boundary.
     *
     * <p>The professor confirmed that overlapping areas must be rejected.
     * Uses {@link GeographicBoundary#overlapsWith(GeographicBoundary)} for
     * the geometric intersection test.</p>
     *
     * @param boundary the boundary to check against all existing areas
     * @return {@code true} if at least one existing area overlaps with the boundary
     */
    boolean existsOverlappingBoundary(GeographicBoundary boundary);
}
