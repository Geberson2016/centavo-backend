package br.com.centavo.repository;

import br.com.centavo.TestcontainersConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Fatia JPA contra Postgres real (Testcontainers), com o schema criado pelas migrações do
// Flyway e validado pelo Hibernate. Cada teste roda numa transação desfeita no final.
// A mesma anotação em todas as classes faz o Spring reaproveitar o contexto e o container.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@interface RepositoryTest {
}
