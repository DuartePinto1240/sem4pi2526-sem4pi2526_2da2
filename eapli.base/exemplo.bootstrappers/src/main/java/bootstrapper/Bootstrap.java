package bootstrapper;

/**
 * Application bootstrap coordinator.
 *
 * <p>Executes all registered bootstrap actions in order during
 * application startup. Each bootstrapper ensures the system has
 * the minimum data required to operate correctly.</p>
 */
public class Bootstrap {

    /**
     * Runs all bootstrap actions sequentially.
     */
    public void execute() {
        new UserBootstrapper().execute();
        new AirControlAreaBootstrapper().execute();
        new AirTransportCompanyBootstrapper().execute();
    }
}
