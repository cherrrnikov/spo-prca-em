# СПО ПРЦА/ВПРЦА — «Электро-М»

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6db33f?logo=springboot&logoColor=white)
![SvelteKit](https://img.shields.io/badge/SvelteKit-2.x-ff3e00?logo=svelte&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169e1?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-dc382d?logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ed?logo=docker&logoColor=white)

Веб-система оперативного планирования работы целевой аппаратуры гидрометеорологического космического аппарата **ГГКС «Электро-М»**. Разработана для НПО им. С.А. Лавочкина (Роскосмос) в рамках выпускной квалификационной работы (МАИ, группа М6О-408Б-22).

Система заменяет существующее Windows-приложение на кроссплатформенный веб-интерфейс, совместимый с российскими операционными системами (Astra Linux, RED OS) в рамках требований импортозамещения.

---

## Что умеет система

- Визуальное планирование сеансов работы целевой аппаратуры на временно́й сетке
- Формирование и сохранение ПРЦА (программы работ целевой аппаратуры) и ВПРЦА (временного плана работ)
- Генерация выходных документов: ПР01, ПР03, ПР04, ВП01
- Загрузка входных данных: ИД02, ИД06, КР01, РО02, прогнозные данные (тень, засветка)
- Проверка ограничений в реальном времени при составлении расписания
- Мультивыбор интервалов: drag-выделение, массовое редактирование и удаление
- Ролевая модель доступа (USER / ADMIN) с JWT-аутентификацией и блокировкой по количеству неудачных попыток входа

---

## Архитектура

Система состоит из двух независимых Spring Boot микросервисов, SvelteKit-фронтенда и Nginx в роли обратного прокси.

**auth** (порт 8080) отвечает за аутентификацию: выдаёт JWT-токены, управляет refresh-токенами, кэширует пользователей в Redis, ведёт учёт неудачных попыток входа.

**backend** (порт 8081) содержит всю бизнес-логику: загружает входные данные из БД, принимает планы от фронтенда, сохраняет ПРЦА/ВПРЦА, генерирует выходные документы. Все запросы к backend проходят JWT-валидацию.

**frontend** (порт 3000) — SvelteKit-приложение с серверными proxy-маршрутами. Прокси на сервере добавляет JWT к запросам к backend, так что токен никогда не уходит в браузер напрямую.

**Nginx** принимает все запросы на порту 80 и маршрутизирует: `/api/auth/` → auth, `/api/` → backend, всё остальное → frontend.

---

## Стек

| Слой | Технологии |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, JdbcTemplate |
| Аутентификация | JWT HS256, Redis 7 (кэш пользователей и rate limiting) |
| База данных | PostgreSQL 16, Flyway (миграции) |
| Frontend | SvelteKit 2, Svelte 5 (runes), TypeScript |
| Документация API | SpringDoc OpenAPI (Swagger UI) |
| Тестирование | JUnit 5, Mockito, Testcontainers (89 тестов) |
| Инфраструктура | Docker, Docker Compose, Nginx |

---

## Быстрый старт

### Требования

- Docker 24+ с Docker Compose v2
- Git

### Запуск

```bash
# Клонировать репозиторий
git clone https://github.com/ВАШ_USERNAME/spo-prca-em.git
cd spo-prca-em

# Создать файл окружения и заполнить переменные
cp .env.example .env

# Создать конфиги сервисов
cp auth/src/main/resources/application.yml.example \
   auth/src/main/resources/application.yml
cp backend/src/main/resources/application.yml.example \
   backend/src/main/resources/application.yml

# Запустить
docker compose up --build -d
```

После запуска приложение доступно по адресу **http://localhost**.

### Переменные окружения

Все переменные задаются в файле `.env` (пример — `.env.example`):

| Переменная | Описание | Пример значения |
|---|---|---|
| `POSTGRES_DB` | Имя базы данных | `spo-prca-em` |
| `POSTGRES_USER` | Пользователь PostgreSQL | `admin` |
| `POSTGRES_PASSWORD` | Пароль PostgreSQL | `changeme` |
| `JWT_SECRET` | Секрет для подписи JWT (минимум 32 символа) | `your-secret-key` |
| `JWT_ISSUER` | Издатель токена | `spo-auth-service` |
| `JWT_ACCESS_EXPIRATION` | Время жизни access-токена в мс | `900000` (15 минут) |
| `JWT_REFRESH_EXPIRATION` | Время жизни refresh-токена в мс | `604800000` (7 дней) |
| `MAX_FAILED_ATTEMPTS` | Количество неудачных попыток входа до блокировки | `5` |
| `ACCOUNT_LOCK_DURATION` | Длительность блокировки аккаунта в минутах | `15` |

---

## API

Swagger UI доступен после запуска:

- **Auth:** http://localhost/auth/swagger-ui/
- **Backend:** http://localhost/backend/swagger-ui/

### Auth `/api/auth`

| Метод | Путь | Описание | Доступ |
|---|---|---|---|
| `POST` | `/api/auth/login` | Вход: возвращает access + refresh токены | Все |
| `POST` | `/api/auth/refresh` | Обновить access-токен по refresh-токену | Все |
| `POST` | `/api/auth/logout` | Инвалидировать refresh-токен | Все |
| `GET` | `/api/auth/users` | Список всех пользователей | ADMIN |
| `POST` | `/api/auth/users` | Создать пользователя | ADMIN |
| `PUT` | `/api/auth/users/{id}/roles` | Изменить роли пользователя | ADMIN |

### Backend `/api`

Все эндпоинты требуют валидный JWT в заголовке `Authorization: Bearer <token>`.

| Метод | Путь | Описание |
|---|---|---|
| `GET` | `/api/id02` | Входные данные ИД02 |
| `GET` | `/api/id06` | Входные данные ИД06 |
| `GET` | `/api/kr01` | Входные данные КР01 |
| `GET` | `/api/ro02` | Входные данные РО02 |
| `GET` | `/api/forecast` | Прогнозные данные (тень, засветка, константы) |
| `POST` | `/api/programs` | Сохранить ПРЦА |
| `GET` | `/api/programs` | Получить список сохранённых ПРЦА |
| `POST` | `/api/vp` | Сохранить ВПРЦА |
| `POST` | `/api/pr01/generate` | Сформировать документ ПР01 |
| `POST` | `/api/pr03/generate` | Сформировать документ ПР03 |
| `POST` | `/api/pr04/generate` | Сформировать документ ПР04 |
| `POST` | `/api/vp01/generate` | Сформировать документ ВП01 |

---

## Тесты

89 тестов: unit-тесты с Mockito, контроллерные тесты через MockMvc и интеграционные тесты на реальной БД через Testcontainers.

```bash
# Запустить тесты auth (54 теста)
cd auth && ./mvnw test

# Запустить тесты backend (35 тестов)
cd backend && ./mvnw test
```

| Класс | Тестов | Тип |
|---|---|---|
| `JwtServiceTest` | 10 | Unit |
| `LoginAttemptServiceImplTest` | 11 | Unit, Mockito |
| `RefreshTokenServiceImplTest` | 9 | Unit, Mockito |
| `AuthServiceImplTest` | 8 | Unit, Mockito |
| `AuthControllerTest` | 8 | MockMvc |
| `AuthServiceIntegrationTest` | 7 | Testcontainers |
| `Pr01BuilderServiceImplTest` | 8 | Unit, Mockito |
| `ProgramsServiceImplTest`

| 6 | Unit, Mockito |
| `Vp01BuilderServiceImplTest` | 7 | Unit, Mockito |
| `ProgramsControllerTest` | 8 | MockMvc |
| `ProgramsServiceIntegrationTest` | 5 | Testcontainers |

---

## База данных

Схема обоих сервисов управляется через Flyway-миграции. Таблицы истории разделены: `flyway_schema_history_auth` и `flyway_schema_history_backend`.

Входные таблицы (`id02`, `id06`, `kr01`, `ro02`, `t_constants`, `t_forecast`, `t_shadow`, `t_zasvetka`, `form_in`) наполняются отдельными SQL-скриптами — Hibernate их не трогает. Таблицы `programs_*` и `vp01_*` находятся под управлением Hibernate.

---

## Лицензия

Проект разработан в рамках выпускной квалификационной работы (МАИ, группа М6О-408Б-22).  
Предназначен для использования в НПО им. С.А. Лавочкина.  