# AGENTS.md — Centavo Backend

**Este arquivo é o regramento obrigatório deste repositório.** Todo código (escrito por humano ou agente) precisa cumpri-lo. O `CLAUDE.md` é o guia de operação do Claude Code e aponta para cá. Um `AGENTS.md` irmão, com as mesmas convenções compartilhadas, vive em `centavo-frontend` — mantenha as seções compartilhadas sincronizadas.

O Centavo é um app de finanças pessoais: **React (Vite) → BFF (Node/TS) → Spring Boot → Postgres**. Este repositório é o **backend de domínio Spring Boot** — a fonte da verdade de todas as regras de negócio.

---

## 0. Política de idioma (obrigatória em todo o projeto)

- **Inglês** — APENAS o código em si (identificadores: nomes de variáveis, funções, classes, enums, colunas/tabelas de banco) e configurações/identificadores técnicos (chaves de `docker-compose`, nomes de variáveis de `.env`, paths/`operationId`/nomes de schema no OpenAPI).
- **Português** — TODO o resto de texto humano: comentários no código, mensagens de commit, este `AGENTS.md`, o `CLAUDE.md`, `README`, seções de roadmap, descrições/summaries de endpoints no OpenAPI, ADRs e qualquer `plano.md` / documento de planejamento.
- **Copy para o usuário final** — resolvido a partir de códigos de erro estáveis via i18n (pt-BR + en); nunca strings prontas no código.
- Mensagens de commit seguem Conventional Commits: prefixo técnico (`feat:`, `fix:`…), texto após o prefixo em português.
- **Documentos de planejamento** (planos, roadmaps de execução) vivem em `centavo-frontend/docs/planos/` (local único, cross-repo), em português, com nome descritivo.

---

## 1. Convenções inegociáveis (todo o projeto)

São obrigatórias. Uma mudança que viole qualquer uma delas não está "pronta".

