package empik.demo.coupon.service.exception;

public class CouponAlreadyUsedException extends RuntimeException {

    public CouponAlreadyUsedException(String couponCode, String userId, Throwable cause) {
        super("Coupon " + couponCode + " was already used by user " + userId, cause);
    }
}