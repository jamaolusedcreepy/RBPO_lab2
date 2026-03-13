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

## Безопасность (Spring Security + JWT)

### Таблицы безопасности

| Таблица        | Описание |
|----------------|----------|
| `app_users`    | Учётные записи для входа: username, BCrypt-пароль, роль. |
| `user_sessions`| Refresh-сессии: jti (UUID), SHA-256 хэш токена, статус, сроки. |

### Роли

| Роль         | Описание |
|--------------|----------|
| `ROLE_USER`  | Обычный пользователь. Создаёт тикеты, просматривает тикеты/категории/SLA. |
| `ROLE_AGENT` | Агент поддержки. Управляет тикетами, просматривает пользователей и отчёты. |
| `ROLE_ADMIN` | Полный доступ ко всем операциям. |

### Матрица доступа

| Эндпоинт | Без токена | USER | AGENT | ADMIN |
|----------|-----------|------|-------|-------|
| `POST /api/auth/register` | ✅ | ✅ | ✅ | ✅ |
| `POST /api/auth/login`    | ✅ | ✅ | ✅ | ✅ |
| `POST /api/auth/refresh`  | ✅ | ✅ | ✅ | ✅ |
| `GET /api/slas/**` | ❌ | ❌ | ✅ | ✅ |
| `POST/PUT/DELETE /api/slas/**` | ❌ | ❌ | ❌ | ✅ |
| `GET /api/categories/**` | ❌ | ❌ | ✅ | ✅ |
| `POST/PUT/DELETE /api/categories/**` | ❌ | ❌ | ❌ | ✅ |
| `GET /api/users/**` | ❌ | ❌ | ✅ | ✅ |
| `POST/PUT/DELETE /api/users/**` | ❌ | ❌ | ❌ | ✅ |
| `GET /api/agents/**` | ❌ | ❌ | ✅ | ✅ |
| `POST/PUT/DELETE /api/agents/**` | ❌ | ❌ | ❌ | ✅ |
| `POST /api/tickets` | ❌ | ✅ | ❌ | ✅ |
| `GET /api/tickets/**` | ❌ | ✅ | ✅ | ✅ |
| `PUT /api/tickets/**` | ❌ | ❌ | ✅ | ✅ |
| `DELETE /api/tickets/**` | ❌ | ❌ | ❌ | ✅ |
| `GET /api/reports/**` | ❌ | ❌ | ✅ | ✅ |

### Аутентификация — Basic Auth

Поддерживается для обратной совместимости. Все защищённые запросы могут использовать заголовок:
```
Authorization: Basic <base64(username:password)>
```

В Postman: вкладка **Authorization → Basic Auth** → введите username и password.

### Аутентификация — JWT Bearer

Рекомендуемый способ. Все защищённые запросы требуют заголовка:
```
Authorization: Bearer <accessToken>
```

В Postman: вкладка **Authorization → Bearer Token** → вставьте `accessToken`.

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

Spring Security поддерживает CSRF-защиту (актуально при использовании Basic Auth с сессиями).

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

> При использовании JWT Bearer CSRF не требуется — запросы stateless.

### Хранение паролей

Пароли хранятся в таблице `app_users` в виде BCrypt-хэша. В коде и скриптах паролей нет.

### Эндпоинты аутентификации

#### Регистрация
```
POST /api/auth/register
```
```json
{ "username": "admin", "password": "Admin123!", "role": "ROLE_ADMIN" }
```

#### Вход
```
POST /api/auth/login
```
```json
{ "username": "admin", "password": "Admin123!" }
```
Ответ:
```json
{
  "accessToken":  "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType":    "Bearer"
}
```

#### Обновление пары токенов
```
POST /api/auth/refresh
```
```json
{ "refreshToken": "eyJ..." }
```
Возвращает новую пару. Старый refresh-токен становится недействительным (token rotation).

### Токены

| Тип           | Время жизни | Назначение |
|---------------|-------------|------------|
| Access token  | 15 минут    | Доступ к API-эндпоинтам |
| Refresh token | 7 дней      | Получение новой пары токенов |

**Payload access-токена:** `sub` (username), `role`, `type=access`, `iat`, `exp`

**Payload refresh-токена:** `sub` (username), `type=refresh`, `jti` (UUID сессии), `iat`, `exp`

### Управление сессиями

Каждый refresh-токен привязан к записи в таблице `user_sessions`.

| Статус    | Описание |
|-----------|----------|
| `ACTIVE`  | Сессия активна, refresh-токен можно использовать |
| `REVOKED` | Токен использован — сессия отозвана (token rotation) |
| `EXPIRED` | Срок сессии истёк |

Повторное использование отозванного refresh-токена возвращает `401`.

Для контроля сессий в БД:
```sql
SELECT jti, status, created_at, expires_at, last_used_at
FROM user_sessions
ORDER BY created_at DESC;
```

### Требования к паролю

- Минимум **8 символов**
- Хотя бы одна **заглавная буква** (A–Z)
- Хотя бы одна **цифра** (0–9)
- Хотя бы один **спецсимвол** (`!@#$%^&*` и др.)

