package empik.demo.coupon.service.exception;

public class CouponLimitReachedException extends RuntimeException {

    public CouponLimitReachedException(String couponCode) {
        super("Coupon usage limit reached for code: " + couponCode);
    }
}