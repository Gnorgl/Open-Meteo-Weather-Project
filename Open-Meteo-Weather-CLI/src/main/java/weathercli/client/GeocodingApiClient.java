package weathercli.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import weathercli.model.LocationData;

/**
 * Handles HttpClient requests to Geocoding.
 */
public class GeocodingApiClient {

    private static final String GEOCODING_API_URL = "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1";
    private final HttpClient httpClient;

    public GeocodingApiClient() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public LocationData fetchCoordinates(String cityName) {
        String encodedCity = java.net.URLEncoder.encode(cityName.trim(), java.nio.charset.StandardCharsets.UTF_8);
        //This ensures city names with spaces or special characters (e.g., "New York" or "São Paulo") are safely formatted as valid URLs.
        String url = String.format(GEOCODING_API_URL, encodedCity);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            //Throws two checked exceptions: IOException and InterruptedException.
            //When checked exceptions are thrown you have to handle them with try-catch.
            return switch (response.statusCode()) {
                case 200 -> parseLocationFromJson(response.body());
                case 400 -> throw new ApiException("Invalid request parameters sent to Geocoding API.");
                case 404 -> throw new ApiException("Geocoding endpoint not found.");
                case 429 -> throw new ApiException("Rate limit exceeded for Geocoding API. Please wait and try again.");
                default -> throw new ApiException("Geocoding API failed with HTTP status code: " + response.statusCode());
            };
        } catch (IOException e) {
            throw new ApiException("Network error while connecting to Geocoding API.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Geocoding request was interrupted.", e);
        }
    }

    private LocationData parseLocationFromJson(String json) {
        //{"results":[{"id":2950159,"name":"Berlin","latitude":52.52437,"longitude":13.41053,...}]}
        //1. Check if "results" key exists and contains data
        if (json == null || !json.contains("\"results\"")) {
            throw new ApiException("No search results found for the specified city.");
        }
        // 2. Extract the first result object from the "results" array
        int resultsStart = json.indexOf("\"results\"");
        int objectStart = json.indexOf("{", resultsStart);
        int objectEnd = json.indexOf("}", objectStart);

        if (objectStart == -1 || objectEnd == -1) {
            throw new ApiException("No valid city results found.");
        }

        String firstResultJson = json.substring(objectStart, objectEnd + 1);

        // 3. Parse individual field values using regex helpers
        String name = extractStringValue(firstResultJson, "name");
        String country = extractStringValue(firstResultJson, "country");
        Double latitude = extractNumberValue(firstResultJson, "latitude");
        Double longitude = extractNumberValue(firstResultJson, "longitude");

        if (name == null || latitude == null || longitude == null) {
            throw new ApiException("Failed to parse location details from response.");
        }

        // Default country to unknown if omitted by API
        if (country == null) {
            country = "Unknown";
        }

        return new LocationData(name, country, latitude, longitude);
    }

    private String extractStringValue(String json, String key) {
        // Matches "key":"value" or "key" : "value"
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Double extractNumberValue(String json, String key) {
        // Matches "key":12.34 or "key" : -56.78
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        java.util.regex.Matcher matcher = pattern.matcher(json);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : null;
    }

    public static void main(String[] args) {
        GeocodingApiClient client = new GeocodingApiClient();
        LocationData location = client.fetchCoordinates("Berlin");
        System.out.println(location);
    }
}
