//package br.com.centavo.repository;
//
//import br.com.centavo.enums.BudgetType;
//import br.com.centavo.enums.TransactionType;
//import br.com.centavo.entity.Category;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
//@Repository
//public class CategoryDAO {
//    private final JdbcTemplate jdbcTemplate;
//
//    public CategoryDAO(JdbcTemplate jdbcTemplate) {
//        this.jdbcTemplate = jdbcTemplate;
//    }
//
//    public void save(Category category) {
//        String sql = "INSERT INTO categories (name, type, user_id, budget_type) VALUES (?, ?, ?, ?)";
//        jdbcTemplate.update(sql, category.getName(), category.getType().name(), category.getUserId(), category.getBudgetType().name());
//    }
//
//    public List<Category> findAll() {
//        String sql = "SELECT * FROM categories";
//        return jdbcTemplate.query(sql, (rs, rowNum) -> {
//            Long userId = rs.getLong("user_id");
//            if (rs.wasNull()) {
//                userId = null;
//            }
//
//            return new Category(
//                    rs.getLong("id"),
//                    userId,
//                    rs.getString("name"),
//                    TransactionType.valueOf(rs.getString("type")),
//                    BudgetType.valueOf(rs.getString("budget_type"))
//            );
//        });
//    }
//}
