package br.com.centavo.service;

import br.com.centavo.dto.AccountRequest;
import br.com.centavo.dto.AccountResponse;
import br.com.centavo.entity.Account;
import br.com.centavo.entity.User;
import br.com.centavo.repository.AccountRepository;
import br.com.centavo.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public AccountResponse create(AccountRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

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

    public AccountResponse findById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));

        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getUser().getId()
        );
    }

    public List<AccountResponse> findAll() {
        return accountRepository.findAll()
                .stream()
                .map(account -> new AccountResponse(
                        account.getId(),
                        account.getName(),
                        account.getType(),
                        account.getUser().getId()
                ))
                .toList();
    }
}