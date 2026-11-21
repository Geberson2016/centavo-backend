# CLAUDE.md

Este arquivo orienta o Claude Code (claude.ai/code) ao trabalhar neste repositório.

## ⚠️ Leia o AGENTS.md primeiro — ele é obrigatório

**O [`AGENTS.md`](./AGENTS.md) é o regramento obrigatório.** Toda mudança precisa cumpri-lo. Versão curta das regras inegociáveis:

0. **Política de idioma** — inglês só no código e em configs (identificadores, colunas de banco, chaves de `.env`/`docker-compose`, paths/schemas do OpenAPI). **Todo o resto em português**: comentários, commits, docs, `AGENTS.md`, `CLAUDE.md`, README, roadmap, ADRs, descrições de endpoints, `plano.md`.
1. **Código em inglês** — identificadores, enums, colunas de banco. Enums e strings de erro em português são o refactor de maior prioridade (ver a tabela PT→EN em `AGENTS.md §4`).
2. **Modelo de erro** — services lançam **exceptions tipadas com códigos estáveis** (ex.: `ACCOUNT_NOT_FOUND`), tratadas por um `@RestControllerAdvice` que retorna RFC 7807. **Sem `RuntimeException` cru.**
3. **OpenAPI contract-first** — `openapi.yaml` é a fonte da verdade; controllers implementam interfaces geradas.
4. **Sem segredos no código** — segredo do JWT e credenciais de banco via env, não literais.
5. **Bean validation** (`@Valid`) nos DTOs de request; dinheiro continua `BigDecimal`.
6. **Migrações Flyway**; `ddl-auto=validate`, nunca `update`.

O Definition of Done e a justificativa completa estão no `AGENTS.md`.

## Visão geral

O Centavo é um app de finanças pessoais. Topologia: **React (Vite) → BFF (Node/TS) → Spring Boot → Postgres**. Este repositório é o **backend de domínio Spring Boot** — a fonte da verdade das regras de negócio. O frontend vive no repositório irmão `centavo-frontend`.

Stack: Spring Boot 3.4.5, Java 21, Spring Security (JWT stateless via `com.auth0:java-jwt`), Spring Data JPA, PostgreSQL, Lombok.

## Comandos

```bash
./mvnw spring-boot:run       # sobe a API (precisa de Postgres na :5432, db 'centavo')
./mvnw test                  # roda os testes
./mvnw verify                # build completo + testes
./mvnw -Dtest=FooTest test   # uma única classe de teste
./mvnw clean package         # gera o jar
```

Requer JDK 21. Config de banco em `src/main/resources/application.properties`.

## Testes

- **`service/`**: unidade com JUnit 5 + Mockito (sem Spring). `support/Fixtures` cria entidades com id e tem o `assertApiError`.
- **`controller/`**: fatia web com `@WebMvcTest`, herdando de `ControllerTestSupport`, que importa o `SecurityConfig` **real** (rotas públicas, 403 sem token) e o matcher `problem(status, code)` para RFC 7807.
- **`repository/`**: `@RepositoryTest` (`@DataJpaTest` contra Postgres do Testcontainers). O schema vem das migrações do Flyway e é validado pelo Hibernate.
- **`ApiIntegrationTest`**: a pilha inteira (JWT real → Postgres), incluindo o isolamento entre usuários.
- O Testcontainers precisa de **Docker**. Nenhum teste usa o banco de dev: o perfil `test` (`src/test/resources/application-test.properties`) traz o segredo do JWT e o resto do que os testes precisam.
- Sem JDK 21 na máquina, rode num container com acesso ao Docker:

```bash
docker run --rm --network host -v /var/run/docker.sock:/var/run/docker.sock \
  -v "$PWD":/app -w /app -v centavo_centavo-m2:/root/.m2 \
  -e TESTCONTAINERS_HOST_OVERRIDE=localhost maven:3.9-eclipse-temurin-21 mvn -B verify
```

## Arquitetura

Fluxo clássico em camadas — `controller → service → repository → entity`, DTOs como `record`s imutáveis (`*Request`, `*Response`, `*Projection`), injeção por construtor em tudo.

- **A segurança é JWT stateless.** O `config/JwtAuthFilter` valida o token `Bearer` e popula o `SecurityContext`; o `config/SecurityConfig` define a filter chain, o CORS e o `BCryptPasswordEncoder`. Só `/api/v1/users/register` e `/api/v1/users/login` são públicas.
- **`AuthUtils.getAuthenticatedUser()`** é como os services obtêm o `User` atual a partir do security context.
- **Escopo por usuário é um invariante de segurança.** Toda query é escopada ao usuário autenticado via finders como `findByIdAndUserId`, `findAllByAccountUserId`, `findByIdAndAccountUserId`. Preserve isso em toda nova leitura/escrita — nunca retorne dados de outro usuário.
- **O mapeamento de DTO é manual** dentro dos services (entity → `*Response`), e endpoints de leitura pesada usam tipos de interface `*Projection`.
- **Entidades**: `User`, `Account`, `Category`, `Transaction` (`@ManyToOne` para Account + Category; `value` é `BigDecimal(19,2)`; `type` é um enum `@Enumerated(STRING)`).

### Problemas conhecidos
- Sem token (ou com token inválido) a API responde **403 sem corpo**, não 401. O BFF traduz para `401 UNAUTHENTICATED`; mudar aqui exige mudar o BFF junto.

## Layout de pacotes

`br.com.centavo` → `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `enums/`, `config/`, `util/`. Todos os endpoints ficam sob `/api/v1`.

## Contrato OpenAPI

- O contrato vive em `src/main/resources/openapi.yaml`. Endpoint novo ou alterado começa **por ele**.
- No `generate-sources`, o `openapi-generator-maven-plugin` gera as interfaces `br.com.centavo.api.*Api` em `target/generated-sources/openapi` (não versionadas). Os controllers só implementam essas interfaces: paths, `@Valid` e content types vêm delas, então não use `@RequestMapping`/`@GetMapping` nos controllers.
- Os schemas do spec são mapeados (`schemaMappings` no `pom.xml`) para os records de `dto/` e os enums de `enums/`, e não geram models. Schema novo exige um `schemaMapping` novo e um record que bata com o spec.
