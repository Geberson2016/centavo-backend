package br.com.centavo.dto;

import java.math.BigDecimal;

public record AccountSummaryResponse(
        Long accountId,
        String accountName,
        String accountType,
        BigDecimal total
) {}