package rcomp;

import domain.AlSafePasswordPolicy;
import domain.BaseRoles;
import domain.ValidEmailDomains;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * TCP server for US86 — Pilot remote access.
 *
 * <p>Listens on port 2228 and spawns one {@link PilotClientHandler} thread
 * per incoming connection. Shares the same PostgreSQL persistence unit as the
 * rest of the system.</p>
 *
 * <pre>
 *   java -cp &lt;fat-jar&gt; rcomp.PilotServer
 * </pre>
 */
public class PilotServer {

    /** Port dedicated to the Pilot service. */
    static final int PORT = 2228;

    private static final String PERSISTENCE_UNIT = "eapli.base.AlSafePU.postgres";

    public static void main(final String[] args) {
        System.out.println("[PILOT-SERVER] AlSafe Pilot Server starting on port " + PORT);

        // Configure AuthzRegistry
        try {
            AuthzRegistry.configure(
                    new JpaAutoTxUserRepository(PERSISTENCE_UNIT, new HashMap<>()),
                    new AlSafePasswordPolicy(),
                    new PlainTextEncoder());
            System.out.println("[PILOT-SERVER] AuthzRegistry configured");
        } catch (final Exception e) {
            System.err.println("[PILOT-SERVER] AuthzRegistry error: " + e.getMessage());
        }

        ValidEmailDomains.register("alsafe.com");
        ValidEmailDomains.register("alsafe.pt");
        ValidEmailDomains.register("isep.ipp.pt");

        bootstrapUsers();

        try (final ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[PILOT-SERVER] Listening on port " + PORT);
            while (true) {
                final Socket client = serverSocket.accept();
                final Thread t = new Thread(new PilotClientHandler(client));
                t.setDaemon(true);
                t.start();
            }
        } catch (final IOException e) {
            System.err.println("[PILOT-SERVER] Fatal: " + e.getMessage());
        }
    }

    private static void bootstrapUsers() {
        try {
            registerSystemUser("pilot@alsafe.com", "Pilot1234", "Pilot", "User", BaseRoles.PILOT);
            System.out.println("[PILOT-SERVER] Bootstrap complete");
        } catch (final Exception e) {
            System.err.println("[PILOT-SERVER] Bootstrap error: " + e.getMessage());
        }
    }

    private static void registerSystemUser(final String email, final String password,
                                           final String first, final String last,
                                           final Role role) {
        try {
            final Set<Role> roles = new HashSet<>();
            roles.add(role);
            AuthzRegistry.userService().registerNewUser(email, password, first, last, email, roles);
            System.out.println("[PILOT-SERVER] System user registered: " + email);
        } catch (final Exception e) {
            System.err.println("[PILOT-SERVER] User already exists: " + email);
        }
    }
}
