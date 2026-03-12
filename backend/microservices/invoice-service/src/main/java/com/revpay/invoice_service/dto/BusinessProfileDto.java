package com.revpay.invoice_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessProfileDto {
    private Long profileId;
    private Long userId;
    private String businessName;
    private String businessType;
    private String taxId;
    private String address;
    private boolean isVerified;
}
