package repositories;

import domain.enginemodel.EngineModel;
import domain.enginemodel.MotorizationType;

import java.util.Optional;

public interface EngineModelRepository {

    boolean existsByNameAndManufacturer(String name, String manufacturer);

    Optional<EngineModel> findByNameAndManufacturer(String name, String manufacturer);

    Iterable<EngineModel> findAll();

    Iterable<EngineModel> findByMotorizationType(MotorizationType type);

    <S extends EngineModel> S save(S engineModel);

    Optional<EngineModel> ofIdentity(Long id);

    boolean containsOfIdentity(Long id);

    long count();
}