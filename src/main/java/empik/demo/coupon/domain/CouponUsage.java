
package empik.demo.coupon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;


@Entity
@Table(name = "coupon_usages")
@IdClass(CouponUsageId.class)
public class CouponUsage {

    @Id
    @Column(name = "coupon_code", nullable = false, length = 50)
    private String couponCode;

    @Id
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @ManyToOne
    @JoinColumn(name = "coupon_code", insertable = false, updatable = false)
    private Coupon coupon;

    @Column(name = "used_at", nullable = false, updatable = false)
    private Instant usedAt;

    protected CouponUsage() {
    }

    public CouponUsage(String couponCode, String userId) {
        this.couponCode = couponCode;
        this.userId = userId;
    }

    @PrePersist
    void prePersist() {
        if (usedAt == null) {
            usedAt = Instant.now();
        }
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Coupon getCoupon() {
        return coupon;
    }

    public void setCoupon(Coupon coupon) {
        this.coupon = coupon;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }
}