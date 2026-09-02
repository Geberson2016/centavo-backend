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

## Arquitetura

Fluxo clássico em camadas — `controller → service → repository → entity`, DTOs como `record`s imutáveis (`*Request`, `*Response`, `*Projection`), injeção por construtor em tudo.

- **A segurança é JWT stateless.** O `config/JwtAuthFilter` valida o token `Bearer` e popula o `SecurityContext`; o `config/SecurityConfig` define a filter chain, o CORS e o `BCryptPasswordEncoder`. Só `/api/v1/users/register` e `/api/v1/users/login` são públicas.
- **`AuthUtils.getAuthenticatedUser()`** é como os services obtêm o `User` atual a partir do security context.
- **Escopo por usuário é um invariante de segurança.** Toda query é escopada ao usuário autenticado via finders como `findByIdAndUserId`, `findAllByAccountUserId`, `findByIdAndAccountUserId`. Preserve isso em toda nova leitura/escrita — nunca retorne dados de outro usuário.
- **O mapeamento de DTO é manual** dentro dos services (entity → `*Response`), e endpoints de leitura pesada usam tipos de interface `*Projection`.
- **Entidades**: `User`, `Account`, `Category`, `Transaction` (`@ManyToOne` para Account + Category; `value` é `BigDecimal(19,2)`; `type` é um enum `@Enumerated(STRING)`).

### Problemas conhecidos a corrigir (rastreados no AGENTS.md)
- Enums/mensagens em português vazando para o contrato do FE (tabela PT→EN em `AGENTS.md §4`).
- `RuntimeException` cru em vez de exceptions tipadas + handler global.
- Segredo do JWT + credenciais de banco hardcoded no `application.properties`.
- `schema.sql` divergiu das entidades + `ddl-auto=update` → migrar para Flyway (`validate`).
- `data-jpa` e `data-jdbc` juntos no classpath → manter só JPA.
- `@CrossOrigin` por controller duplicando o CORS do `SecurityConfig` → centralizar.

## Layout de pacotes

`br.com.centavo` → `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `enums/`, `config/`, `util/`. Todos os endpoints ficam sob `/api/v1`.
