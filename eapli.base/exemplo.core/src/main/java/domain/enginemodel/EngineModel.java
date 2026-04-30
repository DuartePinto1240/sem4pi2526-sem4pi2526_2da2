package domain.enginemodel;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.*;

@Entity
@Table(name = "ENGINE_MODEL",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "manufacturer"}))
public class EngineModel implements AggregateRoot<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String manufacturer;

    @Enumerated(EnumType.STRING)
    private MotorizationType motorizationType;

    @Embedded
    private Thrust thrust;

    @Embedded
    private TSFC tsfc;

    public EngineModel(final String name,
                       final String manufacturer,
                       final MotorizationType motorizationType,
                       final Thrust thrust,
                       final TSFC tsfc) {
        Preconditions.nonEmpty(name, "Engine model name cannot be empty");
        Preconditions.nonEmpty(manufacturer, "Manufacturer cannot be empty");
        Preconditions.nonNull(motorizationType, "Motorization type cannot be null");
        Preconditions.nonNull(thrust, "Thrust cannot be null");
        Preconditions.nonNull(tsfc, "TSFC cannot be null");

        this.name = name;
        this.manufacturer = manufacturer;
        this.motorizationType = motorizationType;
        this.thrust = thrust;
        this.tsfc = tsfc;
    }

    @Override
    public Long identity() {
        return id;
    }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof EngineModel)) return false;
        final EngineModel that = (EngineModel) other;
        return this.name.equalsIgnoreCase(that.name)
                && this.manufacturer.equalsIgnoreCase(that.manufacturer);
    }

    public String name() {
        return name;
    }

    public String manufacturer() {
        return manufacturer;
    }

    public MotorizationType motorizationType() {
        return motorizationType;
    }

    public Thrust thrust() {
        return thrust;
    }

    public TSFC tsfc() {
        return tsfc;
    }

    @Override
    public String toString() {
        return String.format("%s by %s (%s)", name, manufacturer, motorizationType);
    }

    protected EngineModel() {
        // for ORM
    }
}