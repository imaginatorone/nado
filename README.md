<p align="center">
  <img src="frontend/public/logo.png" width="120" alt="Nado">
</p>

<h1 align="center">Nado</h1>
<p align="center"><b>Только то, что надо - и ничего лишнего.</b></p>
<p align="center">Электронная доска объявлений</p>

---

## О проекте

**Nado** - полноценная платформа электронных объявлений, разработанная в рамках курса SolarLab. Пользователи могут размещать, искать и покупать товары, участвовать в аукционах, создавать поисковые запросы «Хочу купить», общаться с продавцами в реальном времени (WebSocket) и получать уведомления через in-app и email каналы.

Проект реализован как production-ready SPA с акцентом на чистую архитектуру, безопасность, модульность и мобильную доступность (PWA).

## Ключевые возможности

- **Объявления** - CRUD с фотографиями, категориями, полнотекстовым поиском (FTS), фильтрацией по региону и цене
- **Жизненный цикл** - черновик → модерация → публикация → продано / архив / блокировка
- **Аукционы** - live-торги с обратным отсчётом, авто-продлением, снайпер-защитой
- **«Хочу купить»** - rule-based matching engine: создаёшь запрос, получаешь совпадения при публикации новых объявлений
- **Уведомления** - единая платформа (IN_APP + EMAIL): модерация, аукционы, wanted, системные события
- **Чат** - realtime переписка через WebSocket (STOMP/SockJS) с polling fallback, поддержка файлов
- **Admin Dashboard** - аналитика: пользователи, объявления, аукционы, категории, 30-дневный тренд
- **Рейтинг доверия** - многофакторная оценка продавца (заполненность профиля, отзывы, верификация)
- **Модерация** - панель для модераторов с одобрением/отклонением/блокировкой объявлений
- **PWA** - установка на домашний экран, standalone mode, offline-shell
- **Dual-mode Auth** - Keycloak OAuth2 + Google IdP (target) / self-issued JWT (legacy)

## Архитектура

```
┌──────────────┐     REST API      ┌──────────────────────────────────┐
│   React SPA  │ ◄──────────────► │         Spring Boot              │
│   (Vite)     │    JSON / JWT     │                                  │
│              │ ◄── WebSocket ──► │  Controller → Service → Repo     │
└──────────────┘   STOMP/SockJS    │                  ↓               │
                                   │            PostgreSQL            │
        ┌──────────────┐           │       (OAuth2 Resource Server)   │
        │   Keycloak   │ ◄────────│       (+ Google IdP)             │
        │              │           └──────────────────────────────────┘
        └──────────────┘
```

Трёхслойная модель (Controller → Service → Repository) с инверсией зависимостей через конструктор. Никаких `@Autowired` на полях.

## Технологический стек

| Слой               | Технологии                                                             |
| ------------------ | ---------------------------------------------------------------------- |
| **Backend**        | Java 11, Spring Boot 2.7, Spring MVC, Spring Security, Spring Data JPA |
| **Auth**           | Keycloak OAuth2 / self-issued JWT (dual-mode), BCrypt                  |
| **БД**             | PostgreSQL 15, Flyway (22 миграции)                                    |
| **Frontend**       | React 18, Vite 5, Vanilla CSS (glassmorphism, dark theme)              |
| **Инфраструктура** | Docker, Docker Compose, Nginx                                          |
| **PWA**            | Service Worker, Web App Manifest, standalone mode                      |
| **Тестирование**   | JUnit 5, Mockito, H2, ArchUnit (117 тестов)                            |
| **API Docs**       | Swagger / SpringDoc OpenAPI                                            |

## Ролевая модель

| Роль                 | Доступ                                                                               |
| -------------------- | ------------------------------------------------------------------------------------ |
| **Неавторизованный** | Просмотр объявлений, поиск, регистрация                                              |
| **Авторизованный**   | Всё выше + создание объявлений, чат, избранное, аукционы, «Хочу купить», уведомления |
| **Модератор**        | Всё выше + панель модерации (одобрение / отклонение / блокировка)                    |
| **Администратор**    | Полный доступ, управление пользователями                                             |

## Аутентификация и авторизация

