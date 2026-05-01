package bootstrapper;

public class Bootstrap {

    public void execute() {
        new UserBootstrapper().execute();
        new AirControlAreaBootstrapper().execute();
        new AirTransportCompanyBootstrapper().execute();
        new AircraftBootstrapper().execute();
    }
}
