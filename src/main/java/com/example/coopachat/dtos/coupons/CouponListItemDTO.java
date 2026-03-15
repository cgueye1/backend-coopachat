package com.example.coopachat.dtos.coupons;

import com.example.coopachat.enums.CouponStatus;
import com.example.coopachat.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour lister un coupon
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponListItemDTO {

    private Long id;
    private String code;
    private String name;
    private DiscountType discountType;
    private BigDecimal value;
    private CouponStatus status;
    private String validFrom; // dd-MM-yyyy
    private String validTo;   // dd-MM-yyyy
    private Integer usageCount;
    private BigDecimal totalGenerated;
}
