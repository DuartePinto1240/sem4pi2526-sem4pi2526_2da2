package repositories;

import domain.aircraft.Aircraft;

import java.util.Optional;

public interface AircraftRepository {

    boolean existsByRegistration(String registration);

    Optional<Aircraft> ofIdentity(String registration);

    Iterable<Aircraft> findAll();

    Iterable<Aircraft> findByCompanyIata(String companyIata);

    Iterable<Aircraft> findByCompanyIataAndModelId(String companyIata, Long modelId);

    Iterable<Aircraft> findByCompanyIataAndManufacturer(String companyIata, String manufacturer);

    <S extends Aircraft> S save(S aircraft);

    boolean containsOfIdentity(String registration);

    long count();
}