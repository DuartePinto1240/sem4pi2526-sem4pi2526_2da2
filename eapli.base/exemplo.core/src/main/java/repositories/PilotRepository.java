package repositories;

import domain.AirTransportCompany;
import domain.pilot.Pilot;
import eapli.framework.domain.repositories.DomainRepository;

import java.util.Optional;

public interface PilotRepository extends DomainRepository<Long, Pilot> {

    /**
     * Returns all active pilots belonging to the given company.
     */
    Iterable<Pilot> findActiveByCompany(AirTransportCompany company);

    /**
     * Finds a pilot by email regardless of active status.
     */
    Optional<Pilot> findByEmail(String email);

    /**
     * Returns all pilots (active and inactive) for a given company.
     */
    Iterable<Pilot> findAllByCompany(AirTransportCompany company);

    <S extends Pilot> S save(S pilot);
}