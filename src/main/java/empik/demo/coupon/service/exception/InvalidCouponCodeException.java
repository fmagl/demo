package empik.demo.coupon.service.exception;

public class InvalidCouponCodeException extends RuntimeException {

    public InvalidCouponCodeException(String message) {
        super(message);
    }
}