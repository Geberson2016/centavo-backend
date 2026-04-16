package br.com.centavo.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        BigDecimal totalBalance,
        BigDecimal creditCardBill,
        BigDecimal monthlySavings
) {}
