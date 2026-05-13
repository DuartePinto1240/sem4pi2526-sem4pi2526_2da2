package bootstrapper;

import controller.RegisterAirControlAreaController;
import eapli.framework.actions.Action;

/**
 * Bootstrap action that pre-loads air control areas (US050).
 *
 * <p>Areas are sized to cover all airports pre-loaded by
 * {@link AirportBootstrapper}. Must run before that bootstrapper.</p>
 *
 * <p>Areas that already exist are silently skipped.</p>
 */
public class AirControlAreaBootstrapper implements Action {

    @Override
    public boolean execute() {
        final RegisterAirControlAreaController ctrl =
                new RegisterAirControlAreaController();

        // Europe

        // Portugal mainland — OPO, LIS
        register(ctrl, "LPPC", "Lisbon ACC",
                36.0, 42.2, -10.0, -6.0);

        // Portugal Azores — PDL
        register(ctrl, "LPPO", "Santa Maria ACC",
                27.0, 43.0, -35.0, -15.0);

        // Portugal Madeira — FNC
        register(ctrl, "LPMA", "Madeira ACC",
                30.0, 35.0, -19.0, -14.0);

        // Spain — MAD, PMI
        register(ctrl, "LECM", "Madrid ACC",
                36.0, 44.0, -9.5, 4.5);

        // Spain Canary Islands — LPA
        register(ctrl, "GCCC", "Canarias ACC",
                25.0, 30.5, -18.5, -13.0);

        // France — CDG
        register(ctrl, "LFFF", "Paris ACC",
                43.0, 51.5, -5.0, 8.5);

        // Germany — FRA, BER
        register(ctrl, "EDGG", "Langen ACC",
                47.0, 55.5, 6.0, 15.5);

        // United Kingdom — LHR, GLA
        register(ctrl, "EGTT", "London ACC",
                49.0, 61.5, -8.5, 2.5);

        // Ireland — SNN
        register(ctrl, "EISN", "Shannon ACC",
                51.0, 55.5, -11.0, -6.0);

        // Iceland — KEF
        register(ctrl, "BIRD", "Reykjavik ACC",
                62.0, 67.0, -25.0, -18.0);

        // Norway — TRD
        register(ctrl, "ENBD", "Oslo ACC",
                57.0, 65.5, 4.5, 15.0);

        // Netherlands — AMS
        register(ctrl, "EHAA", "Amsterdam ACC",
                50.5, 56.0, 2.5, 8.0);

        // Switzerland — ZRH
        register(ctrl, "LSAS", "Swiss ACC",
                45.5, 48.5, 5.5, 11.0);

        // Italy — MXP, FCO
        register(ctrl, "LIRR", "Roma ACC",
                36.5, 47.5, 6.5, 19.0);

        // Americas

        // USA East — JFK, IAD, MIA, EWR, BOS, ATL
        register(ctrl, "KZNY", "New York ARTCC",
                24.0, 48.0, -82.0, -66.0);

        // USA Central — ORD, DFW, MCI
        register(ctrl, "KZAU", "Chicago ARTCC",
                29.0, 49.0, -104.0, -82.0);

        // USA West — LAX, SFO, LAS, DEN
        register(ctrl, "KZLA", "Los Angeles ARTCC",
                29.0, 49.5, -125.0, -100.0);

        // USA Hawaii — HNL
        register(ctrl, "KZHN", "Honolulu ARTCC",
                18.0, 25.0, -162.0, -154.0);

        // Canada — YYC, YUL
        register(ctrl, "CZEG", "Edmonton ACC",
                43.0, 60.0, -120.0, -60.0);

        // Mexico — MEX
        register(ctrl, "MMFR", "Mexico City ACC",
                14.0, 24.0, -107.0, -86.0);

        // Colombia — BOG
        register(ctrl, "SKED", "Bogota ACC",
                -5.0, 13.0, -80.0, -65.0);

        // Brazil — CIG (GIG), REC
        register(ctrl, "SBBS", "Brasilia ACC",
                -33.0, -3.0, -53.0, -33.0);

        // Africa

        // Russia — SVO
        register(ctrl, "UURR", "Moscow ACC",
                53.0, 60.0, 30.0, 45.0);

        // Kazakhstan — ALA
        register(ctrl, "UAAA", "Almaty ACC",
                40.0, 48.0, 70.0, 85.0);

        // South Africa — JNB
        register(ctrl, "FAJA", "Johannesburg ACC",
                -30.0, -22.0, 24.0, 33.0);

        // DR Congo — FIH
        register(ctrl, "FZZA", "Kinshasa ACC",
                -8.0, 3.0, 11.0, 20.0);

        // Mozambique — MPM
        register(ctrl, "FQMA", "Maputo ACC",
                -27.0, -20.0, 30.0, 36.0);

        // Asia & Oceania

        // Japan — HND
        register(ctrl, "RJJJ", "Tokyo ACC",
                30.0, 41.0, 129.0, 146.0);

        // Hong Kong — HKG
        register(ctrl, "VHHK", "Hong Kong ACC",
                20.0, 25.5, 111.0, 117.0);

        // China North — PEK
        register(ctrl, "ZBPE", "Beijing ACC",
                37.0, 43.0, 112.0, 122.0);

        // China Southwest — CTU, LXA
        register(ctrl, "ZUGH", "Chengdu ACC",
                26.0, 34.0, 90.0, 108.0);

        // Singapore — SIN
        register(ctrl, "WSJC", "Singapore ACC",
                -1.0, 5.0, 100.0, 108.0);

        // Australia East — SYD
        register(ctrl, "YMMM", "Melbourne ACC",
                -40.0, -28.0, 140.0, 155.0);

        // Australia Central — ASP
        register(ctrl, "YTCC", "Brisbane ACC",
                -26.0, -20.0, 128.0, 138.0);

        return true;
    }

    private void register(final RegisterAirControlAreaController ctrl,
                          final String code, final String name,
                          final double minLat, final double maxLat,
                          final double minLon, final double maxLon) {
        try {
            ctrl.registerAirControlArea(code, name, minLat, maxLat, minLon, maxLon);
        } catch (final Exception e) {
            // Already exists or validation failed — skip silently
        }
    }
}
