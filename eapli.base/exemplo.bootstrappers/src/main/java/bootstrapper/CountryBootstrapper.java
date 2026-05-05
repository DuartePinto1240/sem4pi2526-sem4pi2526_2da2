package bootstrapper;

import domain.Country;
import eapli.framework.actions.Action;
import persistence.PersistenceContext;
import repositories.CountryRepository;

/**
 * Bootstrap action that pre-loads a set of countries into the system.
 *
 * <p>Must run before {@code AirportBootstrapper} so that country data
 * is available when airports are registered.</p>
 *
 * <p>The professor confirmed: "You can/must load country data via bootstrap.
 * The city can be inserted when creating an airport."</p>
 */
public class CountryBootstrapper implements Action {

    @Override
    public boolean execute() {
        final CountryRepository repo = PersistenceContext.getInstance().countryRepository();

        final String[] countries = {
                "Afghanistan", "Albania", "Algeria", "Andorra", "Angola",
                "Argentina", "Armenia", "Australia", "Austria", "Azerbaijan",
                "Bahrain", "Bangladesh", "Belarus", "Belgium", "Bolivia",
                "Bosnia and Herzegovina", "Brazil", "Bulgaria", "Cambodia",
                "Cameroon", "Canada", "Chile", "China", "Colombia", "Croatia",
                "Cuba", "Cyprus", "Czech Republic", "Denmark", "Ecuador",
                "Egypt", "Estonia", "Ethiopia", "Finland", "France",
                "Georgia", "Germany", "Ghana", "Greece", "Guatemala",
                "Hungary", "Iceland", "India", "Indonesia", "Iran",
                "Iraq", "Ireland", "Israel", "Italy", "Japan",
                "Jordan", "Kazakhstan", "Kenya", "Kosovo", "Kuwait",
                "Latvia", "Lebanon", "Libya", "Lithuania", "Luxembourg",
                "Malaysia", "Malta", "Mexico", "Moldova", "Monaco",
                "Mongolia", "Montenegro", "Morocco", "Mozambique", "Myanmar",
                "Nepal", "Netherlands", "New Zealand", "Nigeria", "Norway",
                "Oman", "Pakistan", "Panama", "Paraguay", "Peru",
                "Philippines", "Poland", "Portugal", "Qatar", "Romania",
                "Russia", "Saudi Arabia", "Serbia", "Singapore", "Slovakia",
                "Slovenia", "South Africa", "South Korea", "Spain", "Sri Lanka",
                "Sweden", "Switzerland", "Syria", "Taiwan", "Thailand",
                "Tunisia", "Turkey", "Ukraine", "United Arab Emirates",
                "United Kingdom", "United States", "Uruguay", "Uzbekistan",
                "Venezuela", "Vietnam", "Yemen", "Zimbabwe"
        };

        for (final String name : countries) {
            try {
                if (!repo.existsByName(name)) {
                    repo.save(new Country(name));
                }
            } catch (final Exception e) {
                // Skip silently — already exists or validation failed
            }
        }

        return true;
    }
}
