package br.com.centavo.service;

import br.com.centavo.dto.AccountRequest;
import br.com.centavo.dto.AccountResponse;
import br.com.centavo.entity.Account;
import br.com.centavo.entity.User;
import br.com.centavo.enums.AccountType;
import br.com.centavo.exception.ErrorCode;
import br.com.centavo.repository.AccountRepository;
import br.com.centavo.support.Fixtures;
import br.com.centavo.util.AuthUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static br.com.centavo.support.Fixtures.assertApiError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AuthUtils authUtils;

    @InjectMocks
    private AccountService accountService;

    private User ana;

    @BeforeEach
    void setUp() {
        ana = Fixtures.user(1L, "ana@centavo.test");
        when(authUtils.getAuthenticatedUser()).thenReturn(ana);
    }

    // O userId do request é legado e ignorado: o dono é sempre o usuário autenticado.
    @Test
    void createAssignsTheAuthenticatedUserIgnoringRequestUserId() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(10L);
            return account;
        });

        AccountResponse response = accountService.create(
                new AccountRequest("Checking", AccountType.CHECKING, 999L));

        ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(ana);
        assertThat(response).isEqualTo(new AccountResponse(10L, "Checking", AccountType.CHECKING, 1L));
    }

    @Test
    void findAllListsOnlyTheAuthenticatedUserAccounts() {
        Account savings = Fixtures.account(10L, AccountType.SAVINGS, ana);
        when(accountRepository.findAllByUserId(1L)).thenReturn(List.of(savings));

        List<AccountResponse> accounts = accountService.findAll();

        assertThat(accounts).containsExactly(
                new AccountResponse(10L, savings.getName(), AccountType.SAVINGS, 1L));
    }

    @Test
    void findByIdDoesNotReturnAnotherUsersAccount() {
        when(accountRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.empty());

        assertApiError(() -> accountService.findById(20L), ErrorCode.ACCOUNT_NOT_FOUND);
    }
}
