package br.com.centavo.dto;

import br.com.centavo.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecentTransactionResponse(
        String description,
        String categoryName,
        LocalDate date,
        BigDecimal value,
        TransactionType type
) {}