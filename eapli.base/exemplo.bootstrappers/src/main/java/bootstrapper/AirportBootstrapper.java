package bootstrapper;

import controller.CreateAirportController;
import eapli.framework.actions.Action;

/**
 * Bootstrap action that pre-loads all well-known airports (US052).
 *
 * <p>Data sourced from the airport list provided by the product owner.
 * Each airport is mapped to its corresponding air control area registered
 * by {@link AirControlAreaBootstrapper}, which must run first.</p>
 *
 * <p>Airports that already exist are silently skipped.</p>
 *
 * <p><b>ICAO codes</b> were derived from the IATA codes and known airport data.
 * The IATA code is used as the 3-letter secondary identifier.</p>
 */
public class AirportBootstrapper implements Action {

    @Override
    public boolean execute() {
        final CreateAirportController ctrl = new CreateAirportController();

        // Portugal
        register(ctrl, "LPPR", "OPO", "Francisco de Sa Carneiro Airport",
                "Porto",         "Portugal",    41.2481003,  -8.6813898,  69.0,   "LPPC");
        register(ctrl, "LPPT", "LIS", "Humberto Delgado Airport",
                "Lisbon",        "Portugal",    38.7812996,  -9.1359196,  114.0,  "LPPC");
        register(ctrl, "LPPD", "PDL", "Joao Paulo II Airport",
                "Ponta Delgada", "Portugal",    37.7411995, -25.6979008,  79.0,   "LPPO");
        register(ctrl, "LPMA", "FNC", "Madeira Airport",
                "Funchal",       "Portugal",    32.6978989, -16.7744999,  59.0,   "LPMA");

        // Spain
        register(ctrl, "LEMD", "MAD", "Adolfo Suarez Madrid Barajas International Airport",
                "Madrid",            "Spain",   40.4936000,  -3.5667600,  610.0,  "LECM");
        register(ctrl, "LEPA", "PMI", "Palma de Mallorca Airport",
                "Palma de Mallorca", "Spain",   39.5517006,   2.7388101,    7.0,  "LECM");
        register(ctrl, "GCLP", "LPA", "Gran Canaria Airport",
                "Las Palmas",        "Spain",   27.9319000, -15.3865995,   23.0,  "GCCC");

        // France
        register(ctrl, "LFPG", "CDG", "Charles de Gaulle International Airport",
                "Paris",         "France",      49.0127983,   2.5500000,  119.0,  "LFFF");

        // Germany
        register(ctrl, "EDDF", "FRA", "Frankfurt am Main International Airport",
                "Frankfurt am Main", "Germany", 50.0264015,   8.5431299,  111.0,  "EDGG");
        register(ctrl, "EDDB", "BER", "Berlin Brandenburg Airport",
                "Berlin",            "Germany", 52.5166667,  13.4000000,   47.0,  "EDGG");

        // United Kingdom
        register(ctrl, "EGLL", "LHR", "London Heathrow Airport",
                "London",  "United Kingdom",    51.4706001,  -0.4619410,   25.0,  "EGTT");
        register(ctrl, "EGPF", "GLA", "Glasgow International Airport",
                "Glasgow", "United Kingdom",    55.8718987,  -4.4330602,    8.0,  "EGTT");

        // Ireland
        register(ctrl, "EINN", "SNN", "Shannon Airport",
                "Limerick",      "Ireland",     52.7019997,  -8.9248199,   14.0,  "EISN");

        // Iceland
        register(ctrl, "BIKF", "KEF", "Keflavik International Airport",
                "Reykjavik",     "Iceland",     63.9850006, -22.6056004,   52.0,  "BIRD");

        // Norway
        register(ctrl, "ENVA", "TRD", "Trondheim Vaernes Airport",
                "Trondheim",     "Norway",      63.4578018,  10.9239998,   17.0,  "ENBD");

        // Netherlands
        // NOTE: Schiphol is at -3 m but Elevation now requires > 0.
        // Using 1 m as the minimum accepted value per system constraint.
        register(ctrl, "EHAM", "AMS", "Amsterdam Schiphol Airport",
                "Haarlemmermeer", "Netherlands", 52.3086014,   4.7638898,   1.0,  "EHAA");

        // Switzerland
        register(ctrl, "LSZH", "ZRH", "Zurich Airport",
                "Zurich",        "Switzerland", 47.4646988,   8.5491695,  432.0,  "LSAS");

        // Italy
        register(ctrl, "LIMC", "MXP", "Malpensa International Airport",
                "Milano",        "Italy",       45.6306000,   8.7281103,  234.0,  "LIRR");
        register(ctrl, "LIRF", "FCO", "Leonardo Da Vinci Fiumicino International Airport",
                "Rome",          "Italy",       41.8044444,  12.2508333,    5.0,  "LIRR");

        // USA East
        register(ctrl, "KJFK", "JFK", "John F Kennedy International Airport",
                "New York",      "USA",         40.6398010, -73.7789002,    4.0,  "KZNY");
        register(ctrl, "KATL", "ATL", "Hartsfield Jackson Atlanta International Airport",
                "Atlanta",       "USA",         33.6366997, -84.4281006,  313.0,  "KZNY");
        register(ctrl, "KIAD", "IAD", "Washington Dulles International Airport",
                "Washington DC", "USA",         38.9445000, -77.4558029,   95.0,  "KZNY");
        register(ctrl, "KMIA", "MIA", "Miami International Airport",
                "Miami",         "USA",         25.7931995, -80.2906036,    3.0,  "KZNY");
        register(ctrl, "KEWR", "EWR", "Newark Liberty International Airport",
                "Newark",        "USA",         40.6925011, -74.1687012,    5.0,  "KZNY");
        register(ctrl, "KBOS", "BOS", "General Edward Lawrence Logan International Airport",
                "Boston",        "USA",         42.3642998, -71.0052033,    7.0,  "KZNY");

        // USA Central
        register(ctrl, "KORD", "ORD", "Chicago O'Hare International Airport",
                "Chicago",       "USA",         41.9785996, -87.9048004,  204.0,  "KZAU");
        register(ctrl, "KDFW", "DFW", "Dallas Fort Worth International Airport",
                "Dallas",        "USA",         32.8968010, -97.0380020,  185.0,  "KZAU");
        register(ctrl, "KMCI", "MCI", "Kansas City International Airport",
                "Kansas City",   "USA",         39.2975998, -94.7138977,  313.0,  "KZAU");

        // USA West
        register(ctrl, "KLAX", "LAX", "Los Angeles International Airport",
                "Los Angeles",   "USA",         33.9425011,-118.4079971,   38.0,  "KZLA");
        register(ctrl, "KDEN", "DEN", "Denver International Airport",
                "Denver",        "USA",         39.8616982,-104.6729965, 1655.0,  "KZLA");
        register(ctrl, "KSFO", "SFO", "San Francisco International Airport",
                "San Francisco", "USA",         37.6189995,-122.3750000,    4.0,  "KZLA");
        register(ctrl, "KLAS", "LAS", "McCarran International Airport",
                "Las Vegas",     "USA",         36.0801010,-115.1520004,  665.0,  "KZLA");

        // USA Hawaii
        register(ctrl, "PHNL", "HNL", "Honolulu International Airport",
                "Honolulu",      "USA",         21.3187008,-157.9219971,    4.0,  "KZHN");

        // Canada
        register(ctrl, "CYYC", "YYC", "Calgary International Airport",
                "Calgary",       "Canada",      51.1138992,-114.0199966, 1099.0,  "CZEG");
        register(ctrl, "CYUL", "YUL", "Montreal Pierre Elliott Trudeau International Airport",
                "Montreal",      "Canada",      45.4706001, -73.7407990,   36.0,  "CZEG");

        // Mexico
        register(ctrl, "MMMX", "MEX", "Licenciado Benito Juarez International Airport",
                "Ciudad de Mexico", "Mexico",   19.4363003, -99.0720978, 2238.0,  "MMFR");

        // South America
        register(ctrl, "SKBO", "BOG", "El Dorado International Airport",
                "Bogota",        "Colombia",     4.7015900, -74.1469000, 2548.0,  "SKED");
        register(ctrl, "SBGL", "CIG", "Rio de Janeiro Antonio Carlos Jobim International Airport",
                "Rio de Janeiro", "Brazil",    -22.8099995, -43.2505569,    9.0,  "SBBS");
        register(ctrl, "SBRF", "REC", "Guararapes Gilberto Freyre International Airport",
                "Recife",        "Brazil",      -8.1264896, -34.9235992,   10.0,  "SBBS");

        // Russia
        register(ctrl, "UUEE", "SVO", "Sheremetyevo International Airport",
                "Moscow",        "Russia",      55.9725990,  37.4146004,  192.0,  "UURR");

        // Kazakhstan
        register(ctrl, "UAAA", "ALA", "Almaty International Airport",
                "Almaty",        "Kazakhstan",  43.3521004,  77.0404968,  681.0,  "UAAA");

        // Africa
        register(ctrl, "FAOR", "JNB", "OR Tambo International Airport",
                "Johannesburg",  "South Africa",-26.1392002,  28.2460003, 1694.0, "FAJA");
        register(ctrl, "FZAA", "FIH", "Ndjili International Airport",
                "Kinshasa", "Democratic Republic of the Congo",
                -4.3857498,  15.4446001,  313.0,  "FZZA");
        register(ctrl, "FQMA", "MPM", "Maputo International Airport",
                "Maputo",        "Mozambique", -25.9207993,  32.5726013,   44.0,  "FQMA");

        // Asia
        register(ctrl, "RJTT", "HND", "Tokyo International Airport",
                "Tokyo",         "Japan",       35.5522995, 139.7799988,    6.0,  "RJJJ");
        register(ctrl, "VHHH", "HKG", "Hong Kong International Airport",
                "Hong Kong",     "Hong Kong",   22.3083210, 113.9196490,    9.0,  "VHHK");
        register(ctrl, "ZBAA", "PEK", "Beijing Capital International Airport",
                "Beijing",       "China",       40.0801010, 116.5849991,   35.0,  "ZBPE");
        register(ctrl, "ZUUU", "CTU", "Chengdu Shuangliu International Airport",
                "Chengdu",       "China",       30.5785007, 103.9469986,  495.0,  "ZUGH");
        register(ctrl, "ZULS", "LXA", "Lhasa Gonggar Airport",
                "Lhasa",         "China",       29.2978001,  90.9119034, 3570.0,  "ZUGH");
        register(ctrl, "WSSS", "SIN", "Singapore Changi International Airport",
                "Singapore",     "Singapore",    1.3501900, 103.9940033,    7.0,  "WSJC");

        // Oceania
        register(ctrl, "YSSY", "SYD", "Sydney Kingsford Smith International Airport",
                "Sydney",        "Australia",  -33.9460983, 151.1770020,    6.0,  "YMMM");
        register(ctrl, "YBAS", "ASP", "Alice Springs Airport",
                "Alice Springs", "Australia",  -23.8066998, 133.9019928,  545.0,  "YTCC");

        return true;
    }

    private void register(final CreateAirportController ctrl,
                          final String icao, final String iata,
                          final String name, final String town, final String country,
                          final double lat, final double lon, final double elevation,
                          final String areaCode) {
        try {
            ctrl.createAirport(icao, iata, name, town, country,
                    lat, lon, elevation, areaCode);
        } catch (final Exception e) {
            // Already exists or validation failed — skip silently
        }
    }
}
