package com.revpay.wallet_service.dto;

import com.revpay.wallet_service.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodDto {
    private Long id;
    private String cardNumber; // Masked
    private String expiryDate;
    @com.fasterxml.jackson.annotation.JsonProperty("isDefault")
    private boolean isDefault;
    private PaymentMethod.CardType cardType;
}
