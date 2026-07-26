package empik.demo.coupon.repository;

import empik.demo.coupon.domain.CouponUsage;
import empik.demo.coupon.domain.CouponUsageId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, CouponUsageId> {
}