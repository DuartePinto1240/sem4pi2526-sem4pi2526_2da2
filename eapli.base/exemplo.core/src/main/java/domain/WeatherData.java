package domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.*;

/**
 * Aggregate Root representing a weather data record in the AISafe system (US041).
 *
 * <p>A weather data record captures wind conditions observed in a specific
 * rectangular sub-area ({@link WeatherSubArea}) of an air control area, over
 * a defined time period ({@link ObservationPeriod}).</p>
 *
 */
@Entity
@Table(name = "WEATHER_DATA")
public class WeatherData implements AggregateRoot<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /**
     * Reference to the air control area by identity (code only).
     * Preserves aggregate boundary — no FK join to AirControlArea.
     */
    @Embedded
    private AirControlAreaCode areaCode;

    /** The rectangular sub-area within the air control area. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "subBoundary.minLat", column = @Column(name = "minLat")),
            @AttributeOverride(name = "subBoundary.maxLat", column = @Column(name = "maxLat")),
            @AttributeOverride(name = "subBoundary.minLon", column = @Column(name = "minLon")),
            @AttributeOverride(name = "subBoundary.maxLon", column = @Column(name = "maxLon"))
    })
    private WeatherSubArea subArea;

    /** The time period during which this observation is valid. */
    @Embedded
    private ObservationPeriod period;

    /** Wind direction and speed at the time of observation. */
    @Embedded
    private WindCondition wind;

    /**
     * Creates a new WeatherData record.
     *
     * @param areaCode reference to the air control area (by code)
     * @param subArea  rectangular sub-area within the air control area
     * @param period   observation time period (end must be after start)
     * @param wind     wind direction and speed
     * @throws IllegalArgumentException if any argument is null
     */
    public WeatherData(final AirControlAreaCode areaCode,
                       final WeatherSubArea subArea,
                       final ObservationPeriod period,
                       final WindCondition wind) {
        Preconditions.nonNull(areaCode, "Area code must not be null");
        Preconditions.nonNull(subArea,  "Sub-area must not be null");
        Preconditions.nonNull(period,   "Observation period must not be null");
        Preconditions.nonNull(wind,     "Wind condition must not be null");
        this.areaCode = areaCode;
        this.subArea  = subArea;
        this.period   = period;
        this.wind     = wind;
    }

    @Override
    public Long identity() {
        return id;
    }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof WeatherData)) return false;
        final WeatherData that = (WeatherData) other;
        return this.areaCode.equals(that.areaCode)
                && this.subArea.equals(that.subArea)
                && this.period.equals(that.period);
    }

    public AirControlAreaCode areaCode() { return areaCode; }
    public WeatherSubArea subArea()       { return subArea;  }
    public ObservationPeriod period()     { return period;   }
    public WindCondition wind()           { return wind;     }

    @Override
    public String toString() {
        return String.format("WeatherData[id=%s, area=%s, subArea=%s, period=%s, wind=%s]",
                id, areaCode, subArea, period, wind);
    }

    /** No-arg constructor required by JPA. */
    protected WeatherData() {
    }
}
