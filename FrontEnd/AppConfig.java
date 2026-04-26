import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

public class AppConfig {
    public static final String BASE_URL = "";

    public static HttpRequest.Builder requestBuilder(String endpoint) {
        return requestBuilderRaw(endpoint)
                .header("Content-Type", "application/json");
    }

    public static HttpRequest.Builder requestBuilderRaw(String endpoint) {
        String normalizedEndpoint = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
        if (!normalizedEndpoint.startsWith("/api/")) {
            normalizedEndpoint = "/api" + normalizedEndpoint;
        }

        return HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + normalizedEndpoint));
    }

    private static String currentUserId = "";

    public static void setCurrentUserId(String id) {
        currentUserId = id;
    }

    public static String getCurrentUserId() {
        return currentUserId;
    }
}
