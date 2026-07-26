package empik.demo.coupon.api;

import empik.demo.coupon.service.exception.CountryMismatchException;
import empik.demo.coupon.service.exception.CouponAlreadyExistsException;
import empik.demo.coupon.service.exception.CouponAlreadyUsedException;
import empik.demo.coupon.service.exception.CouponLimitReachedException;
import empik.demo.coupon.service.exception.CouponNotFoundException;
import empik.demo.coupon.service.exception.GeoIpLookupException;
import empik.demo.coupon.service.exception.InvalidCouponCodeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_TYPE_PREFIX = "https://demo.empik.com/errors/";

    @ExceptionHandler(CouponNotFoundException.class)
    public ProblemDetail handleCouponNotFound(CouponNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Coupon Not Found");
        problem.setType(URI.create(ERROR_TYPE_PREFIX + "coupon-not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler({
            CouponLimitReachedException.class,
            CouponAlreadyUsedException.class,
            CouponAlreadyExistsException.class
    })
    public ProblemDetail handleConflicts(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Coupon Conflict");
        problem.setType(URI.create(ERROR_TYPE_PREFIX + "coupon-conflict"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(CountryMismatchException.class)
    public ProblemDetail handleCountryMismatch(CountryMismatchException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Country Mismatch");
        problem.setType(URI.create(ERROR_TYPE_PREFIX + "country-mismatch"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidCouponCodeException.class)
    public ProblemDetail handleInvalidCouponCode(InvalidCouponCodeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Coupon Code");
        problem.setType(URI.create(ERROR_TYPE_PREFIX + "invalid-coupon-code"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(GeoIpLookupException.class)
    public ProblemDetail handleGeoIpLookup(GeoIpLookupException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "External GeoIP service is currently unavailable.");
        problem.setTitle("GeoIP Service Error");
        problem.setType(URI.create(ERROR_TYPE_PREFIX + "geoip-unavailable"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for one or more fields.");
        problem.setTitle("Validation Error");
        problem.setType(URI.create(ERROR_TYPE_PREFIX + "validation-error"));
        problem.setProperty("timestamp", Instant.now());
        
        List<String> invalidFields = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        
        problem.setProperty("invalidFields", invalidFields);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create(ERROR_TYPE_PREFIX + "internal-error"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}