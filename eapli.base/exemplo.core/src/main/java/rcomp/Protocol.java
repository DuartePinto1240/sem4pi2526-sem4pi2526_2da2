package rcomp;

public final class Protocol {

    // ── Requests ──────────────────────────────────────────────────────────
    public static final String LOGIN             = "LOGIN";
    public static final String LOGOUT            = "LOGOUT";
    public static final String ADD_AIRCRAFT      = "ADD_AIRCRAFT";
    public static final String LIST_FLEET        = "LIST_FLEET";
    public static final String LIST_FLEET_MODEL  = "LIST_FLEET_MODEL";
    public static final String LIST_FLEET_MAKER  = "LIST_FLEET_MAKER";
    public static final String LIST_FLEET_CAP    = "LIST_FLEET_CAP";
    public static final String LIST_FLEET_AGE    = "LIST_FLEET_AGE";
    public static final String DECOMMISSION      = "DECOMMISSION";
    public static final String CREATE_ROUTE      = "CREATE_ROUTE";
    public static final String LIST_AIRPORTS     = "LIST_AIRPORTS";
    public static final String LIST_MODELS       = "LIST_MODELS";

    // ── Responses ─────────────────────────────────────────────────────────
    public static final String OK                = "OK";
    public static final String ERROR             = "ERROR";
    public static final String END               = "END";
    public static final String BYE               = "BYE";

    private Protocol() {}
}