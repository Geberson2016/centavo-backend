package br.com.centavo.dto;

import br.com.centavo.enums.AccountType;

import java.math.BigDecimal;

public record AccountSummaryResponse(
        Long accountId,
        String accountName,
        AccountType accountType,
        BigDecimal totalRevenue,
        BigDecimal totalExpense
) {}