package weathercli.model;
/**
 * Record to hold the fetched city data.
 */
public record LocationData(
        String name,
        String country,
        double latitude,
        double longitude
) {}