Платформа поддерживает два режима, переключаемых через `AUTH_MODE`:

### Keycloak (target)

- Keycloak как Identity Provider с OAuth2/OIDC
- Spring Security Resource Server валидирует JWT от Keycloak
- Realm `nado` с клиентом `nado-frontend`
- Поддержка silent SSO check
- Google IdP настроен (realm config + account linking), VK - extension point

### Legacy JWT (transitional)

- Self-issued JWT через `/api/auth/login`
- `JwtTokenProvider` + `JwtAuthenticationFilter`
- BCrypt для паролей
- Серверная капча (Graphics2D, без внешних зависимостей)

Переключение: `AUTH_MODE=keycloak` или `AUTH_MODE=legacy` в `.env`.

## Жизненный цикл объявления

```
DRAFT → PENDING_MODERATION → PUBLISHED → SOLD / ARCHIVED
                ↓                 ↓
            REJECTED          BLOCKED
                ↓                 ↓
        PENDING_MODERATION   REMOVED
```

- Владелец: draft → submit → (после модерации) → sell / archive / remove
- Модератор: approve / reject (с причиной) / block
- Отклонённое объявление можно отредактировать и повторно отправить
- Каждый переход валидируется через `AdStatusTransitionService` с матрицей допустимых переходов

## Аукционы

- Создание аукциона при публикации объявления (`saleType: AUCTION`)
- Live-обратный отсчёт до `endsAt`
- Минимальный шаг ставки (`bidStep`)
- Авто-продление при ставке в последние 5 минут (снайпер-защита)
- Статусы: `ACTIVE` → `FINISHED` / `NO_BIDS` / `CANCELLED`
- `AuctionOutcome` - отдельная сущность для сделки после торгов
- Уведомления: outbid, won, finished (seller), no bids
- Действия владельца: cancel, extend

## «Хочу купить»

Rule-based matching engine:

1. Пользователь создаёт запрос (query, категория, ценовой диапазон, регион)
2. При публикации нового объявления `WantedMatchingService` проверяет все активные запросы
3. Scoring: категория (+10), цена в диапазоне (+10), регион (+5), keyword overlap (+5)
4. Дедупликация: один матч на пару (request, ad) - на уровне БД и сервиса
5. Автор объявления не матчится со своим запросом
6. Уведомление `WANTED_MATCH` при каждом новом совпадении

## Уведомления

Единая платформа `NotificationService` - все события проходят через `send()`:

| Тип                       | Событие                     | Источник        |
| ------------------------- | --------------------------- | --------------- |
| `AD_APPROVED`             | Объявление одобрено         | Модерация       |
| `AD_REJECTED`             | Объявление отклонено        | Модерация       |
| `AD_BLOCKED`              | Объявление заблокировано    | Модерация       |
| `AUCTION_OUTBID`          | Ставку перебили             | Аукцион         |
| `AUCTION_WON`             | Выиграл аукцион             | Аукцион         |
| `AUCTION_FINISHED_SELLER` | Аукцион завершён (продавцу) | Аукцион         |
| `AUCTION_NO_BIDS`         | Аукцион без ставок          | Аукцион         |
| `WANTED_MATCH`            | Найдено совпадение          | Matching engine |
| `NEW_MESSAGE`             | Новое сообщение             | Extension point |
| `NEW_RATING`              | Новый отзыв                 | Extension point |
| `SYSTEM`                  | Системное                   | Manual          |

Каналы: **IN_APP** (обязательный), **EMAIL** (6 MVP-типов, `emailVerified` gate, Spring Mail).
Дедупликация на уровне SHA-256 canonical payload hash.

## Безопасность и приватность

- Пароли: BCrypt
- JWT: signature validation (Keycloak JWK / self-issued HMAC)
- Email/телефон: скрыты в публичных DTO, доступны только владельцу и при взаимодействии в чате
- Загрузка файлов: MIME whitelist + extension validation через `FileValidationService`
- Капча: серверная генерация через `Graphics2D` (без внешних сервисов)
- Секреты: все credentials через `.env`, не в коде
- CORS: настраиваемый список origins

## Desktop / Mobile / PWA

