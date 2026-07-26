package empik.demo.coupon.service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeoIpLookupResponse(
        String status,
        String countryCode,
        String message
) {
    public boolean isSuccessful() {
        return "success".equalsIgnoreCase(status);
    }
}