package weathercli.model;
/**
 * Record to hold the fetched weather data.
 */
public record WeatherData(
        double temperature,
        int humidity,
        double windSpeed,
        int weatherCode
) {}