### Хранение данных

- Пароли — BCrypt-хэш в таблице `app_users`
- Refresh-токены — SHA-256 хэш в таблице `user_sessions`
- В коде и скриптах никаких паролей и токенов нет

---

## TLS / HTTPS

### Структура цепочки сертификатов

```
Root CA  (STS-RootCA, самоподписанный, 10 лет)
  └── Intermediate CA  (STS-IntermediateCA, подписан Root CA, 5 лет)
        └── Server cert  (CN=localhost, подписан Intermediate CA, 1 год)
```

Все сертификаты содержат `OU=Student-<номер_студенческого_билета>`.

### Генерация сертификатов

```bash
# Укажите свой номер студенческого билета
export STUDENT_ID=12345678
export KEYSTORE_PASSWORD=yourStrongPassword

bash generate-certs.sh
```

Созданные файлы (в директории `certs/` — исключены из git):
| Файл | Описание |
|------|----------|
| `certs/sts-root-ca.crt` | Корневой CA — добавить в доверенные |
| `certs/sts-intermediate-ca.crt` | Промежуточный CA |
| `certs/sts-server.crt` | Серверный сертификат |
| `certs/sts-chain.crt` | Полная цепочка |
| `src/main/resources/keystore.p12` | PKCS12 keystore для Spring Boot (не в git) |

### Запуск с HTTPS

```bash
export SSL_ENABLED=true
export SSL_KEY_STORE_PASSWORD=yourStrongPassword
export SERVER_PORT=8443
mvn spring-boot:run
```

Приложение будет доступно по адресу: `https://localhost:8443`

### Добавление Root CA в доверенные

**Windows** (через certlm.msc):
1. Win+R → `certlm.msc`
2. Trusted Root Certification Authorities → All Tasks → Import
3. Выбрать `certs/sts-root-ca.crt`

**macOS:**
```bash
sudo security add-trusted-cert -d -r trustRoot \
  -k /Library/Keychains/System.keychain certs/sts-root-ca.crt
```

**Ubuntu/Debian:**
```bash
sudo cp certs/sts-root-ca.crt /usr/local/share/ca-certificates/sts-root-ca.crt
sudo update-ca-certificates
```

### Переменные окружения для TLS

| Переменная | По умолчанию | Описание |
|------------|-------------|----------|
| `SSL_ENABLED` | `false` | Включить HTTPS |
| `SSL_KEY_STORE_PATH` | `classpath:keystore.p12` | Путь к keystore |
| `SSL_KEY_STORE_PASSWORD` | `changeit` | Пароль keystore |
| `SERVER_PORT` | `8080` | Порт (8443 для HTTPS) |

> Keystore и приватные ключи **никогда** не коммитятся в репозиторий — добавлены в `.gitignore`.

### Настройка Postman для HTTPS

В Postman: **Settings → General → SSL certificate verification → OFF**
(для self-signed сертификатов в dev-среде)

Или добавьте `certs/sts-root-ca.crt` как доверенный CA:
**Settings → Certificates → CA Certificates → Add**

В коллекции переменная `baseUrl` уже установлена на `https://localhost:8443`. Для HTTP замените на `http://localhost:8080`.

---

## CI/CD (GitHub Actions)

### Пайплайн

Файл: `.github/workflows/ci.yml`

При каждом push/PR в ветку `main` запускается:

| Шаг | Описание |
|-----|----------|
| Checkout | Клонирование репозитория |
| Set up JDK 21 | Установка Java |
| Restore keystore | Декодирование keystore из GitHub Secret |
| Compile | `mvn compile` |
| Test | `mvn test` (с PostgreSQL service) |
| Package | `mvn package -DskipTests` |
| Upload artifact | JAR загружается в GitHub Artifacts (хранится 30 дней) |

### GitHub Secrets

Перейдите в **Settings → Secrets and variables → Actions** репозитория и добавьте:

| Secret | Описание |
|--------|----------|
| `KEYSTORE_BASE64` | Base64-кодированный keystore.p12 |
| `KEYSTORE_PASSWORD` | Пароль от keystore |

Получить base64 от keystore:
```bash
# Linux/macOS
base64 -w 0 src/main/resources/keystore.p12

# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("src\main\resources\keystore.p12"))
```

Скопируйте вывод и вставьте как значение секрета `KEYSTORE_BASE64`.

> Приватные ключи, keystore и сертификаты **не хранятся в репозитории**. Только в GitHub Secrets.

---

## Коллекция запросов

Все запросы (CRUD + бизнес-операции + JWT-сценарий) находятся в файле `postman_collection.json`.

Импорт в Postman: **File → Import → выбрать `postman_collection.json`**.

**Быстрый старт:**
1. Выполни `POST /api/auth/register` — создай пользователей
2. Выполни `POST /api/auth/login` — токены сохранятся в переменные коллекции автоматически
3. Все остальные запросы используют `Bearer {{accessToken}}` из переменных
