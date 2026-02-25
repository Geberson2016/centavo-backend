package br.com.centavo;

import br.com.centavo.dao.AccountDAO;
import br.com.centavo.dao.CategoryDAO;
import br.com.centavo.dao.TransactionDAO;
import br.com.centavo.dao.UserDAO;
import br.com.centavo.enums.AccountType;
import br.com.centavo.enums.BudgetType;
import br.com.centavo.enums.TransactionType;
import br.com.centavo.model.Account;
import br.com.centavo.model.Category;
import br.com.centavo.model.Transaction;
import br.com.centavo.model.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@SpringBootApplication
public class CentavoApplication {

	public static void main(String[] args) {
        SpringApplication.run(CentavoApplication.class, args);
	}
    @Bean
    CommandLineRunner testDatabase(UserDAO userDAO, AccountDAO accountDAO, CategoryDAO categoryDAO, TransactionDAO transactionDAO) {
        return args -> {
            System.out.println("\n=== INICIANDO TESTE COMPLETO DO CENTAVO ===");

            // 1. Criar Usuário
            User guto = new User(null, "Guto Mendes");
            userDAO.save(guto);
            Long gutoId = userDAO.findAll().get(0).getId();
            System.out.println("-> Usuário criado: " + guto.getName() + " (ID: " + gutoId + ")");

            User geberson = new User(null, "Geberson Antonio");
            userDAO.save(geberson);
            Long gebersonId = userDAO.findAll().get(1).getId();
            System.out.println("-> Usuário criado: " + geberson.getName() + " (ID: " + gebersonId + ")");

            // 2. Criar Categoria GLOBAL (Sistema)
            Category catGlobal = new Category(null, null, "Alimentação", TransactionType.DESPESA, BudgetType.FIXO);
            categoryDAO.save(catGlobal);
            System.out.println("-> Categoria Global criada: " + catGlobal.getName());

            // 3. Criar Categoria PERSONALIZADA (do Guto)
            Category catGuto = new Category(null, gutoId, "Salario Kyros", TransactionType.RECEITA, BudgetType.FIXO);
            categoryDAO.save(catGuto);
            System.out.println("-> Categoria do Usuário criada: " + catGuto.getName());

            Category catGeber = new Category(null, gebersonId, "Financiamento Caixa", TransactionType.DESPESA, BudgetType.VARIAVEL);
            categoryDAO.save(catGeber);
            System.out.println("-> Categoria do Usuário criada: " + catGeber.getName());

            // 4. Criar Conta
            Account conta = new Account(null, gutoId, "Banco Inter", AccountType.CARTAO_CREDITO);
            accountDAO.save(conta);
            System.out.println("-> Conta criada: " + conta.getName());

            // 1. Pegamos os IDs necessários (buscando do banco)
            Long contaId = accountDAO.findAll().get(0).getId();
            Long categoriaId = categoryDAO.findAll().get(0).getId();

            // 2. Criamos a transação com LocalDate
            Transaction t1 = new Transaction(
                    null,
                    contaId,
                    categoriaId,
                    java.time.LocalDate.now(),
                    new BigDecimal("55.90"),
                    "Assinatura Netflix",
                    TransactionType.DESPESA
            );
            Transaction t2 = new Transaction(
                    null,
                    contaId,
                    categoryDAO.findAll().get(1).getId(),
                    java.time.LocalDate.now(),
                    new BigDecimal("8239.90"),
                    "salario",
                    TransactionType.RECEITA
            );
            transactionDAO.save(t1);
            transactionDAO.save(t2);


        };
    }
}
