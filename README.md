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

## Безопасность (Spring Security)

### Роли

| Роль         | Описание |
|--------------|----------|
| `ROLE_USER`  | Обычный пользователь. Создаёт тикеты, просматривает свои тикеты, категории и SLA. |
| `ROLE_AGENT` | Агент поддержки. Управляет тикетами, просматривает пользователей и отчёты. |
| `ROLE_ADMIN` | Полный доступ ко всем операциям. |

### Матрица доступа

| Эндпоинт | USER | AGENT | ADMIN |
|----------|------|-------|-------|
| `POST /api/auth/register` | ✅ | ✅ | ✅ |
| `GET /api/slas/**` | ❌ | ✅ | ✅ |
| `POST/PUT/DELETE /api/slas/**` | ❌ | ❌ | ✅ |
| `GET /api/categories/**` | ❌ | ✅ | ✅ |
| `POST/PUT/DELETE /api/categories/**` | ❌ | ❌ | ✅ |
| `GET /api/users/**` | ❌ | ✅ | ✅ |
| `POST/PUT/DELETE /api/users/**` | ❌ | ❌ | ✅ |
| `GET /api/agents/**` | ❌ | ✅ | ✅ |
| `POST/PUT/DELETE /api/agents/**` | ❌ | ❌ | ✅ |
| `POST /api/tickets` | ✅ | ❌ | ✅ |
| `GET /api/tickets/**` | ✅ | ✅ | ✅ |
| `PUT /api/tickets/**` | ❌ | ✅ | ✅ |
| `DELETE /api/tickets/**` | ❌ | ❌ | ✅ |
| `GET /api/reports/**` | ❌ | ✅ | ✅ |

### Аутентификация — Basic Auth

Все запросы (кроме регистрации) требуют заголовка:
```
Authorization: Basic <base64(username:password)>
```

В Postman: вкладка **Authorization → Basic Auth** → введите username и password.

### Регистрация пользователей

```
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "username": "admin",
  "password": "Admin123!",
  "role": "ROLE_ADMIN"
}
```

Доступные роли: `ROLE_USER`, `ROLE_AGENT`, `ROLE_ADMIN`.

#### Требования к паролю

- Минимум **8 символов**
- Хотя бы одна **заглавная буква** (A–Z)
- Хотя бы одна **цифра** (0–9)
- Хотя бы один **спецсимвол** (`!@#$%^&*` и др.)

Примеры плохих паролей → ответ `400 Bad Request`:
- `simple` — слишком короткий
- `password1` — нет заглавной и спецсимвола
- `Password1` — нет спецсимвола

### CSRF-токены

Spring Security защищает изменяющие запросы (POST/PUT/DELETE) CSRF-токеном.

**Шаг 1.** Сделайте любой GET-запрос с Basic Auth — в ответе придёт cookie `XSRF-TOKEN`.

**Шаг 2.** Для каждого POST/PUT/DELETE добавьте заголовок:
```
X-XSRF-TOKEN: <значение из cookie XSRF-TOKEN>
```

В Postman это можно автоматизировать скриптом в **Tests** вкладке GET-запроса:
```javascript
const token = pm.cookies.get('XSRF-TOKEN');
pm.environment.set('xsrf_token', token);
```
Затем в заголовках других запросов: `X-XSRF-TOKEN: {{xsrf_token}}`.

### Хранение паролей

Пароли хранятся в таблице `app_users` в виде BCrypt-хэша. В коде и скриптах паролей нет.

---

## Коллекция запросов

Все запросы (CRUD + бизнес-операции) с примерами тел и полным сценарием находятся в файле `postman_collection.json`.

Импорт в Postman: **File → Import → выбрать `postman_collection.json`**.
