-- Создание таблицы users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы agents
CREATE TABLE agents (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы slas
CREATE TABLE slas (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    response_time_hours INTEGER NOT NULL CHECK (response_time_hours > 0),
    resolution_time_hours INTEGER NOT NULL CHECK (resolution_time_hours > 0),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы categories
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    sla_id BIGINT REFERENCES slas(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы tickets
CREATE TABLE tickets (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    agent_id BIGINT REFERENCES agents(id) ON DELETE SET NULL,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    solution TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    response_deadline TIMESTAMP,
    resolution_deadline TIMESTAMP,
    closed_at TIMESTAMP,
    CONSTRAINT valid_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'ON_HOLD', 'RESOLVED', 'CLOSED', 'CANCELLED', 'REOPENED'))
);

-- Создание индексов для ускорения поиска
CREATE INDEX idx_tickets_user_id ON tickets(user_id);
CREATE INDEX idx_tickets_agent_id ON tickets(agent_id);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_category_id ON tickets(category_id);
CREATE INDEX idx_tickets_deadlines ON tickets(response_deadline, resolution_deadline);

-- Таблица для эскалации тикетов
CREATE TABLE ticket_escalations (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    escalated_from_agent_id BIGINT REFERENCES agents(id),
    escalated_to_agent_id BIGINT REFERENCES agents(id),
    reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Таблица для истории изменения статусов
CREATE TABLE ticket_status_history (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    changed_by VARCHAR(100),
    change_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);