package controller;

import domain.AirControlArea;
import domain.AirControlAreaCode;
import domain.WeatherData;
import dto.AirControlAreaDTO;
import dto.FlightPlanDTO;
import dto.WeatherDataDTO;
import lprog.FlightPlan;
import persistence.PersistenceContext;
import repositories.AirControlAreaRepository;
import repositories.FlightPlanRepository;
import repositories.WeatherDataRepository;

import java.util.ArrayList;
import java.util.List;

public class InsertWeatherDataController {

    private final FlightPlanRepository  flightPlanRepo;
    private final WeatherDataRepository weatherRepo;
    private final AirControlAreaRepository areaRepo;

    public InsertWeatherDataController() {
        this.flightPlanRepo = PersistenceContext.getInstance().flightPlanRepository();
        this.weatherRepo    = PersistenceContext.getInstance().weatherDataRepository();
        this.areaRepo       = PersistenceContext.getInstance().airControlAreaRepository();
    }

    // Package-private constructor for unit testing
    InsertWeatherDataController(final FlightPlanRepository flightPlanRepo,
                                final WeatherDataRepository weatherRepo,
                                final AirControlAreaRepository areaRepo) {
        this.flightPlanRepo = flightPlanRepo;
        this.weatherRepo    = weatherRepo;
        this.areaRepo       = areaRepo;
    }

    public List<FlightPlanDTO> getPilotFlightPlans(final String pilotEmail) {
        final List<FlightPlanDTO> result = new ArrayList<>();
        for (final FlightPlan p : flightPlanRepo.findByPilot(pilotEmail)) {
            if (p.isModifiable()) {
                result.add(FlightPlanDTO.from(p));
            }
        }
        return result;
    }

    public List<AirControlAreaDTO> getAvailableAreas() {
        final List<AirControlAreaDTO> result = new ArrayList<>();
        for (final AirControlArea a : areaRepo.findAll()) {
            result.add(AirControlAreaDTO.from(a));
        }
        return result;
    }

    public List<WeatherDataDTO> getAvailableWeatherData (final String areaCode) {
        List<WeatherDataDTO> availableWeatherData = new ArrayList<>();
        for (WeatherData wd : weatherRepo.findByArea(new AirControlAreaCode(areaCode))) {
            availableWeatherData.add(WeatherDataDTO.from(wd));
        }
        return availableWeatherData;
    }

    public void insertWeatherData(final Long flightPlanId, final Long weatherDataId) {
        final FlightPlan flightPlan = flightPlanRepo.ofIdentity(flightPlanId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Flight plan not found: " + flightPlanId));

        if (!flightPlan.isModifiable()) {
            throw new IllegalArgumentException("Flight plan is not modifiable.");
        }

        flightPlan.addWeatherData(weatherDataId);
        flightPlanRepo.save(flightPlan);
    }
}