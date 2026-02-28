//package br.com.centavo.repository;
//
//import br.com.centavo.enums.TransactionType;
//import br.com.centavo.entity.Transaction;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
//@Repository
//public class TransactionDAO {
//    private final JdbcTemplate jdbcTemplate;
//
//    public TransactionDAO(JdbcTemplate jdbcTemplate) {
//        this.jdbcTemplate = jdbcTemplate;
//    }
//
//    public void save(Transaction transaction) {
//        String sql = "INSERT INTO transactions (account_id, category_id, date, value, description, type) VALUES (?, ?, ?, ?, ?, ?)";
//        jdbcTemplate.update(sql,
//                transaction.getAccountId(),
//                transaction.getCategoryId(),
//                transaction.getDate(),
//                transaction.getValue(),
//                transaction.getDescription(),
//                transaction.getType().name()
//        );
//    }
//
//    public List<Transaction> findAll() {
//        String sql = "SELECT * FROM transactions";
//        return jdbcTemplate.query(sql, (rs, rowNum) -> {
//           return new Transaction(
//             rs.getLong("id"),
//             rs.getLong("account_id"),
//             rs.getLong("category_id"),
//                   rs.getObject("date", java.time.LocalDate.class),
//                   rs.getBigDecimal("value"),
//             rs.getString("description"),
//                   TransactionType.valueOf(rs.getString("type"))
//           );
//        });
//    }
//}
