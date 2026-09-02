package br.com.centavo.dto;

import br.com.centavo.enums.BudgetType;
import br.com.centavo.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryRequest(
        @NotBlank String name,
        @NotNull TransactionType type,
        Long userId,
        @NotNull BudgetType budgetType
) {}
