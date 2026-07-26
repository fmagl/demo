package empik.demo.coupon.service.exception;

public class CouponAlreadyExistsException extends RuntimeException {

    public CouponAlreadyExistsException(String couponCode, Throwable cause) {
        super("Coupon already exists: " + couponCode, cause);
    }
}