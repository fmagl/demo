package empik.demo.coupon.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coupon.geoip")
public record GeoIpProperties(String baseUrl) {
}