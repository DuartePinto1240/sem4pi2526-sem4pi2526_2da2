package bootstrapper; // Ou o package que o teu projeto usar para bootstraps

import controller.RegisterUserController;
import eapli.framework.actions.Action;
import java.util.Calendar;
import java.util.HashSet;

public class UserBootstrapper implements Action {

    @Override
    public boolean execute() {
        final RegisterUserController controller = new RegisterUserController();

        try {
            controller.registerUser("admin@alsafe.pt", "Administrador AlSafe", "912345678", new HashSet<>(), Calendar.getInstance());
            controller.registerUser("manager@alsafe.pt", "Gestor de Frota", "911111111", new HashSet<>(), Calendar.getInstance());

            return true;
        } catch (Exception e) {
            System.out.println("Erro no Bootstrap de Utilizadores: " + e.getMessage());
            return false;
        }
    }
}