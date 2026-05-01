package repositories;

import domain.aircraft.Aircraft;

import java.util.Optional;

public interface AircraftRepository {

    boolean existsByRegistration(String registration);

    Optional<Aircraft> ofIdentity(String registration);

    Iterable<Aircraft> findAll();

    Iterable<Aircraft> findByCompanyIata(String companyIata);

    <S extends Aircraft> S save(S aircraft);

    boolean containsOfIdentity(String registration);

    long count();
}