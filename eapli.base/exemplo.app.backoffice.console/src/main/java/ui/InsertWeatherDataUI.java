package ui;

import controller.InsertWeatherDataController;
import dto.AirControlAreaDTO;
import dto.FlightPlanDTO;
import dto.WeatherDataDTO;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.List;

/**
 * UI for US082 – Insert Weather Data in a Flight Plan.
 */
public class InsertWeatherDataUI extends AbstractUI {

    final InsertWeatherDataController ctrl;

    public InsertWeatherDataUI() {
        this.ctrl = new InsertWeatherDataController();
    }

    @Override
    protected boolean doShow() {

        System.out.println("\n--- Insert Weather Data in Flight Plan ---");

        // list the pilot's modifiable flight plans
        final List<FlightPlanDTO> plans = getPilotFlightPlans();
        if (plans.isEmpty()) {
            System.out.println(">> No modifiable flight plans found.");
            return false;
        }

        System.out.println("\nYour modifiable flight plans:");
        for (int i = 0; i < plans.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, plans.get(i));
        }

        final int planIdx = Console.readInteger("Select flight plan:") - 1;
        if (planIdx < 0 || planIdx >= plans.size()) {
            System.out.println(">> Invalid selection.");
            return false;
        }
        final FlightPlanDTO selectedPlan = plans.get(planIdx);

        // list all available air control areas
        final List<AirControlAreaDTO> areas = ctrl.getAvailableAreas();
        if (areas.isEmpty()) {
            System.out.println(">> No air control areas found.");
            return false;
        }

        System.out.println("\nAvailable air control areas:");
        for (int i = 0; i < areas.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, areas.get(i));
        }

        final int areaIdx = Console.readInteger("Select area:") - 1;
        if (areaIdx < 0 || areaIdx >= areas.size()) {
            System.out.println(">> Invalid selection.");
            return false;
        }
        final AirControlAreaDTO selectedArea = areas.get(areaIdx);

        // list available weather data for the selected area
        final List<WeatherDataDTO> weatherList =
                getAvailableWeatherData(selectedArea.areaCode);
        if (weatherList.isEmpty()) {
            System.out.println(">> No weather data found for area '"
                    + selectedArea.areaCode + "'.");
            return false;
        }

        System.out.println("\nAvailable weather data:");
        for (int i = 0; i < weatherList.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, weatherList.get(i));
        }

        final int weatherIdx = Console.readInteger("Select weather data:") - 1;
        if (weatherIdx < 0 || weatherIdx >= weatherList.size()) {
            System.out.println(">> Invalid selection.");
            return false;
        }
        final WeatherDataDTO selectedWeather = weatherList.get(weatherIdx);

        // confirm before inserting
        System.out.printf("%nAdd weather data '%s' from area '%s' to flight plan '%s'? (y/n): ",
                selectedWeather, selectedArea.name, selectedPlan.flightId);
        final String confirm = Console.readLine("");
        if (!confirm.trim().equalsIgnoreCase("y")) {
            System.out.println(">> Operation cancelled.");
            return false;
        }

        // insert the weather data into the flight plan
        insertWeatherData(selectedPlan.id, selectedWeather.id);

        return false;
    }

    private List<FlightPlanDTO> getPilotFlightPlans () {
        final String email = AuthzRegistry.authorizationService()
                .session()
                .map(s -> s.authenticatedUser().username().toString())
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));
        return ctrl.getPilotFlightPlans(email);
    }

    private List<WeatherDataDTO> getAvailableWeatherData (String areaCode) {
        return ctrl.getAvailableWeatherData(areaCode);
    }

    private void insertWeatherData (Long flightPlanId, Long weatherDataId) {
        try {
            ctrl.insertWeatherData(flightPlanId, weatherDataId);
            System.out.println("\n>> Weather data added successfully.");
        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }
    }

    @Override
    public String headline() {
        return "US082 - Insert Weather Data in a Flight Plan";
    }
}