package empik.demo.coupon.service.exception;

public class CouponNotFoundException extends RuntimeException {

    public CouponNotFoundException(String couponCode) {
        super("Coupon not found: " + couponCode);
    }
}