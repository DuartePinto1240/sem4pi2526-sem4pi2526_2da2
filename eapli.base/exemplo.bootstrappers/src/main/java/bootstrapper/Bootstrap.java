package bootstrapper;

public class Bootstrap {

    public void execute() {
        new UserBootstrapper().execute();
        new CountryBootstrapper().execute();
        new AirControlAreaBootstrapper().execute();
        new AirportBootstrapper().execute();
        new AirTransportCompanyBootstrapper().execute();
        new AircraftBootstrapper().execute();
    }
}
