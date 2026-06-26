package br.com.centavo.service;

import br.com.centavo.dto.AccountRequest;
import br.com.centavo.dto.AccountResponse;
import br.com.centavo.dto.AccountSummaryResponse;
import br.com.centavo.entity.Account;
import br.com.centavo.entity.User;
import br.com.centavo.repository.AccountRepository;
import br.com.centavo.util.AuthUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final AuthUtils authUtils;

    public AccountService(AccountRepository accountRepository, AuthUtils authUtils) {
        this.accountRepository = accountRepository;
        this.authUtils = authUtils;
    }

    public AccountResponse create(AccountRequest request) {
        User user = authUtils.getAuthenticatedUser();

        Account account = new Account(
                request.name(),
                request.type(),
                user
        );

        Account savedAccount = accountRepository.save(account);

        return new AccountResponse(
                savedAccount.getId(),
                savedAccount.getName(),
                savedAccount.getType(),
                savedAccount.getUser().getId()
        );
    }

    public List<AccountResponse> findAll() {
        User user = authUtils.getAuthenticatedUser();

        return accountRepository.findAllByUserId(user.getId())
                .stream()
                .map(account -> new AccountResponse(
                        account.getId(),
                        account.getName(),
                        account.getType(),
                        account.getUser().getId()
                ))
                .toList();
    }

    public AccountResponse findById(Long id) {
        User user = authUtils.getAuthenticatedUser();

        Account account = accountRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));

        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getUser().getId()
        );
    }

    public List<AccountSummaryResponse> findAccountsSummary() {
        User user = authUtils.getAuthenticatedUser();

        return accountRepository.findAllWithTotal(user.getId())
                .stream()
                .map(p -> new AccountSummaryResponse(
                        p.getAccountId(),
                        p.getAccountName(),
                        p.getAccountType(),
                        p.getTotal()
                ))
                .toList();
    }
}