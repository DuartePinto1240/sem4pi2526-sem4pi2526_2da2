package repositories;

import domain.aircraftmodel.AircraftModel;

import java.util.Optional;

public interface AircraftModelRepository {

    boolean existsByNameAndManufacturer(String name, String manufacturer);

    Optional<AircraftModel> findByNameAndManufacturer(String name, String manufacturer);

    Iterable<AircraftModel> findAll();

    <S extends AircraftModel> S save(S aircraftModel);

    Optional<AircraftModel> ofIdentity(Long id);

    boolean containsOfIdentity(Long id);

    long count();
}