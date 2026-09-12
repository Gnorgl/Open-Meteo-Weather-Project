package weathercli.model;

public record WeatherData(
        double temperature,
        int humidity,
        double windSpeed,
        int weatherCode
) {}
