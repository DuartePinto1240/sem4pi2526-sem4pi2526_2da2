package dto;

/**
 * Read-only DTO carrying the minimum data the UI needs to show a customer list.
 */
public class CustomerDTO {

    public enum CustomerType { AIR_TRANSPORT_COMPANY, AIR_CONTROL_AREA }

    public final String       identity; // iata+icao string for company, code string for area
    public final String       name;
    public final CustomerType type;

    public CustomerDTO(final String identity, final String name, final CustomerType type) {
        this.identity = identity;
        this.name     = name;
        this.type     = type;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s)", type, name, identity);
    }
}