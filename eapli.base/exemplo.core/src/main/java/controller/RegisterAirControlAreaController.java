package controller;

import domain.AirControlArea;
import domain.AirControlAreaCode;
import domain.GeographicBoundary;
import persistence.PersistenceContext;
import repositories.AirControlAreaRepository;

/**
 * Application-layer controller for US050 — Register an Air Control Area.
 *
 *   <li>Area code must be unique system-wide.</li>
 *   <li>Area name must be unique system-wide.</li>
 *
 * <p>All domain validation (coordinate ranges, min &lt; max) is enforced
 * by {@link GeographicBoundary} itself.</p>
 */
public class RegisterAirControlAreaController {

    private final AirControlAreaRepository repo;

    /**
     * Creates the controller, obtaining the repository from
     * {@link PersistenceContext}.
     */
    public RegisterAirControlAreaController() {
        this.repo = PersistenceContext.getInstance().airControlAreaRepository();
    }

    /**
     * Returns all registered air control areas, ordered for display.
     *
     * <p>Used by the UI to show existing areas before the operator
     * enters new data, helping to avoid accidental overlaps.</p>
     *
     * @return iterable of all existing {@link AirControlArea} records
     */
    public Iterable<AirControlArea> allAreas() {
        return repo.findAll();
    }

    /**
     * Registers a new Air Control Area.
     *
     * @param areaCode the unique code for this area (e.g. "LPPC")
     * @param name     human-readable name (e.g. "Lisbon ACC") — must be unique
     * @param minLat   minimum latitude  [-90, 90]
     * @param maxLat   maximum latitude  [-90, 90], must be &gt; minLat
     * @param minLon   minimum longitude [-180, 180]
     * @param maxLon   maximum longitude [-180, 180], must be &gt; minLon
     * @return the persisted {@link AirControlArea}
     * @throws IllegalArgumentException if the code or name already exists,
     *                                  or any value fails domain validation
     */
    public AirControlArea registerAirControlArea(final String areaCode,
                                                  final String name,
                                                  final double minLat,
                                                  final double maxLat,
                                                  final double minLon,
                                                  final double maxLon) {
        // 1. Unique code check
        final AirControlAreaCode code = new AirControlAreaCode(areaCode);
        if (repo.containsOfIdentity(code)) {
            throw new IllegalArgumentException(
                    "An air control area with code '" + areaCode + "' already exists.");
        }

        // 2. Unique name check
        if (repo.existsByName(name)) {
            throw new IllegalArgumentException(
                    "An air control area with name '" + name + "' already exists.");
        }

        // 3. Build value objects — GeographicBoundary validates ranges internally
        final GeographicBoundary boundary = new GeographicBoundary(minLat, maxLat, minLon, maxLon);
        if (repo.existsOverlappingBoundary(boundary)) {
            throw new IllegalArgumentException(
                    "The provided geographic boundary overlaps with an existing air control area.");
        }

        // 4. Create and persist
        final AirControlArea area = new AirControlArea(areaCode, name, boundary);
        return repo.save(area);
    }
}
