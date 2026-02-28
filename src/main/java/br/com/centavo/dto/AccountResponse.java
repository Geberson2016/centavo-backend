package br.com.centavo.dto;

import br.com.centavo.enums.AccountType;

public record AccountResponse(
        Long id,
        String name,
        AccountType type,
        Long userId
) {}
