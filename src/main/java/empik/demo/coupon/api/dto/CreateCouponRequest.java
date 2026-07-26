package empik.demo.coupon.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCouponRequest(
        @NotBlank String code,
        @NotBlank @Size(min = 2, max = 2) @Pattern(regexp = "^[A-Za-z]{2}$") String countryCode,
        @NotNull @Min(1) Integer maxUses
) {
}