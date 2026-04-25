package bootstrapper;

/**
 * Ponto de entrada do processo de bootstrap do sistema AlSafe.
 * Inicializa os dados base necessários para o funcionamento do sistema.
 */
public class Bootstrap {

    public void execute() {
        System.out.println(">> A inicializar dados de bootstrap...");
        new UserBootstrapper().execute();
        new AirTransportCompanyBootstrapper().execute();
        System.out.println(">> Bootstrap concluído.");
    }
}