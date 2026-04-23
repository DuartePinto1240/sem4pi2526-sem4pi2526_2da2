package bootstrapper;

import controller.RegisterUserController;
import eapli.framework.actions.Action;
import java.util.Calendar;
import java.util.HashSet;

public class Bootstrap {

    public void execute() {

        new UserBootstrapper().execute();
    }
}
