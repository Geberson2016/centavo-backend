-- Migra os valores dos enums de dominio de portugues para ingles (AGENTS.md secao 4).
-- Os enums sao persistidos como EnumType.STRING, entao os dados gravados precisam
-- ser reescritos junto com o rename no codigo Java.
--
-- Contexto: o banco de dev foi criado por ddl-auto=update, e o Hibernate gerou check
-- constraints com os valores em portugues (ex.: accounts_type_check). Elas precisam cair
-- ANTES do UPDATE, senao ele falha. Em banco novo (criado pela V1) essas checks nao
-- existem, por isso a remocao e feita por descoberta dinamica: funciona nos dois casos.

-- 1) Remove qualquer CHECK que incida sobre as colunas de enum, seja qual for o nome gerado.
DO $$
DECLARE
    alvo RECORD;
BEGIN
    FOR alvo IN
        SELECT DISTINCT rel.relname AS tabela, con.conname AS constraint_name
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace ns ON ns.oid = rel.relnamespace
        JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY (con.conkey)
        WHERE con.contype = 'c'
          AND ns.nspname = current_schema()
          AND (rel.relname, att.attname) IN (
                ('accounts', 'type'),
                ('categories', 'type'),
                ('categories', 'budget_type'),
                ('transactions', 'type')
              )
    LOOP
        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT IF EXISTS %I', alvo.tabela, alvo.constraint_name);
    END LOOP;
END $$;

-- 2) Reescreve os dados existentes para os valores em ingles.
UPDATE accounts SET type = CASE type
    WHEN 'CORRENTE'       THEN 'CHECKING'
    WHEN 'POUPANCA'       THEN 'SAVINGS'
    WHEN 'INVESTIMENTO'   THEN 'INVESTMENT'
    WHEN 'DINHEIRO'       THEN 'CASH'
    WHEN 'CARTAO_CREDITO' THEN 'CREDIT_CARD'
    ELSE type
END;

UPDATE categories SET type = CASE type
    WHEN 'RECEITA' THEN 'INCOME'
    WHEN 'DESPESA' THEN 'EXPENSE'
    ELSE type
END;

UPDATE categories SET budget_type = CASE budget_type
    WHEN 'FIXO'     THEN 'FIXED'
    WHEN 'VARIAVEL' THEN 'VARIABLE'
    ELSE budget_type
END;

UPDATE transactions SET type = CASE type
    WHEN 'RECEITA' THEN 'INCOME'
    WHEN 'DESPESA' THEN 'EXPENSE'
    ELSE type
END;

-- 3) Recria as checks com os valores em ingles, agora com nome estavel e versionado.
--    Defesa em profundidade: o enum ja e validado no Java, mas o banco tambem barra lixo.
ALTER TABLE accounts
    ADD CONSTRAINT accounts_type_check
    CHECK (type IN ('CHECKING', 'SAVINGS', 'INVESTMENT', 'CASH', 'CREDIT_CARD'));

ALTER TABLE categories
    ADD CONSTRAINT categories_type_check
    CHECK (type IN ('INCOME', 'EXPENSE'));

ALTER TABLE categories
    ADD CONSTRAINT categories_budget_type_check
    CHECK (budget_type IN ('FIXED', 'VARIABLE'));

ALTER TABLE transactions
    ADD CONSTRAINT transactions_type_check
    CHECK (type IN ('INCOME', 'EXPENSE'));
