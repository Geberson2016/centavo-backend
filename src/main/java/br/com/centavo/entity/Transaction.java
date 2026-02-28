//package br.com.centavo.entity;
//
//import br.com.centavo.enums.TransactionType;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//
//public class Transaction {
//    private Long id;
//    private Long accountId;
//    private Long categoryId;
//    private LocalDate date;
//    private BigDecimal value;
//    private String description;
//    private TransactionType type;
//
//    public Transaction(){}
//
//    public Transaction(Long id, Long accountId, Long categoryId, LocalDate date, BigDecimal value, String description, TransactionType type) {
//        this.id = id;
//        this.accountId = accountId;
//        this.categoryId = categoryId;
//        this.date = date;
//        this.value = value;
//        this.description = description;
//        this.type = type;
//    }
//
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public Long getAccountId() {
//        return accountId;
//    }
//
//    public void setAccountId(Long accountId) {
//        this.accountId = accountId;
//    }
//
//    public Long getCategoryId() {
//        return categoryId;
//    }
//
//    public void setCategoryId(Long categoryId) {
//        this.categoryId = categoryId;
//    }
//
//    public LocalDate getDate() {
//        return date;
//    }
//
//    public void setDate(LocalDate date) {
//        this.date = date;
//    }
//
//    public BigDecimal getValue() {
//        return value;
//    }
//
//    public void setValue(BigDecimal value) {
//        this.value = value;
//    }
//
//    public String getDescription() {
//        return description;
//    }
//
//    public void setDescription(String description) {
//        this.description = description;
//    }
//
//    public TransactionType getType() {
//        return type;
//    }
//
//    public void setType(TransactionType type) {
//        this.type = type;
//    }
//}
