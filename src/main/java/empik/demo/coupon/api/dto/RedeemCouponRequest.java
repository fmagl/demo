package empik.demo.coupon.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedeemCouponRequest(
        @NotBlank @Size(max = 100) String userId
) {
}