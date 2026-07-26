package empik.demo.coupon.api;

import empik.demo.coupon.api.dto.CreateCouponRequest;
import empik.demo.coupon.api.dto.CouponResponse;
import empik.demo.coupon.api.dto.CouponUsageResponse;
import empik.demo.coupon.api.dto.RedeemCouponRequest;
import empik.demo.coupon.service.CouponService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(couponService.createCoupon(request));
    }

    @PostMapping("/{code}/redeem")
    public ResponseEntity<CouponUsageResponse> redeemCoupon(
            @PathVariable String code,
            @Valid @RequestBody RedeemCouponRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(couponService.redeemCoupon(code, request, httpServletRequest));
    }
}