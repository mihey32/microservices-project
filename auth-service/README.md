# Auth Service

Микросервис аутентификации и управления аккаунтами для e-commerce платформы.
JWT (access + refresh), OAuth 2.1 (Google/GitHub), email-верификация, сброс пароля,
события в Kafka (`user.created`, `password.reset-requested`).

## Стек

- **Java 21**, Spring Boot 4.1.1, Spring Security 6, Spring Data JPA, Spring Kafka
- **PostgreSQL 18** (аккаунты, токены), **Redis 7** (rate-limit), **Kafka 3.8** (события)
- **Testcontainers** (интеграционные тесты), **JaCoCo** (coverage), **springdoc-openapi** (Swagger)

## Быстрый старт (из чистого клона)

```bash
git clone git@github.com:mihey32/microservices-project.git
cd microservices-project/auth-service

# 1. Создать .env из шаблона
cp .env.example .env

# 2. Сгенерировать JWT_SECRET и вставить в .env
openssl rand -base64 32
#    → откройте .env, замените CHANGE_ME_BASE64_32_BYTES на сгенерированное значение

# 3. Поднять стек (Postgres, Redis, Kafka, MailHog, auth-service)
docker compose up --build
```

Через ~60 секунд (первый раз дольше — скачиваются образы и Maven-зависимости):

- **API:** http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **MailHog (письма):** http://localhost:8025

Остановить: `docker compose down`. Остановить с удалением данных БД: `docker compose down -v`.

### OAuth2 (опционально)

Без этих переменных сервис стартует, но `/oauth2/authorization/{provider}` вернёт ошибку.
В `.env` заполните:

- **Google:** https://console.cloud.google.com/apis/credentials → redirect URI `http://localhost:8080/login/oauth2/code/google`
- **GitHub:** https://github.com/settings/developers → callback `http://localhost:8080/login/oauth2/code/github`

## Запуск из IDE

Если не хотите Docker для самого сервиса — поднимите только инфраструктуру:

```bash
docker compose up -d db redis kafka mailhog
```

Затем в IntelliJ IDEA:

- **Run → Edit Configurations → Environment variables** — добавьте переменные из `.env`
  (`JWT_SECRET`, `SPRING_DATASOURCE_*`, `SPRING_KAFKA_*`, `SPRING_DATA_REDIS_HOST`)
- **Active profiles:** `dev`
- Run `AuthServiceApplication`

Или из терминала:

```bash
set -a && source .env && set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Тесты

```bash
./mvnw clean test
```

- **Testcontainers** автоматически поднимает `postgres:18-alpine`, `redis:7-alpine`, `apache/kafka-native:3.8.0`
- Docker Desktop должен быть запущен
- Первый прогон ~2–3 мин (скачивание образов), последующие ~60 с
- **Coverage:** `target/site/jacoco/index.html` (JaCoCo, HTML-отчёт)

Запуск одного класса: `./mvnw test -Dtest=AuthServiceKafkaTest`.

## Структура

```
src/main/java/com/ecommerce/auth_service/
├── config/         — SecurityConfig, OpenApiConfig, RateLimitConfig
├── controller/     — AuthController (REST)
├── dto/            — request/response, события Kafka
├── exceptions/     — доменные исключения + GlobalExceptionHandler
├── mapper/         — Account ↔ AccountDto
├── model/          — JPA entities (Account, RefreshToken, ...)
├── repository/     — Spring Data JPA
├── security/       — JwtAuthenticationFilter, RateLimitFilter, OAuth2 handlers
└── service/        — AccountService, AuthService, JwtService, RefreshTokenService, ...
```

## События Kafka

| Topic | Producer | Consumer(s) | Назначение |
|---|---|---|---|
| `user.created` | auth-service | user-service | Создание локального пользователя |
| `password.reset-requested` | auth-service | notification-service | Отправка письма со ссылкой сброса |

## Известные ограничения (MVP)

- Kafka-шаблон **не транзакционный** — at-least-once без outbox-паттерна
- Rate-limit `5/min` на IP+URI — при появлении gateway переедет туда
- Avro/Schema Registry — в плане (сейчас JSON)
- Flyway/Liquibase — в плане (сейчас `ddl-auto=validate` в prod)

## Полезные команды

```bash
# Логи только auth-service
docker compose logs -f auth_service

# Зайти в контейнер
docker compose exec auth_service sh

# Пересобрать образ после изменения кода
docker compose up --build auth_service

# Проверить compose без запуска
docker compose config
```