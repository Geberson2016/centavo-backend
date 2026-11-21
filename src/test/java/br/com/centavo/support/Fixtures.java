package br.com.centavo.support;

import br.com.centavo.entity.Account;
import br.com.centavo.entity.Category;
import br.com.centavo.entity.User;
import br.com.centavo.enums.AccountType;
import br.com.centavo.enums.BudgetType;
import br.com.centavo.enums.TransactionType;
import br.com.centavo.exception.ApiException;
import br.com.centavo.exception.ErrorCode;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Entidades já com id, para os testes de unidade (sem banco) e de controller.
public final class Fixtures {

    private Fixtures() {
    }

    public static User user(long id, String email) {
        User user = new User("Ana", email, null, "hashed-password");
        // User não expõe setId: o id só nasce no banco.
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static Account account(long id, AccountType type, User owner) {
        Account account = new Account("Account " + id, type, owner);
        account.setId(id);
        return account;
    }

    public static Category category(long id, TransactionType type, User owner) {
        Category category = new Category("Category " + id, type, owner, BudgetType.VARIABLE);
        category.setId(id);
        return category;
    }

    // Afirma que a chamada lança ApiException com o code esperado.
    public static void assertApiError(ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(expected));
    }
}
