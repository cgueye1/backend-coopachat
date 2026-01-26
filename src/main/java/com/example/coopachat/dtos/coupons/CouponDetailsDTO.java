package com.example.coopachat.dtos.coupons;

import com.example.coopachat.enums.CouponScope;
import com.example.coopachat.enums.CouponStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO pour les détails d'un coupon
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponDetailsDTO {

    private Long id;
    private String code;
    private String name;
    private BigDecimal value;
    private CouponScope scope;
    private CouponStatus status;
    private Boolean isActive;
    private String validFrom; // dd-MM-yyyy
    private String validTo;   // dd-MM-yyyy
    private Integer usageCount;
    private BigDecimal totalGenerated;
    private List<CouponProductItemDTO> products;
}
