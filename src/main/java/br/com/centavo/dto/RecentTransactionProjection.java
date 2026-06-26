package br.com.centavo.dto;

import br.com.centavo.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface RecentTransactionProjection {
    String getDescription();
    String getCategoryName();
    LocalDate getDate();
    BigDecimal getValue();
    TransactionType getType();
}