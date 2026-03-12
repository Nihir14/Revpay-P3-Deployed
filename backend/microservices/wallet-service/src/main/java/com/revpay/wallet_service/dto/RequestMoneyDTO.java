package com.revpay.wallet_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestMoneyDTO {
    private String targetEmail;
    private BigDecimal amount;
}
