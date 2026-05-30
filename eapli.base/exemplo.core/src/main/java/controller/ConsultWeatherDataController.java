package controller;

import application.WeatherDataQueryService;
import domain.AirControlArea;
import domain.BaseRoles;
import dto.WeatherDataDTO;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import persistence.PersistenceContext;

import java.time.LocalDate;
import java.util.List;

/**
 * Thin controller for US043 — Consult Weather Data.
 *
 * <p>Responsibilities: enforce authorization, delegate to
 * {@link WeatherDataQueryService}. No business logic here.</p>
 */
public class ConsultWeatherDataController {

    private final AuthorizationService    authz;
    private final WeatherDataQueryService svc;

    public ConsultWeatherDataController() {
        this.authz = AuthzRegistry.authorizationService();
        this.svc   = new WeatherDataQueryService(
                PersistenceContext.getInstance().airControlAreaRepository(),
                PersistenceContext.getInstance().weatherDataRepository());
    }

    ConsultWeatherDataController(final AuthorizationService authz, final WeatherDataQueryService svc) {
        this.authz = authz;
        this.svc   = svc;
    }

    /**
     * Returns all registered air control areas for the user to choose from.
     *
     * @return iterable of {@link AirControlArea}
     */
    public Iterable<AirControlArea> availableAreas() {
        authz.ensureAuthenticatedUserHasAnyOf(BaseRoles.WEATHER_PERSON);
        return svc.allAreas();
    }

    /**
     * Returns weather data for the given area that covers the given date.
     *
     * @param areaCode string area code
     * @param date     the day to filter by
     * @return list of {@link WeatherDataDTO}
     * @throws IllegalArgumentException if area does not exist
     */
    public List<WeatherDataDTO> weatherDataByAreaAndDate(final String areaCode, final LocalDate date) {
        authz.ensureAuthenticatedUserHasAnyOf(BaseRoles.WEATHER_PERSON);
        return svc.weatherDataByAreaAndDate(areaCode, date);
    }
}
