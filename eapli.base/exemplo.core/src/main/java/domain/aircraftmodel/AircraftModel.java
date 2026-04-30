package domain.aircraftmodel;

import domain.enginemodel.EngineModel;
import domain.enginemodel.MotorizationType;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "AIRCRAFT_MODEL",
        uniqueConstraints = @UniqueConstraint(columnNames = {"model_name", "manufacturer"}))
public class AircraftModel implements AggregateRoot<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_name")
    private String modelName;

    private String manufacturer;

    @Enumerated(EnumType.STRING)
    private AircraftType type;

    @Embedded
    private FlightCharacteristics characteristics;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "AIRCRAFT_MODEL_ENGINE",
            joinColumns = @JoinColumn(name = "aircraft_model_id"),
            inverseJoinColumns = @JoinColumn(name = "engine_model_id"))
    private List<EngineModel> certifiedEngines = new ArrayList<>();

    public AircraftModel(final String modelName,
                         final String manufacturer,
                         final AircraftType type,
                         final FlightCharacteristics characteristics,
                         final List<EngineModel> engines) {
        Preconditions.nonEmpty(modelName, "Model name cannot be empty");
        Preconditions.nonEmpty(manufacturer, "Manufacturer cannot be empty");
        Preconditions.nonNull(type, "Aircraft type cannot be null");
        Preconditions.nonNull(characteristics, "Flight characteristics cannot be null");
        Preconditions.nonNull(engines, "Engine list cannot be null");
        Preconditions.ensure(!engines.isEmpty(),
                "At least one engine model must be certified");
        validateEngineTypes(engines);

        this.modelName       = modelName;
        this.manufacturer    = manufacturer;
        this.type            = type;
        this.characteristics = characteristics;
        this.certifiedEngines = new ArrayList<>(engines);
    }

    public void addCertifiedEngine(final EngineModel engine) {
        Preconditions.nonNull(engine, "Engine model cannot be null");
        Preconditions.ensure(
                engine.motorizationType() == certifiedEngines.get(0).motorizationType(),
                "Engine motorization type must match existing certified engines");
        Preconditions.ensure(!certifiedEngines.contains(engine),
                "Engine model is already certified for this aircraft model");
        certifiedEngines.add(engine);
    }

    public void removeCertifiedEngine(final EngineModel engine) {
        Preconditions.nonNull(engine, "Engine model cannot be null");
        Preconditions.ensure(certifiedEngines.contains(engine),
                "Engine model is not certified for this aircraft model");
        Preconditions.ensure(certifiedEngines.size() > 1,
                "Aircraft model must retain at least one certified engine model");
        certifiedEngines.remove(engine);
    }

    private void validateEngineTypes(final List<EngineModel> engines) {
        final MotorizationType firstType = engines.get(0).motorizationType();
        for (final EngineModel e : engines) {
            Preconditions.ensure(e.motorizationType() == firstType,
                    "All engine models must share the same motorization type");
        }
    }

    @Override
    public Long identity() {
        return id;
    }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof AircraftModel)) return false;
        final AircraftModel that = (AircraftModel) other;
        return this.modelName.equalsIgnoreCase(that.modelName)
                && this.manufacturer.equalsIgnoreCase(that.manufacturer);
    }

    public String modelName()                         { return modelName; }
    public String manufacturer()                      { return manufacturer; }
    public AircraftType type()                        { return type; }
    public FlightCharacteristics characteristics()    { return characteristics; }
    public List<EngineModel> certifiedEngines()       { return Collections.unmodifiableList(certifiedEngines); }

    @Override
    public String toString() {
        return String.format("%s by %s (%s)", modelName, manufacturer, type);
    }

    protected AircraftModel() {
        // for ORM
    }
}