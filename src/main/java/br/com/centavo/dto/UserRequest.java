package br.com.centavo.dto;

public record UserRequest(
        String name,
        String email,
        String phone,
        String password
) {}