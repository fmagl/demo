package empik.demo.coupon.service;

import empik.demo.coupon.service.client.GeoIpLookupResponse;
import empik.demo.coupon.service.exception.GeoIpLookupException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GeoIpService {

    private static final String DEFAULT_LOCAL_COUNTRY_CODE = "PL";
    private static final String LOCALHOST_IPV4 = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    private static final String LOCALHOST_IPV6_SHORT = "::1";

    private final RestClient geoIpRestClient;

    public GeoIpService(RestClient geoIpRestClient) {
        this.geoIpRestClient = geoIpRestClient;
    }

    public String resolveCountryCode(HttpServletRequest request) {
        String clientIp = resolveClientIp(request);
        if (isLocalDevelopmentIp(clientIp)) {
            return DEFAULT_LOCAL_COUNTRY_CODE;
        }

        return resolveCountryCodeFromGeoIp(clientIp);
    }

    private String resolveCountryCodeFromGeoIp(String clientIp) {
        try {
            GeoIpLookupResponse response = geoIpRestClient.get()
                    .uri("/{ip}", clientIp)
                    .retrieve()
                    .body(GeoIpLookupResponse.class);

            return extractCountryCode(response, clientIp);
        } catch (RuntimeException exception) {
            if (exception instanceof GeoIpLookupException geoIpLookupException) {
                throw geoIpLookupException;
            }

            throw new GeoIpLookupException("Unable to resolve country code for IP: " + clientIp, exception);
        }
    }

    private String extractCountryCode(GeoIpLookupResponse response, String clientIp) {
        if (response == null || !response.isSuccessful()) {
            throw new GeoIpLookupException("Unable to resolve country code for IP: " + clientIp);
        }

        String countryCode = response.countryCode();
        if (countryCode == null || countryCode.isBlank()) {
            throw new GeoIpLookupException("Unable to resolve country code for IP: " + clientIp);
        }

        return countryCode.toUpperCase(Locale.ROOT);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private boolean isLocalDevelopmentIp(String ipAddress) {
        return ipAddress != null && (
                LOCALHOST_IPV4.equals(ipAddress)
                        || LOCALHOST_IPV6.equals(ipAddress)
                        || LOCALHOST_IPV6_SHORT.equals(ipAddress)
                        || ipAddress.startsWith("192.168.")
        );
    }
}