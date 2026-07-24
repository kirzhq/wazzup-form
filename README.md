# Wazzup Contacts

Web-приложение для авторизации сотрудников и управления контактами через
пользовательский API Wazzup v3.

## Возможности

- настройка API-ключа Wazzup;
- вход по телефону активного сотрудника;
- сохранение последнего введённого телефона;
- серверная сессия после успешного входа;
- загрузка всех контактов с пагинацией Wazzup по 100 записей;
- поиск по имени и телефону во всём списке, а не только на первой странице;
- создание контактов WhatsApp, Telegram, Viber и MAX;
- редактирование имени, телефона и социальной сети;
- удаление контактов с подтверждением.

## Согласованный стек

| Часть | Технологии |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Web, Spring Data JPA, Spring Security |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS |
| База данных | PostgreSQL 17 |
| API приложения | REST/JSON |
| Сборка | Maven, npm, Docker Compose |
| Production frontend | Nginx |

## Архитектура

```text
Браузер :8080
      │
      ▼
Nginx + React
      │ /api/*
      ▼
Spring Boot :8080
      ├──────────────► Wazzup API v3
      │
      └──────────────► PostgreSQL
                         └─ API-ключ приложения
```

Frontend обращается только к относительным адресам `/api`. При локальной
разработке их проксирует Vite, в Docker — Nginx. Backend хранит API-ключ в
PostgreSQL и обращается к Wazzup с заголовком `Authorization: Bearer ...`.

После проверки телефона backend создаёт HTTP-сессию. Все операции с контактами
защищены Spring Security и без действующей сессии возвращают `401`.

## Быстрый запуск через Docker

Требуется только Docker с поддержкой Compose.

1. При желании скопируйте настройки окружения:

   ```bash
   cp .env.example .env
   ```

   Для локального тестового запуска можно использовать значения по умолчанию.
   Для публикации обязательно замените пароль PostgreSQL.

2. Соберите и запустите всё приложение:

   ```bash
   docker compose up --build
   ```

3. Откройте:

   ```text
   http://localhost:8080
   ```

При первом запуске откроется настройка API-ключа. Данные PostgreSQL сохраняются
в Docker volume и не исчезают после обычной остановки контейнеров.

Остановка:

```bash
docker compose down
```

Остановка с удалением локальной базы:

```bash
docker compose down -v
```

## Переменные Docker Compose

| Переменная | По умолчанию | Назначение |
|---|---|---|
| `APP_PORT` | `8080` | Порт приложения на компьютере |
| `POSTGRES_DB` | `wazzup_contacts` | Имя базы |
| `POSTGRES_USER` | `wazzup_user` | Пользователь базы |
| `POSTGRES_PASSWORD` | `wazzup_password` | Пароль базы |

Файлы `.env` исключены из Git. В репозитории хранится только безопасный пример
`.env.example`.

## Локальная разработка без Docker-сборки

PostgreSQL можно запустить отдельно:

```bash
docker compose up -d postgres
```

Backend:

```bash
cd backend
./mvnw spring-boot:run
```

Frontend в другом терминале:

```bash
cd frontend
npm ci
npm run dev
```

Приложение разработки будет доступно на `http://localhost:5173`.

Backend поддерживает переменные:

| Переменная | Значение по умолчанию |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/wazzup_contacts` |
| `DB_USERNAME` | `wazzup_user` |
| `DB_PASSWORD` | `wazzup_password` |
| `SERVER_PORT` | `8080` |

## Основные endpoint приложения

| Метод и путь | Назначение | Доступ |
|---|---|---|
| `GET /api/health` | Проверка backend | Публичный |
| `GET /api/settings` | Статус настройки ключа | Публичный |
| `PUT /api/settings/api-key` | Сохранение API-ключа | Публичный |
| `POST /api/auth/login` | Вход по телефону сотрудника | Публичный |
| `POST /api/auth/logout` | Завершение сессии | Сессия |
| `GET /api/contacts?search=` | Полный список или поиск | Сессия |
| `POST /api/contacts` | Создание контакта | Сессия |
| `PATCH /api/contacts/{id}` | Полное редактирование | Сессия |
| `PATCH /api/contacts/{id}/name` | Изменение только имени | Сессия |
| `DELETE /api/contacts/{id}` | Удаление | Сессия |

## Проверки

Backend:

```bash
cd backend
./mvnw test
```

Frontend:

```bash
cd frontend
npm run lint
npm run build
```

Проверка итоговой конфигурации Compose:

```bash
docker compose config
```

## Безопасность

- реальный API-ключ и `.env` нельзя коммитить;
- перед публикацией следует заменить пароль PostgreSQL;
- API-ключ хранится в локальной базе приложения;
- cookie сессии имеет флаги `HttpOnly` и `SameSite=Lax`;
- production-контейнер backend запускается не от root;
- наружу публикуется только Nginx, PostgreSQL и backend остаются во внутренней
  Docker-сети.
