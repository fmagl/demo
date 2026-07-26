package empik.demo.coupon.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class CouponUsageId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String couponCode;
    private String userId;

    public CouponUsageId() {
    }

    public CouponUsageId(String couponCode, String userId) {
        this.couponCode = couponCode;
        this.userId = userId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CouponUsageId that = (CouponUsageId) o;
        return Objects.equals(couponCode, that.couponCode) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(couponCode, userId);
    }
}