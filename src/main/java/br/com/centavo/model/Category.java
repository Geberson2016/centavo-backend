package br.com.centavo.model;

import br.com.centavo.enums.BudgetType;
import br.com.centavo.enums.TransactionType;

public class Category {
    private Long id;
    private Long userId;
    private String name;
    private TransactionType type;
    private BudgetType budgetType;

    public Category(){}

    public Category(Long id, Long userId, String name, TransactionType type, BudgetType budgetType) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.budgetType = budgetType;
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

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public BudgetType getBudgetType() {
        return budgetType;
    }

    public void setBudgetType(BudgetType budgetType) {
        this.budgetType = budgetType;
    }
}
