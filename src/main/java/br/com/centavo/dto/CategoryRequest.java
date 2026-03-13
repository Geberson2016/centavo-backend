package br.com.centavo.dto;

import br.com.centavo.enums.BudgetType;
import br.com.centavo.enums.TransactionType;

public record CategoryRequest(
        String name,
        TransactionType type,
        Long userId,
        BudgetType budgetType
) {}
