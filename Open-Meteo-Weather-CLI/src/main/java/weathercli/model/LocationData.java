package weathercli.model;

public record LocationData(
        String name,
        String country,
        double latitude,
        double longitude
) {}
