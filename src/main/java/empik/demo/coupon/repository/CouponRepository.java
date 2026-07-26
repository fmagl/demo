package empik.demo.coupon.repository;

import empik.demo.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, String> {

    @Modifying
    @Query("UPDATE Coupon c SET c.currentUses = c.currentUses + 1 WHERE c.code = :code AND c.currentUses < c.maxUses")
    int incrementCurrentUses(@Param("code") String code);
}