- **Responsive-first** CSS: все экраны адаптированы под mobile (640px breakpoint)
- **PWA manifest**: installable, standalone mode, 192px + 512px иконки
- **Service Worker**: кеширование статики, SPA-фоллбек для deep links. API и auth не кешируются
- **Safe area**: поддержка notch/gesture bar через `env(safe-area-inset-bottom)`
- **Touch targets**: минимум 44px по Apple HIG
- **Bottom navigation**: адаптивная панель с bell-badge для уведомлений

## Структура проекта

```
nado/
├── backend/
│   ├── src/main/java/com/solarl/nado/
│   │   ├── config/          # конфигурация (Security, CORS, Swagger, Keycloak, DataSeeder)
│   │   ├── controller/      # REST + WS контроллеры (20 шт.)
│   │   ├── dto/             # request/response DTO
│   │   ├── entity/          # JPA-сущности (16 шт.)
│   │   ├── exception/       # глобальный обработчик ошибок
│   │   ├── repository/      # Spring Data репозитории
│   │   ├── security/        # JWT, AuthFacade, фильтры
│   │   └── service/         # бизнес-логика (23 сервиса)
│   └── src/main/resources/
│       └── db/migration/    # Flyway (V1..V22)
├── frontend/
│   ├── src/
│   │   ├── api/             # Axios API layer
│   │   ├── components/      # переиспользуемые компоненты
│   │   ├── context/         # AuthContext (dual-mode)
│   │   └── pages/           # 18 страниц
│   └── public/              # PWA manifest, SW, иконки
├── keycloak/                # realm export для автоматического импорта
├── docker-compose.yml
├── .env.example
└── README.md
```

## Быстрый старт (Docker)

```bash
# 1. Клонируем
git clone https://github.com/your-username/nado.git && cd nado

# 2. Настраиваем окружение
cp .env.example .env
# Отредактируйте .env: задайте JWT_SECRET, POSTGRES_PASSWORD

# 3. Запускаем
docker-compose up -d

# 4. Открываем
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080/api
# Swagger: http://localhost:8080/swagger-ui.html
# Keycloak (если AUTH_MODE=keycloak): http://localhost:8180
```

## Локальный запуск (без Docker)

### Backend

```bash
# Нужен Java 11+, Maven, PostgreSQL
cd backend
mvn spring-boot:run
# API: http://localhost:8080/api
```

### Frontend

```bash
cd frontend
npm install
npm run dev
# Dev server: http://localhost:5173
```

## Переменные окружения

| Переменная           | Описание                   | Значение по умолчанию                         |
| -------------------- | -------------------------- | --------------------------------------------- |
| `AUTH_MODE`          | Режим аутентификации       | `legacy`                                      |
| `POSTGRES_DB`        | Имя базы данных            | `nado`                                        |
| `POSTGRES_USER`      | Пользователь БД            | `nado_user`                                   |
| `POSTGRES_PASSWORD`  | Пароль БД                  | —                                             |
| `JWT_SECRET`         | Ключ для self-issued JWT   | —                                             |
| `KEYCLOAK_ISSUER`    | URL Keycloak realm         | `http://localhost:8180/realms/nado`           |
| `GOOGLE_CLIENT_ID`   | OAuth Client ID (Google)   | — (требует Google Console)                    |
| `GOOGLE_CLIENT_SECRET`| OAuth Secret (Google)     | — (требует Google Console)                    |
| `UPLOAD_DIR`         | Директория загрузки файлов | `./uploads`                                   |
| `CORS_ORIGINS`       | Разрешённые origins        | `http://localhost:3000,http://localhost:5173` |
| `ADMIN_SEED_ENABLED` | Создать admin при старте   | `false`                                       |
| `MAIL_ENABLED`       | Включить email-канал       | `false`                                       |
| `MAIL_FROM`          | Адрес отправителя email    | `noreply@nado.ru`                             |
| `SMTP_HOST`          | SMTP-сервер                | `smtp.gmail.com`                              |

Полный список - в `.env.example`.

## Тесты

```bash
cd backend
mvn test
```

**117 тестов**, 20 тестовых классов:

