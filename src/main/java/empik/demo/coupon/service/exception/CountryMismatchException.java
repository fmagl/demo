package empik.demo.coupon.service.exception;

public class CountryMismatchException extends RuntimeException {

    public CountryMismatchException(String couponCode, String expectedCountryCode, String actualCountryCode) {
        super("Coupon " + couponCode + " is restricted to country " + expectedCountryCode + ", but request came from " + actualCountryCode);
    }
}