package domain.aircraftmodel;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class FlightCharacteristics implements ValueObject {

    @Column(name = "empty_weight")
    private double emptyWeight;

    @Column(name = "mzfw")
    private double mzfw;

    @Column(name = "mtow")
    private double mtow;

    @Column(name = "max_fuel_capacity")
    private double maxFuelCapacity;

    @Column(name = "service_ceiling")
    private double serviceCeiling;

    @Column(name = "cruise_speed")
    private double cruiseSpeed;

    @Column(name = "maximum_range")
    private double maximumRange;

    @Column(name = "wing_area")
    private double wingArea;

    @Column(name = "drag_coefficient")
    private double dragCoefficient;

    @Column(name = "lift_coefficient")
    private double liftCoefficient;

    public FlightCharacteristics(final double emptyWeight,
                                 final double mzfw,
                                 final double mtow,
                                 final double maxFuelCapacity,
                                 final double serviceCeiling,
                                 final double cruiseSpeed,
                                 final double maximumRange,
                                 final double wingArea,
                                 final double dragCoefficient,
                                 final double liftCoefficient) {
        Preconditions.ensure(emptyWeight > 0,
                "Empty weight must be positive");
        Preconditions.ensure(mzfw > 0,
                "MZFW must be positive");
        Preconditions.ensure(mtow > 0,
                "MTOW must be positive");
        Preconditions.ensure(emptyWeight < mzfw,
                "Empty weight must be less than MZFW");
        Preconditions.ensure(mzfw < mtow,
                "MZFW must be less than MTOW");
        Preconditions.ensure(maxFuelCapacity > 0,
                "Maximum fuel capacity must be positive");
        Preconditions.ensure(serviceCeiling > 0,
                "Service ceiling must be positive");
        Preconditions.ensure(cruiseSpeed > 0,
                "Cruise speed must be positive");
        Preconditions.ensure(maximumRange > 0,
                "Maximum range must be positive");
        Preconditions.ensure(wingArea > 0,
                "Wing area must be positive");
        Preconditions.ensure(dragCoefficient > 0,
                "Drag coefficient must be positive");
        Preconditions.ensure(liftCoefficient > 0,
                "Lift coefficient must be positive");

        this.emptyWeight     = emptyWeight;
        this.mzfw            = mzfw;
        this.mtow            = mtow;
        this.maxFuelCapacity = maxFuelCapacity;
        this.serviceCeiling  = serviceCeiling;
        this.cruiseSpeed     = cruiseSpeed;
        this.maximumRange    = maximumRange;
        this.wingArea        = wingArea;
        this.dragCoefficient = dragCoefficient;
        this.liftCoefficient = liftCoefficient;
    }

    public double emptyWeight()     { return emptyWeight; }
    public double mzfw()            { return mzfw; }
    public double mtow()            { return mtow; }
    public double maxFuelCapacity() { return maxFuelCapacity; }
    public double serviceCeiling()  { return serviceCeiling; }
    public double cruiseSpeed()     { return cruiseSpeed; }
    public double maximumRange()    { return maximumRange; }
    public double wingArea()        { return wingArea; }
    public double dragCoefficient() { return dragCoefficient; }
    public double liftCoefficient() { return liftCoefficient; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof FlightCharacteristics)) return false;
        final FlightCharacteristics that = (FlightCharacteristics) o;
        return Double.compare(that.emptyWeight,     emptyWeight)     == 0
                && Double.compare(that.mzfw,            mzfw)            == 0
                && Double.compare(that.mtow,            mtow)            == 0
                && Double.compare(that.maxFuelCapacity, maxFuelCapacity) == 0
                && Double.compare(that.serviceCeiling,  serviceCeiling)  == 0
                && Double.compare(that.cruiseSpeed,     cruiseSpeed)     == 0
                && Double.compare(that.maximumRange,    maximumRange)    == 0
                && Double.compare(that.wingArea,        wingArea)        == 0
                && Double.compare(that.dragCoefficient, dragCoefficient) == 0
                && Double.compare(that.liftCoefficient, liftCoefficient) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(emptyWeight, mzfw, mtow, maxFuelCapacity,
                serviceCeiling, cruiseSpeed, maximumRange,
                wingArea, dragCoefficient, liftCoefficient);
    }

    @Override
    public String toString() {
        return String.format(
                "FlightCharacteristics[emptyWeight=%.1f, mzfw=%.1f, mtow=%.1f, " +
                        "maxFuel=%.1f, ceiling=%.1f, speed=%.1f, range=%.1f, " +
                        "wingArea=%.2f, Cd=%.4f, Cl=%.4f]",
                emptyWeight, mzfw, mtow, maxFuelCapacity,
                serviceCeiling, cruiseSpeed, maximumRange,
                wingArea, dragCoefficient, liftCoefficient);
    }

    protected FlightCharacteristics() {
        // for ORM
    }
}