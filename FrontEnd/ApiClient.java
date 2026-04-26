import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;

public class ApiClient {

    private static final CookieManager cookieManager = new CookieManager();
    private static final HttpClient client;

    static {
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .build();
    }
    public static HttpClient getClient() {
        System.out.println("getClient called from: " + Thread.currentThread().getStackTrace()[2]);
        System.out.println("Client hash: " + client.hashCode());
        return client;
    }
}