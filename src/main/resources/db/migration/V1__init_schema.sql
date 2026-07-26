CREATE TABLE coupons (
    code VARCHAR(50) PRIMARY KEY,
    country_code VARCHAR(2) NOT NULL,
    max_uses INTEGER NOT NULL,
    current_uses INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE coupon_usages (
    coupon_code VARCHAR(50) NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    used_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (coupon_code, user_id),
    CONSTRAINT fk_coupon_usages_coupon FOREIGN KEY (coupon_code) REFERENCES coupons(code)
);