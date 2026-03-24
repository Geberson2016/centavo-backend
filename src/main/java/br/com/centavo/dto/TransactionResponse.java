package br.com.centavo.dto;

import br.com.centavo.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse (
        Long id,
        String description,
        BigDecimal value,
        LocalDate date,
        TransactionType type,
        Long accountId,
        Long categoryId
) {
}
