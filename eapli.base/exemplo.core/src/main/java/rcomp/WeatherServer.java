package rcomp;

import domain.AlSafePasswordPolicy;
import domain.BaseRoles;
import domain.EmailAddress;
import domain.User;
import domain.ValidEmailDomains;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import persistence.PersistenceContext;
import repositories.UserRepository;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * TCP server for US44 — Weather Person remote access.
 *
 * <p>Listens on {@link #PORT} and spawns one {@link WeatherClientHandler}
 * thread per incoming connection.  The server shares the same PostgreSQL
 * persistence unit as the rest of the system; </p>
 *
 * <p>Start this class as a standalone Java process on the DEI cloud node,
 * for example:</p>
 * <pre>
 *   java -cp &lt;fat-jar&gt; rcomp.WeatherServer
 * </pre>
 */
public class WeatherServer {

    /** Port dedicated to the Weather Person service. */
    private static final int PORT = 2226;

    private static final String PERSISTENCE_UNIT = "eapli.base.AlSafePU.postgres";

    public static void main(final String[] args) {
        System.out.println("[WEATHER-SERVER] AISafe Weather Server starting on port " + PORT);

        // Configure AuthzRegistry
        try {
            AuthzRegistry.configure(new JpaAutoTxUserRepository(PERSISTENCE_UNIT, new HashMap<>()),
                    new AlSafePasswordPolicy(), new PlainTextEncoder());
            System.out.println("[WEATHER-SERVER] AuthzRegistry configured");
        } catch (final Exception e) {
            System.err.println("[WEATHER-SERVER] AuthzRegistry error: " + e.getMessage());
        }

        // Register allowed e-mail domains
        ValidEmailDomains.register("alsafe.com");
        ValidEmailDomains.register("alsafe.pt");
        ValidEmailDomains.register("isep.ipp.pt");
        System.out.println("[WEATHER-SERVER] Email domains registered");

        // Bootstrap a default Weather Person user
        bootstrapUsers();

        // Accept TCP connections
        try (final ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[WEATHER-SERVER] Listening on port " + PORT);

            while (true) {
                final Socket client = serverSocket.accept();
                final Thread thread = new Thread(new WeatherClientHandler(client));
                thread.setDaemon(true);
                thread.start();
            }
        } catch (final IOException e) {
            System.err.println("[WEATHER-SERVER] Fatal: " + e.getMessage());
        }
    }

    // Bootstrap helpers
    private static void bootstrapUsers() {
        try {
            registerSystemUser("weather@alsafe.com", "Weather1234", "Weather", "Person", BaseRoles.WEATHER_PERSON);

            registerSystemUser("admin@alsafe.com", "Admin1234", "Admin", "User", BaseRoles.ADMIN);

            registerDomainUser("weather@alsafe.com", "Weather Person", "910000002", "STAFF-WP-001",
                    LocalDate.now().plusYears(10));

            System.out.println("[WEATHER-SERVER] Bootstrap complete");
        } catch (final Exception e) {
            System.err.println("[WEATHER-SERVER] Bootstrap error: " + e.getMessage());
        }
    }

    private static void registerSystemUser(final String email, final String password, final String first,
                                           final String last, final Role role) {
        try {
            final Set<Role> roles = new HashSet<>();
            roles.add(role);
            AuthzRegistry.userService().registerNewUser(email, password, first, last, email, roles);
            System.out.println("[WEATHER-SERVER] System user registered: " + email);
        } catch (final Exception e) {
            System.err.println("[WEATHER-SERVER] User already exists: " + email);
        }
    }

    private static void registerDomainUser(final String email, final String name, final String phone,
                                           final String staffNumber, final LocalDate clearance) {
        try {
            final UserRepository repo = PersistenceContext.getInstance().userRepository();
            final EmailAddress emailVO = EmailAddress.valueOf(email);
            if (repo.ofIdentity(emailVO).isEmpty()) {
                repo.save(new User(emailVO, name, phone, staffNumber, clearance));
                System.out.println("[WEATHER-SERVER] Domain user registered: " + email);
            }
        } catch (final Exception e) {
            System.err.println("[WEATHER-SERVER] Domain user error: " + e.getMessage());
        }
    }
}
