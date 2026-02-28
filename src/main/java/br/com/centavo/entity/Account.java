package br.com.centavo.entity;

import br.com.centavo.enums.AccountType;

public class Account {
    private Long id;
    private Long userId; // A chave estrangeira aqui
    private String name;
    private AccountType type;

    public Account() {}

    public Account(Long id, Long userId, String name, AccountType type) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }
}
