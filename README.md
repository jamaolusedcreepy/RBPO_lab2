# Support Ticket System

REST API сервис для управления тикетами технической поддержки.

## Тема

Система обработки заявок (тикетов) в службе поддержки. Пользователи создают тикеты с описанием проблемы, агенты принимают и решают их. Каждая категория тикетов имеет привязанное SLA (соглашение об уровне обслуживания), которое определяет дедлайны ответа и решения.

## Основные сущности

| Сущность   | Таблица      | Описание |
|------------|-------------|----------|
| `User`     | `users`     | Пользователи, создающие тикеты. Email уникален. |
| `Agent`    | `agents`    | Агенты поддержки, обрабатывающие тикеты. Email уникален. Может быть активен/неактивен. |
| `SLA`      | `slas`      | Соглашение об уровне обслуживания: время первого ответа и время решения в часах. Название уникально. |
| `Category` | `categories`| Категория тикета (например, Infrastructure, Security). Привязана к одному SLA. Название уникально. |
| `Ticket`   | `tickets`   | Заявка пользователя. Связана с User, Agent, Category. Имеет статус, дедлайны и историю решения. |

### Связи между таблицами

```
SLA         ──1:1──  Category
Category    ──1:N──  Ticket
User        ──1:N──  Ticket
Agent       ──1:N──  Ticket
```

## Настройка и запуск

### Требования
- Java 21
- Maven 3.8+
- PostgreSQL 14+

### Переменные окружения

Скопируйте `.env.example` в `.env` и заполните значения:

```bash
DB_URL=jdbc:postgresql://localhost:5432/supportdb
DB_USERNAME=postgres
DB_PASSWORD=your_password
SERVER_PORT=8080
```

Создайте базу данных PostgreSQL:
```sql
CREATE DATABASE supportdb;
```

### Запуск

```bash
mvn spring-boot:run
```

При первом запуске база данных автоматически заполняется тестовыми данными:
- 4 SLA (Critical, High, Medium, Low)
- 4 Category (Infrastructure, Security, Software, General Support)
- 3 User (Alice, Bob, Carol)
- 3 Agent (Max, Anna, Pete)
- 8 Ticket в различных статусах

## Операции сервиса

### CRUD по каждой сущности

| Метод  | Путь                        | Описание |
|--------|-----------------------------|----------|
| POST   | `/api/users`                | Создать пользователя |
| GET    | `/api/users`                | Все пользователи |
| GET    | `/api/users/{id}`           | Пользователь по ID |
| PUT    | `/api/users/{id}`           | Обновить пользователя |
| DELETE | `/api/users/{id}`           | Удалить пользователя |
| POST   | `/api/agents`               | Создать агента |
| GET    | `/api/agents`               | Все агенты |
| GET    | `/api/agents/active`        | Только активные агенты |
| GET    | `/api/agents/{id}`          | Агент по ID |
| PUT    | `/api/agents/{id}`          | Обновить агента |
| DELETE | `/api/agents/{id}`          | Удалить агента |
| POST   | `/api/slas`                 | Создать SLA |
| GET    | `/api/slas`                 | Все SLA |
| GET    | `/api/slas/{id}`            | SLA по ID |
| PUT    | `/api/slas/{id}`            | Обновить SLA |
| DELETE | `/api/slas/{id}`            | Удалить SLA |
| POST   | `/api/categories`           | Создать категорию |
| GET    | `/api/categories`           | Все категории |
| GET    | `/api/categories/{id}`      | Категория по ID |
| PUT    | `/api/categories/{id}`      | Обновить категорию |
| DELETE | `/api/categories/{id}`      | Удалить категорию |
| POST   | `/api/tickets`              | Создать тикет |
| GET    | `/api/tickets`              | Все тикеты |
| GET    | `/api/tickets/{id}`         | Тикет по ID |
| GET    | `/api/tickets/user/{userId}`| Тикеты пользователя |
| GET    | `/api/tickets/agent/{agentId}` | Тикеты агента |
| GET    | `/api/tickets/overdue`      | Просроченные тикеты |
| PUT    | `/api/tickets/{id}`         | Обновить тикет |
| PUT    | `/api/tickets/{id}/assign`  | Назначить агента |
| PUT    | `/api/tickets/{id}/status`  | Изменить статус |
| PUT    | `/api/tickets/{id}/close`   | Закрыть тикет с решением |
| DELETE | `/api/tickets/{id}`         | Удалить тикет |

### Бизнес-операции

| №  | Метод | Путь | Затронутые сущности | Описание |
|----|-------|------|---------------------|----------|
| 1  | POST  | `/api/tickets/{id}/auto-assign`              | Ticket + Agent            | Автоматически назначает тикет на активного агента с наименьшей текущей нагрузкой |
| 2  | PUT   | `/api/tickets/{id}/escalate?categoryId={id}` | Ticket + Category + SLA   | Эскалирует тикет: меняет категорию и пересчитывает дедлайны SLA |
| 3  | PUT   | `/api/agents/{id}/deactivate?reassignToAgentId={id}` | Agent + Ticket  | Деактивирует агента и переназначает все его активные тикеты на другого агента |
| 4  | GET   | `/api/reports/categories`                    | Category + Ticket + SLA   | Статистика по категориям: количество тикетов в каждом статусе + параметры SLA |
| 5  | PUT   | `/api/tickets/{id}/reopen?assignToAgentId={id}` | Ticket + Category + SLA + Agent | Повторно открывает закрытый/отменённый тикет и пересчитывает SLA-дедлайны от текущего времени |

### Дополнительный отчёт

| Метод | Путь                   | Описание |
|-------|------------------------|----------|
| GET   | `/api/reports/agents`  | Нагрузка на агентов: кол-во активных/решённых/закрытых тикетов |

## Статусная машина тикетов

```
OPEN → IN_PROGRESS → RESOLVED → CLOSED
  ↓         ↓              ↓
CANCELLED  ON_HOLD      REOPENED
  ↓         ↓              ↓
REOPENED  IN_PROGRESS  IN_PROGRESS
```

## Коллекция запросов

Все запросы (CRUD + бизнес-операции) с примерами тел и полным сценарием находятся в файле `postman_collection.json`.

Импорт в Postman: **File → Import → выбрать `postman_collection.json`**.
