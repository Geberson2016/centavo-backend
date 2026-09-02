package br.com.centavo.dto;

import br.com.centavo.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AccountRequest(
        @NotBlank String name,
        @NotNull AccountType type,
        Long userId
) {}
