package br.com.centavo.repository;

import br.com.centavo.dto.AccountSummaryProjection;
import br.com.centavo.entity.Account;
import br.com.centavo.entity.Category;
import br.com.centavo.entity.Transaction;
import br.com.centavo.entity.User;
import br.com.centavo.enums.AccountType;
import br.com.centavo.enums.BudgetType;
import br.com.centavo.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
class AccountRepositoryTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 10);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private AccountRepository repository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User ana;
    private User bob;
    private Account bobAccount;
    private Category bobCategory;

    @BeforeEach
    void setUp() {
        ana = em.persist(new User("Ana", "ana@centavo.test", null, "hash"));
        Account checking = em.persist(new Account("Checking", AccountType.CHECKING, ana));
        Account card = em.persist(new Account("Card", AccountType.CREDIT_CARD, ana));
        em.persist(new Account("Savings", AccountType.SAVINGS, ana));
        Category salary = em.persist(new Category("Salary", TransactionType.INCOME, ana, BudgetType.FIXED));
        Category food = em.persist(new Category("Food", TransactionType.EXPENSE, ana, BudgetType.VARIABLE));

        em.persist(transaction("5000.00", TransactionType.INCOME, checking, salary));
        em.persist(transaction("1500.00", TransactionType.EXPENSE, checking, food));
        em.persist(transaction("120.50", TransactionType.EXPENSE, card, food));

        bob = em.persist(new User("Bob", "bob@centavo.test", null, "hash"));
        bobAccount = em.persist(new Account("Bob checking", AccountType.CHECKING, bob));
        bobCategory = em.persist(new Category("Bob salary", TransactionType.INCOME, bob, BudgetType.FIXED));
        em.persist(transaction("999.99", TransactionType.INCOME, bobAccount, bobCategory));

        em.flush();
        em.clear();
    }

    // LEFT JOIN: conta sem lançamento aparece com zero, e as contas do Bob ficam de fora.
    @Test
    void summaryTotalsEachAccountOfTheUserIncludingEmptyOnes() {
        Map<String, AccountSummaryProjection> byName = repository.findAllWithTotal(ana.getId()).stream()
                .collect(Collectors.toMap(AccountSummaryProjection::getAccountName, Function.identity()));

        assertThat(byName).containsOnlyKeys("Checking", "Card", "Savings");
        assertTotals(byName.get("Checking"), "5000.00", "1500.00");
        assertTotals(byName.get("Card"), "0", "120.50");
        assertTotals(byName.get("Savings"), "0", "0");
        assertThat(byName.get("Card").getAccountType()).isEqualTo(AccountType.CREDIT_CARD);
    }

    @Test
    void accountLookupsAreScopedToTheOwner() {
        assertThat(repository.findByIdAndUserId(bobAccount.getId(), ana.getId())).isEmpty();
        assertThat(repository.findByIdAndUserId(bobAccount.getId(), bob.getId())).isPresent();
        List<Account> anaAccounts = repository.findAllByUserId(ana.getId());
        assertThat(anaAccounts).extracting(Account::getName)
                .containsExactlyInAnyOrder("Checking", "Card", "Savings");
    }

    @Test
    void categoryLookupsAreScopedToTheOwner() {
        assertThat(categoryRepository.findByIdAndUserId(bobCategory.getId(), ana.getId())).isEmpty();
        assertThat(categoryRepository.findByIdAndUserId(bobCategory.getId(), bob.getId())).isPresent();
        assertThat(categoryRepository.findAllByUserId(ana.getId())).extracting(Category::getName)
                .containsExactlyInAnyOrder("Salary", "Food");
    }

    private static void assertTotals(AccountSummaryProjection summary, String revenue, String expense) {
        assertThat(summary.getTotalRevenue()).isEqualByComparingTo(revenue);
        assertThat(summary.getTotalExpense()).isEqualByComparingTo(expense);
    }

    private static Transaction transaction(String value, TransactionType type, Account account, Category category) {
        return new Transaction("Entry", new BigDecimal(value), DATE, type, account, category);
    }
}
