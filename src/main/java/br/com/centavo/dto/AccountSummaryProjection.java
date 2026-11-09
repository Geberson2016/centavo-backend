package br.com.centavo.dto;

import br.com.centavo.enums.AccountType;

import java.math.BigDecimal;

public interface AccountSummaryProjection {
    Long getAccountId();
    String getAccountName();
    AccountType getAccountType();
    BigDecimal getTotalRevenue();
    BigDecimal getTotalExpense();
}