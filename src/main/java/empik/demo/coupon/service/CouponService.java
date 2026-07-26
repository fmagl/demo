
package empik.demo.coupon.service;

import empik.demo.coupon.api.dto.CreateCouponRequest;
import empik.demo.coupon.api.dto.CouponResponse;
import empik.demo.coupon.api.dto.CouponUsageResponse;
import empik.demo.coupon.api.dto.RedeemCouponRequest;
import empik.demo.coupon.domain.Coupon;
import empik.demo.coupon.domain.CouponUsage;
import empik.demo.coupon.repository.CouponRepository;
import empik.demo.coupon.repository.CouponUsageRepository;
import empik.demo.coupon.service.exception.CountryMismatchException;
import empik.demo.coupon.service.exception.CouponAlreadyExistsException;
import empik.demo.coupon.service.exception.CouponAlreadyUsedException;
import empik.demo.coupon.service.exception.CouponLimitReachedException;
import empik.demo.coupon.service.exception.CouponNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final GeoIpService geoIpService;
    private final TransactionTemplate transactionTemplate;

    public CouponService(
            CouponRepository couponRepository,
            CouponUsageRepository couponUsageRepository,
            GeoIpService geoIpService,
            TransactionTemplate transactionTemplate
    ) {
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
        this.geoIpService = geoIpService;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request) {
        String normalizedCode = normalizeCouponCode(request.code());
        String normalizedCountryCode = normalizeCountryCode(request.countryCode());

        Coupon coupon = new Coupon(normalizedCode, normalizedCountryCode, request.maxUses());

        try {
            Coupon savedCoupon = couponRepository.save(coupon);
            return toResponse(savedCoupon);
        } catch (DataIntegrityViolationException exception) {
            throw new CouponAlreadyExistsException(normalizedCode, exception);
        }
    }

    public CouponUsageResponse redeemCoupon(String code, RedeemCouponRequest request, HttpServletRequest httpServletRequest) {
        String normalizedCode = normalizeCouponCode(code);
        String normalizedUserId = normalizeUserId(request.userId());

        Coupon coupon = couponRepository.findById(normalizedCode)
                .orElseThrow(() -> new CouponNotFoundException(normalizedCode));

        String requestCountryCode = geoIpService.resolveCountryCode(httpServletRequest);
        if (!coupon.getCountryCode().equalsIgnoreCase(requestCountryCode)) {
            throw new CountryMismatchException(normalizedCode, coupon.getCountryCode(), requestCountryCode);
        }

        CouponUsageResponse couponUsageResponse = transactionTemplate.execute(status -> {
            CouponUsage usage = new CouponUsage(normalizedCode, normalizedUserId);
            try {
                couponUsageRepository.saveAndFlush(usage);
            } catch (DataIntegrityViolationException exception) {
                throw new CouponAlreadyUsedException(normalizedCode, normalizedUserId, exception);
            }

            int updatedRows = couponRepository.incrementCurrentUses(normalizedCode);
            if (updatedRows == 0) {
                throw new CouponLimitReachedException(normalizedCode);
            }

            return toResponse(usage);
        });

        if (couponUsageResponse == null) {
            throw new IllegalStateException("Coupon usage response must not be null");
        }

        return couponUsageResponse;
    }

    private String normalizeCouponCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCountryCode(String countryCode) {
        return countryCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeUserId(String userId) {
        return userId.trim();
    }

    private CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(
                coupon.getCode(),
                coupon.getCountryCode(),
                coupon.getMaxUses(),
                coupon.getCurrentUses(),
                coupon.getCreatedAt()
        );
    }

    private CouponUsageResponse toResponse(CouponUsage couponUsage) {
        return new CouponUsageResponse(
                couponUsage.getCouponCode(),
                couponUsage.getUserId(),
                couponUsage.getUsedAt()
        );
    }
}