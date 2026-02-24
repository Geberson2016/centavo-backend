package br.com.centavo.dao;

import br.com.centavo.enums.AccountType;
import br.com.centavo.model.Account;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AccountDAO {
    private final JdbcTemplate jdbcTemplate;

    public AccountDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Account account) {
        String sql = "INSERT INTO accounts (user_id, name, type) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, account.getUserId(), account.getName(), account.getType().name());
    }

    public List<Account> findAll() {
        String sql = "SELECT * FROM accounts";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new Account(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("name"),
                        AccountType.valueOf(rs.getString("type"))
                )
        );
    }
}
