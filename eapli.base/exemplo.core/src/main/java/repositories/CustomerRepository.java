package repositories;

import domain.AirControlArea;
import domain.AirControlAreaCode;
import domain.AirTransportCompany;
import domain.CompanyCode;
import dto.CustomerDTO;

import java.util.Optional;

/**
 * Single repository for all customer aggregates.
 *
 * A "customer" is either an {@link AirTransportCompany} or an
 * {@link AirControlArea}. The sequence diagrams for all collaborator
 * management use cases show a single CustomerRepository participant that
 * handles both types — this interface reflects that design.
 *
 * findById() routes to the correct aggregate using CustomerType,
 * which is the discriminator the controller receives from the UI.
 */
public interface CustomerRepository {

    /** Returns all registered Air Transport Companies. */
    Iterable<AirTransportCompany> findAllAirTransportCompanies();

    /** Returns all registered Air Control Areas. */
    Iterable<AirControlArea> findAllAirControlAreas();

    /**
     * Finds a customer aggregate by its string identity and type.
     *
     * For AIR_TRANSPORT_COMPANY the id is "IATA/ICAO" (e.g. "TP/TAP").
     * For AIR_CONTROL_AREA the id is the area code (e.g. "LPPC").
     *
     * @param customerId   string identity of the customer
     * @param customerType discriminator between company and area
     * @return Optional containing the matching aggregate, or empty
     */
    Optional<Object> findById(String customerId, CustomerDTO.CustomerType customerType);

    /** Persists an updated Air Transport Company aggregate. */
    AirTransportCompany save(AirTransportCompany company);

    /** Persists an updated Air Control Area aggregate. */
    AirControlArea save(AirControlArea area);
}