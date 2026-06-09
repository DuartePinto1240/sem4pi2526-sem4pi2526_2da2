package rcomp;

public final class Protocol {

    // Shared responses
    public static final String OK                = "OK";
    public static final String ERROR             = "ERROR";
    public static final String END               = "END";
    public static final String BYE               = "BYE";

    // Shared Requests
    public static final String LOGIN             = "LOGIN";
    public static final String LOGOUT            = "LOGOUT";

    // ATCC commands (US78)
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

    // Weather Person commands (US44)
    public static final String LIST_AREAS       = "LIST_AREAS";
    public static final String REGISTER_WEATHER = "REGISTER_WEATHER";
    public static final String IMPORT_WEATHER   = "IMPORT_WEATHER";
    public static final String END_FILE         = "END_FILE";
    public static final String CONSULT_WEATHER  = "CONSULT_WEATHER";

    // Pilot commands (US86)
    public static final String LIST_DRAFT_PLANS   = "LIST_DRAFT_PLANS";
    public static final String LIST_ALL_PLANS     = "LIST_ALL_PLANS";
    public static final String IMPORT_FLIGHT_PLAN = "IMPORT_FLIGHT_PLAN";
    public static final String LIST_WEATHER_AREAS = "LIST_WEATHER_AREAS";
    public static final String LIST_WEATHER_DATA  = "LIST_WEATHER_DATA";
    public static final String INSERT_WEATHER     = "INSERT_WEATHER";
    public static final String TEST_FLIGHT_PLAN   = "TEST_FLIGHT_PLAN";

    private Protocol() {}
}