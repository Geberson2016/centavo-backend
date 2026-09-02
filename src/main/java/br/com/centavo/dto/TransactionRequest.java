package br.com.centavo.dto;

import br.com.centavo.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest (
        @NotBlank String description,
        @NotNull @Positive BigDecimal value,
        @NotNull LocalDate date,
        @NotNull Long accountId,
        @NotNull Long categoryId,
        @NotNull TransactionType type
) {
}
