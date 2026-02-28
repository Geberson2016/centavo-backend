package br.com.centavo.dto;

import br.com.centavo.enums.AccountType;

public record AccountRequest(
        String name,
        AccountType type,
        Long userId
) {}
