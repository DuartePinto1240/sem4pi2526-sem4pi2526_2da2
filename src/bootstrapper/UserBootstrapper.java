package bootstrapper;

import controller.RegisterUserController;
import domain.User;
import eapli.framework.actions.Action;

/**
 * Bootstrapper que cria a conta de administrador padrão do sistema.
 *
 * Credenciais do Admin:
 *   Email:    admin@alsafe.com
 *   Password: Admin1234
 *   Staff Nº: STAFF-ADMIN-001
 */
public class UserBootstrapper implements Action {

    // Credenciais do administrador padrão — alterar conforme necessário
    private static final String ADMIN_EMAIL    = "admin@alsafe.com";
    private static final String ADMIN_PASSWORD = "Admin1234";
    private static final String ADMIN_NAME     = "Administrador AlSafe";
    private static final String ADMIN_PHONE    = "912000000";
    private static final String ADMIN_STAFF    = "STAFF-ADMIN-001";

    @Override
    public boolean execute() {
        try {
            final RegisterUserController controller = new RegisterUserController();
            User admin = controller.registerAdmin(
                    ADMIN_EMAIL, ADMIN_NAME, ADMIN_PHONE, ADMIN_PASSWORD, ADMIN_STAFF);

            System.out.println(">> Bootstrap: conta admin criada/confirmada.");
            System.out.println("   Email:    " + ADMIN_EMAIL);
            System.out.println("   Password: " + ADMIN_PASSWORD);
            System.out.println("   Staff Nº: " + ADMIN_STAFF);
            return true;
        } catch (Exception e) {
            System.err.println(">> Bootstrap ERRO: " + e.getMessage());
            return false;
        }
    }
}