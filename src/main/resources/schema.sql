DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS accounts (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(30) NOT NULL -- Ex: 'Corrente', 'Poupanca', 'Dinheiro'
);

CREATE TABLE IF NOT EXISTS categories (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE, -- NULL para categorias padrão do sistema
    name VARCHAR(50) NOT NULL,
    type VARCHAR(10) NOT NULL, -- 'RECEITA' ou 'DESPESA'
    budget_type VARCHAR(10) -- FIXO ou VARIAVEL (Facilita o Relatório)
);

CREATE TABLE IF NOT EXISTS transactions (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    category_id INTEGER NOT NULL REFERENCES categories(id),
    date DATE NOT NULL,
    value DECIMAL(15, 2) NOT NULL,
    description TEXT,
    type VARCHAR(10) NOT NULL -- 'RECEITA' ou 'DESPESA'
);