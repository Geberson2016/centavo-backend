package br.com.centavo.dto;

public record AuthRequest(
        String email,
        String password
) {}