| Класс                           | Покрытие                                                           |
| ------------------------------- | ------------------------------------------------------------------ |
| `AdServiceTest`                 | поиск, создание, проверка владельца                                |
| `AdStatusTransitionServiceTest` | матрица переходов, аудит, валидация ролей                          |
| `AuctionServiceTest`            | ставки, завершение, no bids, cancel, extend, снайпер-защита        |
| `AuthServiceTest`               | регистрация, дубликаты, автоматический вход                        |
| `NotificationServiceTest`       | send, dedup, convenience-методы                                    |
| `WantedMatchingServiceTest`     | full match, dedup, skip own, skip non-published                    |
| `SecurityContractTest`          | public/protected endpoints, DTO privacy                            |
| `FileValidationServiceTest`     | MIME/extension whitelist                                           |
| `ArchitectureTest`              | ArchUnit: слои, зависимости, именование                            |
| `EmailNotificationServiceTest`  | gates (verified/disabled/type), SMTP failure survival              |
| `ChatServiceSecurityTest`       | room membership, outsider rejection, attachment auth               |
| `FtsSanitizationTest`           | tsquery sanitization, SQL injection, cyrillic, special chars       |
| + 8 других                      | users, categories, comments, ratings, favorites, trust, phone, JWT |

## Что реализовано / Extension points

### Core (работает)

- Полный CRUD объявлений с фото, категориями, регионами
- Ad lifecycle + модерация
- Аукционы (live bidding, auto-extend, outcome tracking)
- «Хочу купить» (rule-based matching)
- Уведомления (единая платформа, 11 типов, IN_APP + EMAIL каналы)
- Чат продавец/покупатель (WebSocket STOMP + polling fallback)
- Полнотекстовый поиск (PostgreSQL FTS, tsvector + GIN, russian config)
- Admin dashboard с аналитикой (users, ads, auctions, categories, 30-day trend)
- Рейтинг доверия (foundation)
- Dual-mode auth (Keycloak / JWT)
- Google IdP через Keycloak (realm config, account linking)
- PWA (installable, offline shell, deep links)
- Серверная капча
- Phone verification flow

### Extension points (подготовлены, не реализованы в MVP)

- SMS-канал уведомлений
- VK IdP через Keycloak
- Payment integration для аукционов

## FAQ

**Почему React, а не JSP/Thymeleaf?**
По ТЗ клиент должен взаимодействовать с сервером через REST API. SPA на React - естественный выбор. Плюс это ближе к реальным продуктовым командам.

**Почему Flyway, а не Liquibase?**
Flyway проще для проекта такого размера. SQL-миграции читаются без абстракций, версионирование через `V1__`, `V2__` интуитивно понятно.

**Почему собственная капча?**
Показать, что задача решается средствами Java без внешних зависимостей. Серверная генерация через `Graphics2D` - хорошая практика для работы с `BufferedImage`.

**Почему Vanilla CSS?**
Полный контроль над стилями. Glassmorphism, адаптивная вёрстка, тёмная тема, PWA - всё написано руками.

**Почему dual-mode auth?**
Keycloak - целевая архитектура для production (OAuth2, SSO, social login). Legacy JWT сохранён для простоты разработки и демонстрации без внешних зависимостей.

## Планы развития

- [x] Полнотекстовый поиск (PostgreSQL FTS, tsvector + GIN, russian config, ranked results)
- [x] Admin dashboard с аналитикой (users, ads, auctions, wanted, categories, 30-day chart)
- [x] Email-уведомления (Spring Mail, emailVerified gate, SMTP через env, graceful fallback)
- [x] Google IdP через Keycloak (realm config с placeholder, account linking, documented setup)
- [x] WebSocket для чата (STOMP/SockJS, dual-mode auth interceptor, polling fallback)
- [ ] Оплата через Stripe/ЮKassa для аукционов
- [ ] Миграция на Java 17 / Spring Boot 3

> **Google IdP:** требуется настройка в Google Cloud Console + Keycloak admin.
> **Email:** SMTP credentials через `.env`, по умолчанию отключен (`MAIL_ENABLED=false`).
> **WebSocket:** auth интегрирован с существующей security model (legacy JWT / Keycloak).

---

<p align="center">
  Разработано в рамках курса <a href="https://solarlab.ru">SolarLab</a>
</p>
