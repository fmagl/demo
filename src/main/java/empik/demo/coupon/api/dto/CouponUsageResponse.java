package empik.demo.coupon.api.dto;

import java.time.Instant;

public record CouponUsageResponse(
        String couponCode,
        String userId,
        Instant usedAt
) {
}