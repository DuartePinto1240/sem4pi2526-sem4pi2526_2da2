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

public class ATCCServer {

    private static final int    PORT             = 2225;
    private static final String PERSISTENCE_UNIT = "eapli.base.AlSafePU.postgres";

    public static void main(final String[] args) {
        System.out.println("[SERVER] AISafe ATCC Server starting on port " + PORT);

        // ── 1. Configurar AuthzRegistry ───────────────────────────────────
        try {
            AuthzRegistry.configure(
                    new JpaAutoTxUserRepository(PERSISTENCE_UNIT, new HashMap<>()),
                    new AlSafePasswordPolicy(),
                    new PlainTextEncoder()
            );
            System.out.println("[SERVER] AuthzRegistry configured");
        } catch (final Exception e) {
            System.err.println("[SERVER] AuthzRegistry: " + e.getMessage());
        }

        // ── 2. Registar domínios de email ─────────────────────────────────
        ValidEmailDomains.register("alsafe.com");
        ValidEmailDomains.register("alsafe.pt");
        ValidEmailDomains.register("isep.ipp.pt");
        System.out.println("[SERVER] Email domains registered");

        // ── 3. Bootstrap utilizadores ─────────────────────────────────────
        bootstrapUsers();

        // ── 4. Teste direto de autenticação ───────────────────────────────
        System.out.println("[TEST] Testing authenticate directly...");
        try {
            final Optional<eapli.framework.infrastructure.authz.application.UserSession>
                    testSession = AuthzRegistry.authenticationService()
                    .authenticate("atcc@alsafe.com", "Atcc1234");
            System.out.println("[TEST] Result: " + testSession.isPresent());
        } catch (final Exception e) {
            System.err.println("[TEST] Error: " + e.getMessage());
            e.printStackTrace();
        }

        // ── 5. Aceitar ligações TCP ───────────────────────────────────────
        try (final ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] Listening on port " + PORT);

            while (true) {
                final Socket client = serverSocket.accept();
                final Thread thread = new Thread(new ATCCClientHandler(client));
                thread.setDaemon(true);
                thread.start();
            }
        } catch (final IOException e) {
            System.err.println("[SERVER] Fatal error: " + e.getMessage());
        }
    }

    private static void bootstrapUsers() {
        try {
            registerSystemUser(
                    "atcc@alsafe.com", "Atcc1234",
                    "ATCC", "Collaborator",
                    BaseRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

            registerSystemUser(
                    "admin@alsafe.com", "Admin1234",
                    "Admin", "User",
                    BaseRoles.ADMIN);

            registerDomainUser(
                    "atcc@alsafe.com", "ATCC Collaborator",
                    "910000001", "STAFF-ATCC-001",
                    LocalDate.now().plusYears(10));

            registerDomainUser(
                    "admin@alsafe.com", "Admin User",
                    "910000000", "STAFF-ADMIN-001",
                    LocalDate.now().plusYears(10));

            System.out.println("[SERVER] Bootstrap users complete");
        } catch (final Exception e) {
            System.err.println("[SERVER] Bootstrap error: " + e.getMessage());
        }
    }

    private static void registerSystemUser(final String email,
                                           final String password,
                                           final String first,
                                           final String last,
                                           final Role role) {
        try {
            final Set<Role> roles = new HashSet<>();
            roles.add(role);
            AuthzRegistry.userService()
                    .registerNewUser(email, password, first, last, email, roles);
            System.out.println("[SERVER] System user registered: " + email);
        } catch (final Exception e) {
            System.err.println("[SERVER] User already exists: " + email);
        }
    }

    private static void registerDomainUser(final String email,
                                           final String name,
                                           final String phone,
                                           final String staffNumber,
                                           final LocalDate clearance) {
        try {
            final UserRepository repo =
                    PersistenceContext.getInstance().userRepository();
            final EmailAddress emailVO = EmailAddress.valueOf(email);
            if (repo.ofIdentity(emailVO).isEmpty()) {
                repo.save(new User(emailVO, name, phone, staffNumber, clearance));
                System.out.println("[SERVER] Domain user registered: " + email);
            }
        } catch (final Exception e) {
            System.err.println("[SERVER] Domain user error: " + e.getMessage());
        }
    }
}