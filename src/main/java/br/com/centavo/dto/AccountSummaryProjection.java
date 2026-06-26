package br.com.centavo.dto;

import java.math.BigDecimal;

public interface AccountSummaryProjection {
    Long getAccountId();
    String getAccountName();
    String getAccountType();
    BigDecimal getTotalRevenue();
    BigDecimal getTotalExpense();
}