### 1.1 Código em inglês
Identificadores, enums, tabelas/colunas de banco e chaves de configuração ficam em **inglês**. Texto para o usuário aparece *apenas* como **códigos** de erro resolvidos para texto localizado na borda (§1.2) — nunca como strings de português cravadas em services/exceptions. Comentários no código, por outro lado, são em português (§0).
> Violações atuais: enums em português e mensagens de `RuntimeException` em português. Ver a [tabela de migração PT→EN](#4-migração-pten-obrigatória) — é o refactor de maior prioridade.

### 1.2 i18n & modelo de erro
- Services lançam **exceptions de domínio tipadas** carregando um **código estável** (ex.: `ACCOUNT_NOT_FOUND`), nunca uma frase pronta e nunca um `RuntimeException` cru.
- Um único `@RestControllerAdvice` mapeia exceptions → **RFC 7807 `application/problem+json`** com `{ code, status, detail? }`.
- Mensagens localizadas (pt-BR + en) são resolvidas a partir do `code` (via `MessageSource` com base no `Accept-Language`, ou pelo frontend). O contrato na rede carrega o código; humanos veem texto localizado.

### 1.3 API contract-first (OpenAPI)
- `openapi.yaml` é a **fonte única da verdade**, versionado no git. Define todo endpoint que o BFF consome.
- Gere as interfaces Java do servidor a partir do spec (openapi-generator `spring`, `interfaceOnly=true`) e faça os controllers implementá-las; gere o client TS do lado do frontend. Mantenha annotations/DTOs consistentes com o spec.
- Não sobe endpoint que não esteja no spec. Mudança que quebra compatibilidade exige bump de versão.

### 1.4 SOLID (aplicado ao backend — em boa parte já seguido)
- **SRP** — controllers finos (só mapeamento HTTP); cada service dono de uma capacidade de negócio; nada de god-services.
- **DIP** — injeção por construtor em tudo (já é o padrão); dependa de interfaces de repository/service. Introduza uma interface de service quando ajudar a testar ou houver múltiplas implementações — não adicione cerimônia onde não agrega.
- **OCP/ISP/LSP** — interfaces pequenas e focadas; DTOs como `record`s imutáveis; estenda via novos tipos, não via `if/else` espalhado.

### 1.5 Sem segredos no código-fonte
Segredo do JWT e credenciais de banco devem vir de variáveis de ambiente / config do Spring, não de literais no `application.properties`. Forneça um `application-example.properties` (ou `.env.example`); mantenha valores reais fora do git.
> Violações atuais: `api.security.token.secret=chave-super-secreta-12345` e `spring.datasource.username/password=admin` estão hardcoded.

### 1.6 Validação na borda
DTOs de request usam constraints do `jakarta.validation` (`@NotNull`, `@Email`, `@Positive`, …) e os controllers usam `@Valid`. Nunca confie no client. Dinheiro é `BigDecimal` (já correto) — nunca `double`/`float`.

### 1.7 Migrações de banco
- Adotar **Flyway** (ou Liquibase). Definir `spring.jpa.hibernate.ddl-auto=validate` — **não `update`**. Mudanças de schema são arquivos de migração versionados e imutáveis.
- O `schema.sql` mantido à mão está atualmente **fora de sincronia** com as entidades (ex.: `users` só tem `id, name` lá, mas a entidade tem email/phone/password) — deve ser substituído por migrações, não deixado divergindo.

### 1.8 Definition of Done
- [ ] Código em inglês; nenhuma string de português no código; erros são exceptions tipadas com códigos (sem `RuntimeException` cru).
- [ ] Endpoint presente no `openapi.yaml`; DTOs/annotations batem com o spec.
- [ ] DTOs de request validados com `@Valid` + constraints.
- [ ] Nenhum segredo commitado; nova config ligada via env.
- [ ] Mudança de schema sobe como migração Flyway.
- [ ] Teste de unidade para o service; teste de fatia (`@WebMvcTest`) para o controller (ver roadmap).
- [ ] `./mvnw verify` passa; mensagem de commit em Conventional Commits, texto em português.

---

## 2. Arquitetura & convenções do backend

Em camadas, e as partes boas devem ser preservadas:

```
controller/   camada HTTP fina — @RestController, mapeia request → service → ResponseEntity
service/      lógica de negócio; obtém o chamador via AuthUtils.getAuthenticatedUser()
repository/   interfaces do Spring Data; finders escopados por usuário (findByIdAndUserId, …)
entity/       @Entity do JPA (Account, Category, Transaction, User)
dto/          records imutáveis: *Request (entrada), *Response (saída), *Projection (read models)
enums/        enums de domínio (a serem "inglesados" — §4)
config/       SecurityConfig (filter chain do JWT, CORS, BCrypt), JwtAuthFilter
util/         AuthUtils
```

- **Segurança** — JWT stateless. O `JwtAuthFilter` valida o token `Bearer` e popula o `SecurityContext`; `AuthUtils.getAuthenticatedUser()` é como os services obtêm o `User` atual. `/api/v1/users/register|login` são as únicas rotas públicas.
- **Escopo por usuário é um invariante de segurança** — toda leitura/escrita deve ser escopada ao usuário autenticado via finders como `findByIdAndUserId` / `findAllByAccountUserId`. Nunca exponha dados de outro usuário.
- **CORS** — centralize só no `SecurityConfig`. Remova os `@CrossOrigin` por controller (hoje duplicados em alguns); origens vêm de config, não de literais.
- **Persistência** — o pom traz **os dois**: `spring-boot-starter-data-jpa` e `spring-boot-starter-data-jdbc`. Escolha um (JPA é o em uso) e remova o outro para evitar ambiguidade.

---

## 3. Comandos

```bash
./mvnw spring-boot:run     # sobe a API (precisa de Postgres na :5432, db 'centavo')
./mvnw test                # testes de unidade/integração
./mvnw verify              # build completo + testes
./mvnw -Dtest=FooTest test # roda uma única classe de teste
./mvnw clean package       # gera o jar
```

Requer JDK 21. A conexão com o Postgres está em `src/main/resources/application.properties`.

---

## 4. Migração PT→EN (obrigatória)

Enums de domínio, strings de erro e comentários estão em português e vazam para o contrato do frontend. Migrar para inglês. **Isso toca Java + o frontend + o banco** (valores de enum são gravados como `EnumType.STRING`), então sobe como uma mudança coordenada, com migração de dados no Flyway.

**Enums**

| Enum | Português (hoje) | Inglês (alvo) |
|---|---|---|
| `TransactionType` | `RECEITA` | `INCOME` |
| `TransactionType` | `DESPESA` | `EXPENSE` |
| `AccountType` | `CORRENTE` | `CHECKING` |
| `AccountType` | `POUPANCA` | `SAVINGS` |
| `AccountType` | `INVESTIMENTO` | `INVESTMENT` |
| `AccountType` | `DINHEIRO` | `CASH` |
| `AccountType` | `CARTAO_CREDITO` | `CREDIT_CARD` |
| `BudgetType` | `FIXO` | `FIXED` |
| `BudgetType` | `VARIAVEL` | `VARIABLE` |

**Mensagens de erro → códigos** (hoje lançadas dos services como `RuntimeException`)

| Mensagem em português (hoje) | Código de erro (alvo) |
|---|---|
| `E-mail já cadastrado` | `EMAIL_ALREADY_EXISTS` |
| `Conta não encontrada` | `ACCOUNT_NOT_FOUND` |
| `Categoria não encontrada` | `CATEGORY_NOT_FOUND` |
| `Transação não encontrada` | `TRANSACTION_NOT_FOUND` |
| incompatibilidade de tipo categoria/transação | `TRANSACTION_CATEGORY_TYPE_MISMATCH` |

Notas de migração: acompanhe o rename dos enums com um `UPDATE` no Flyway sobre as linhas existentes (`transactions.type`, `accounts.type`, `categories.type`, `categories.budget_type`); atualize os union types do frontend (`'RECEITA' | 'DESPESA'` → `'INCOME' | 'EXPENSE'`) no mesmo PR; adicione bundles de mensagem pt-BR + en indexados pelos novos códigos.

---

## 5. Roadmap & recomendações (projeto novo — ideias a adotar)

**Fundações obrigatórias**
- **Tratamento global de erro** — `@RestControllerAdvice` + exceptions tipadas + RFC 7807 (substitui o `RuntimeException` cru).
- **OpenAPI contract-first** — escrever o `openapi.yaml`, gerar interfaces do servidor + client TS.
- **Migrações Flyway** + `ddl-auto=validate`; aposentar o `schema.sql` que diverge.
- **Externalizar segredos** — segredo do JWT e credenciais do datasource via env; `application-example.properties`.
- **Bean validation** em todos os DTOs de request.
- **Remover o starter de persistência duplicado** (manter JPA) e os `@CrossOrigin` por controller.

**Ferramental / DX**
- **Testes** — JUnit 5 + Mockito para services, `@WebMvcTest` para controllers, **Testcontainers** (Postgres) para testes de repository/integração.
- **Spotless + Checkstyle** para reforçar inglês/formatação no código; ligar no `./mvnw verify`.
- **CI (GitHub Actions)** — build + test no PR; **Dependabot** para atualização de dependências.
- **Docker Compose** — postgres + backend + bff + web para dev local com um comando; um `Dockerfile` do backend.
- **Observabilidade** — `spring-boot-starter-actuator`, logging estruturado em JSON, IDs de request/correlação.

**Ideias de produto (o modelo de dados já dá indícios delas)**
- Transações recorrentes/agendadas (o dashboard já reporta receita/despesa agendada).
- Orçamentos por categoria usando `BudgetType` (FIXED/VARIABLE) com acompanhamento por período.
- Relatórios & exportação CSV/PDF, consultas por período, paginação nas listas de transação.
- Seed de categorias padrão para novos usuários (`categories.user_id` já permite defaults do sistema via NULL).
