# Wazzup Contacts

Веб-приложение для работы с контактами Wazzup. Пользователь входит по номеру
телефона сотрудника, после чего может просматривать, искать, создавать,
редактировать и удалять контакты.

## Функциональность

- настройка API-ключа Wazzup;
- авторизация по номеру активного сотрудника;
- сохранение последнего введённого номера;
- загрузка полного списка контактов с пагинацией по 100 записей;
- раздельный поиск по имени и телефону;
- создание контактов для WhatsApp, Telegram, Viber и MAX;
- изменение имени, телефона и мессенджера;
- удаление контактов с подтверждением.

## Стек

| Часть | Технологии |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Web, Spring Data JPA, Spring Security |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS |
| База данных | PostgreSQL 17 |
| Сборка | Maven, npm |
| Запуск | Docker Compose |
| Web-сервер | Nginx |

## Архитектура

```text
Browser
   │
   ▼
Nginx + React
   │ /api
   ▼
Spring Boot
   ├── Wazzup API v3
   └── PostgreSQL
```

Frontend отправляет запросы на относительные адреса `/api`. При разработке
запросы проксирует Vite, в Docker — Nginx. Backend обращается к Wazzup API и
хранит ключ подключения в PostgreSQL.

После успешной проверки номера создаётся HTTP-сессия. Методы работы с контактами
доступны только авторизованному пользователю.

## Запуск через Docker

Требования:

- Docker;
- Docker Compose.

Создать локальный файл окружения:

```bash
cp .env.example .env
```

Собрать и запустить приложение:

```bash
docker compose up --build
```

Приложение будет доступно по адресу:

```text
http://localhost:8080
```

При первом запуске нужно указать API-ключ Wazzup. PostgreSQL использует Docker
volume, поэтому настройки сохраняются после остановки контейнеров.

Остановить приложение:

```bash
docker compose down
```

Остановить приложение и удалить локальную базу:

```bash
docker compose down -v
```

Если порт `8080` занят, можно выбрать другой:

```bash
APP_PORT=18080 docker compose up --build
```

## Переменные окружения

| Переменная | Значение по умолчанию | Назначение |
|---|---|---|
| `APP_PORT` | `8080` | Внешний порт приложения |
| `POSTGRES_DB` | `wazzup_contacts` | Имя базы данных |
| `POSTGRES_USER` | `wazzup_user` | Пользователь PostgreSQL |
| `POSTGRES_PASSWORD` | `wazzup_password` | Пароль PostgreSQL |

Для backend также доступны:

| Переменная | Значение по умолчанию |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/wazzup_contacts` |
| `DB_USERNAME` | `wazzup_user` |
| `DB_PASSWORD` | `wazzup_password` |
| `SERVER_PORT` | `8080` |

## Локальная разработка

Запустить PostgreSQL:

```bash
docker compose up -d postgres
```

Запустить backend:

```bash
cd backend
./mvnw spring-boot:run
```

В другом терминале запустить frontend:

```bash
cd frontend
npm ci
npm run dev
```

Frontend будет доступен на `http://localhost:5173`.

## API приложения

| Метод и путь | Назначение | Доступ |
|---|---|---|
| `GET /api/health` | Проверка состояния backend | Публичный |
| `GET /api/settings` | Проверка наличия API-ключа | Публичный |
| `PUT /api/settings/api-key` | Сохранение API-ключа | Публичный |
| `POST /api/auth/login` | Вход по номеру сотрудника | Публичный |
| `POST /api/auth/logout` | Завершение сессии | Сессия |
| `GET /api/contacts?name=&phone=` | Получение и поиск контактов | Сессия |
| `POST /api/contacts` | Создание контакта | Сессия |
| `PATCH /api/contacts/{id}` | Редактирование контакта | Сессия |
| `PATCH /api/contacts/{id}/name` | Изменение имени | Сессия |
| `DELETE /api/contacts/{id}` | Удаление контакта | Сессия |

## Проверка проекта

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

Docker Compose:

```bash
docker compose config
```

## Безопасность

- `.env` и реальные API-ключи не должны попадать в Git;
- перед развёртыванием необходимо заменить стандартный пароль PostgreSQL;
- backend и PostgreSQL доступны только во внутренней Docker-сети;
- сессионная cookie создаётся с флагами `HttpOnly` и `SameSite=Lax`;
- backend-контейнер запускается от непривилегированного пользователя.
