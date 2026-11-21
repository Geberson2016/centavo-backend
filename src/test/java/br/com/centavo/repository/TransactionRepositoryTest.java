package br.com.centavo.repository;

import br.com.centavo.dto.RecentTransactionProjection;
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

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
class TransactionRepositoryTest {

    private static final LocalDate MONTH_START = LocalDate.of(2026, 11, 1);
    private static final LocalDate MONTH_END = LocalDate.of(2026, 11, 30);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private TransactionRepository repository;

    private User ana;
    private User bob;
    private Transaction bobIncome;

    // Mês de referência: novembro de 2026. A Ana tem lançamentos nas duas bordas do mês,
    // um antes (outubro) e um agendado (dezembro); o Bob existe para provar o escopo.
    @BeforeEach
    void setUp() {
        ana = em.persist(new User("Ana", "ana@centavo.test", null, "hash"));
        Account checking = em.persist(new Account("Checking", AccountType.CHECKING, ana));
        Account card = em.persist(new Account("Card", AccountType.CREDIT_CARD, ana));
        Category salary = em.persist(new Category("Salary", TransactionType.INCOME, ana, BudgetType.FIXED));
        Category food = em.persist(new Category("Food", TransactionType.EXPENSE, ana, BudgetType.VARIABLE));
        Category housing = em.persist(new Category("Housing", TransactionType.EXPENSE, ana, BudgetType.FIXED));

        persist("Salary", "5000.00", MONTH_START, TransactionType.INCOME, checking, salary);
        persist("Groceries", "120.50", LocalDate.of(2026, 11, 10), TransactionType.EXPENSE, card, food);
        persist("Coffee", "8.00", LocalDate.of(2026, 11, 10), TransactionType.EXPENSE, card, food);
        persist("Rent", "1500.00", MONTH_END, TransactionType.EXPENSE, checking, housing);
        persist("Old groceries", "80.00", LocalDate.of(2026, 10, 31), TransactionType.EXPENSE, card, food);
        persist("Next rent", "1500.00", LocalDate.of(2026, 12, 1), TransactionType.EXPENSE, checking, housing);

        bob = em.persist(new User("Bob", "bob@centavo.test", null, "hash"));
        Account bobCard = em.persist(new Account("Bob card", AccountType.CREDIT_CARD, bob));
        Category bobSalary = em.persist(new Category("Bob salary", TransactionType.INCOME, bob, BudgetType.FIXED));
        Category bobFood = em.persist(new Category("Bob food", TransactionType.EXPENSE, bob, BudgetType.VARIABLE));
        bobIncome = persist("Bob salary", "999.99", MONTH_START, TransactionType.INCOME, bobCard, bobSalary);
        persist("Bob lunch", "50.00", LocalDate.of(2026, 11, 10), TransactionType.EXPENSE, bobCard, bobFood);

        em.flush();
        em.clear();
    }

    @Test
    void sumByTypeCountsOnlyTheUserTransactions() {
        assertThat(repository.sumByType(TransactionType.INCOME, ana.getId())).isEqualByComparingTo("5000.00");
        assertThat(repository.sumByType(TransactionType.EXPENSE, ana.getId())).isEqualByComparingTo("3208.50");
        assertThat(repository.sumByType(TransactionType.INCOME, bob.getId())).isEqualByComparingTo("999.99");
    }

    // O SUM sem linhas volta null; o DashboardService converte para zero.
    @Test
    void sumByTypeIsNullWhenThereIsNothingToSum() {
        User carol = em.persist(new User("Carol", "carol@centavo.test", null, "hash"));

        assertThat(repository.sumByType(TransactionType.EXPENSE, carol.getId())).isNull();
    }

    @Test
    void sumByTypeAndDateBetweenIncludesBothEdgesOfTheMonth() {
        assertThat(repository.sumByTypeAndDateBetween(TransactionType.INCOME, MONTH_START, MONTH_END, ana.getId()))
                .isEqualByComparingTo("5000.00");
        assertThat(repository.sumByTypeAndDateBetween(TransactionType.EXPENSE, MONTH_START, MONTH_END, ana.getId()))
                .isEqualByComparingTo("1628.50");
    }

    @Test
    void creditCardBillSumsOnlyCreditCardExpensesOfTheMonth() {
        BigDecimal bill = repository.sumExpensesByAccountTypeAndDateBetween(
                AccountType.CREDIT_CARD, MONTH_START, MONTH_END, ana.getId());

        assertThat(bill).isEqualByComparingTo("128.50");
    }

    @Test
    void scheduledSumCountsOnlyTransactionsAfterTheMonth() {
        assertThat(repository.sumScheduledByType(TransactionType.EXPENSE, MONTH_END, ana.getId()))
                .isEqualByComparingTo("1500.00");
        assertThat(repository.sumScheduledByType(TransactionType.INCOME, MONTH_END, ana.getId())).isNull();
    }

    @Test
    void recentTransactionsAreNewestFirstAndRespectTheStartDate() {
        List<RecentTransactionProjection> recent = repository.findRecentByUserId(ana.getId(), MONTH_START);

        // Mesma data: o lançamento mais novo (id maior) vem antes.
        assertThat(recent).extracting(RecentTransactionProjection::getDescription)
                .containsExactly("Next rent", "Rent", "Coffee", "Groceries", "Salary");
        assertThat(recent.getFirst().getCategoryName()).isEqualTo("Housing");
        assertThat(recent.getFirst().getType()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void findByIdAndAccountUserIdDoesNotReturnAnotherUsersTransaction() {
        assertThat(repository.findByIdAndAccountUserId(bobIncome.getId(), ana.getId())).isEmpty();
        assertThat(repository.findByIdAndAccountUserId(bobIncome.getId(), bob.getId())).isPresent();
    }

    @Test
    void findAllByAccountUserIdListsOnlyTheUserTransactions() {
        assertThat(repository.findAllByAccountUserId(bob.getId()))
                .extracting(Transaction::getDescription)
                .containsExactlyInAnyOrder("Bob salary", "Bob lunch");
    }

    private Transaction persist(String description, String value, LocalDate date, TransactionType type,
                                Account account, Category category) {
        return em.persist(new Transaction(description, new BigDecimal(value), date, type, account, category));
    }
}
