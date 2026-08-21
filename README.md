# Wazzup Contacts

Веб-приложение для управления контактами Wazzup и автоматического создания
контактов из существующих переписок. Сотрудник входит по номеру телефона,
просматривает полный список, выполняет поиск, добавляет, редактирует и удаляет
контакты.

Приложение работает с двумя интерфейсами Wazzup:

- пользовательский API v3 — сотрудники и контакты;
- технический API v2 — OAuth, выгрузка сообщений и вебхуки новых диалогов.

Тексты сообщений используются только во время обработки выгрузки и не
сохраняются в SQLite. Из файла извлекаются имя, телефон, username, `chat_id` и
тип мессенджера.

## Возможности

- настройка API-ключа Wazzup через интерфейс;
- авторизация по телефону действующего сотрудника;
- загрузка всех контактов с пагинацией по 100 записей;
- отдельный поиск по имени и телефону по полному списку;
- фильтрация контактов по мессенджерам;
- создание, редактирование и удаление контактов;
- сохранение настоящего `chat_id` при редактировании Telegram и MAX;
- OAuth-подключение технического API;
- первоначальное создание контактов из истории переписок;
- защита от повторного создания контактов;
- ежедневная контрольная синхронизация;
- обработка новых диалогов через вебхук `message.add`.

## Стек

| Часть | Технологии |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Web, Spring Data JPA, Spring Security |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS |
| База данных | SQLite |
| Интеграция | Wazzup API v3, Technical API v2, OAuth 2.0 + PKCE |
| Сборка | Maven, npm, Docker Compose |
| Web-сервер | Nginx |

## Как работает синхронизация

```text
Первое подключение
    ├── OAuth технического API
    ├── выгрузка messages_dump с 2020 года
    ├── выделение уникальных собеседников
    └── создание контактов в Wazzup API v3 пачками по 100

Дальнейшая работа
    ├── message.add → создание нового контакта
    └── раз в сутки → контрольная выгрузка последних двух дней
```

Контакт считается существующим при совпадении мессенджера и `chat_id` либо
телефона. Транспорт `tgapi` из технической выгрузки преобразуется в `telegram`.

Для WhatsApp и Viber `chat_id` обычно является номером. Для Telegram и MAX это
внутренний идентификатор; телефон отображается только тогда, когда Wazzup его
передал. Одинаковые имена не объединяются автоматически, потому что они могут
принадлежать разным людям.

## Быстрый запуск через Docker

Требования: Docker Engine и Docker Compose.

```bash
git clone https://github.com/kirzhq/wazzap-form.git
cd wazzap-form
cp .env.example .env
```

Заполнить `.env`:

```dotenv
APP_PORT=8080
WAZZUP_PARTNER_CLIENT_ID=
WAZZUP_PARTNER_EMAIL=
WAZZUP_PARTNER_PASSWORD=
WAZZUP_OAUTH_REDIRECT_URI=http://127.0.0.1
WAZZUP_TOKEN_ENCRYPTION_KEY=
WAZZUP_WEBHOOK_URL=
WAZZUP_SYNC_ENABLED=false
```

`WAZZUP_TOKEN_ENCRYPTION_KEY` должен быть длинной случайной строкой. Он
используется для шифрования OAuth-токенов в SQLite и не должен меняться после
подключения.

```bash
docker compose up -d --build
```

Открыть `http://localhost:8080`, указать пользовательский API-ключ v3, войти по
телефону сотрудника и подключить технический API в настройках.

```bash
docker compose ps
curl http://localhost:8080/api/health
docker compose logs --tail=200
```

Остановка:

```bash
docker compose down
```

SQLite хранится в именованном Docker volume. Команда `docker compose down -v`
удаляет базу, настройки и OAuth-токены, поэтому применяется только для полного
сброса.

## OAuth и адрес перенаправления

Wazzup принимает только зарегистрированный для `client_id` адрес
перенаправления. Для локальной авторизации можно использовать разрешённый адрес:

```dotenv
APP_PORT=80
WAZZUP_OAUTH_REDIRECT_URI=http://127.0.0.1
```

Для сервера следует зарегистрировать публичный HTTPS callback, например:

```dotenv
WAZZUP_OAUTH_REDIRECT_URI=https://contacts.example.ru
```

Frontend принимает `code` и `state`, backend проверяет `state`, обменивает код
на токены и автоматически обновляет access token через refresh token.

## Постоянная синхронизация на сервере

Для мгновенного добавления новых собеседников Wazzup должен иметь доступ к
публичному HTTPS endpoint:

```dotenv
WAZZUP_WEBHOOK_URL=https://contacts.example.ru/api/partner/webhook
WAZZUP_SYNC_ENABLED=true
```

Backend создаёт недостающие подписки `message.add` и
`messages_dump.status_update`. Локальный `127.0.0.1` недоступен серверам Wazzup,
поэтому рабочим вебхукам требуется публичный домен с HTTPS.

## Установка на Ubuntu

Инструкция рассчитана на Ubuntu 22.04 LTS и 24.04 LTS. Java, Maven, Node.js и
SQLite отдельно устанавливать не требуется.

### 1. Установить Docker

```bash
sudo apt update
sudo apt install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}") stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io \
  docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"
```

После добавления в группу `docker` нужно выйти из SSH-сессии и войти заново.

### 2. Установить приложение

```bash
sudo mkdir -p /opt/wazzup-contacts
sudo chown "$USER":"$USER" /opt/wazzup-contacts
git clone https://github.com/kirzhq/wazzap-form.git /opt/wazzup-contacts
cd /opt/wazzup-contacts
cp .env.example .env
nano .env
docker compose up -d --build
```

Если приложение публикуется напрямую на порту 8080:

```bash
sudo ufw allow OpenSSH
sudo ufw allow 8080/tcp
sudo ufw enable
```

Для production рекомендуется поставить перед приложением Caddy или Nginx,
включить HTTPS и проксировать домен на `127.0.0.1:8080`.

### 3. Обновление

```bash
cd /opt/wazzup-contacts
git pull --ff-only
docker compose up -d --build
```

Обычное обновление не удаляет SQLite volume.

## Переменные окружения

| Переменная | Назначение |
|---|---|
| `APP_PORT` | Внешний порт приложения |
| `WAZZUP_PARTNER_CLIENT_ID` | Client ID технической интеграции |
| `WAZZUP_PARTNER_EMAIL` | Логин партнёрского кабинета |
| `WAZZUP_PARTNER_PASSWORD` | Пароль партнёрского кабинета |
| `WAZZUP_OAUTH_REDIRECT_URI` | Зарегистрированный OAuth redirect URI |
| `WAZZUP_TOKEN_ENCRYPTION_KEY` | Ключ шифрования OAuth-токенов |
| `WAZZUP_WEBHOOK_URL` | Публичный endpoint для вебхуков |
| `WAZZUP_SYNC_ENABLED` | Включение фоновой синхронизации |
| `DB_URL` | JDBC URL SQLite для backend |
| `SERVER_PORT` | Внутренний порт backend |

Пользовательский API-ключ v3 задаётся через интерфейс и хранится в SQLite.

## Локальная разработка

Backend:

```bash
cd backend
./mvnw spring-boot:run
```

Frontend:

```bash
cd frontend
npm ci
npm run dev
```

Frontend доступен на `http://localhost:5173`; запросы `/api` проксируются на
backend.

## API приложения

| Метод и путь | Назначение | Доступ |
|---|---|---|
| `GET /api/health` | Проверка backend | Публичный |
| `PUT /api/settings/api-key` | Сохранение API-ключа v3 | Публичный |
| `POST /api/auth/login` | Вход сотрудника | Публичный |
| `POST /api/auth/logout` | Выход | Сессия |
| `GET /api/contacts?name=&phone=` | Контакты и поиск | Сессия |
| `POST /api/contacts` | Создание контакта | Сессия |
| `PATCH /api/contacts/{id}` | Редактирование контакта | Сессия |
| `DELETE /api/contacts/{id}` | Удаление контакта | Сессия |
| `GET /api/partner/status` | Статус технического API | Сессия |
| `GET /api/partner/oauth/start` | Начало OAuth | Сессия |
| `POST /api/partner/oauth/complete` | Завершение OAuth | Сессия |
| `POST /api/partner/sync` | Контрольная синхронизация | Сессия |
| `POST /api/partner/webhook` | Приём событий Wazzup | Публичный |

Документация Wazzup:

- [OAuth](https://wazzup24.ru/help/api/auth-full/);
- [выгрузка сообщений](https://wazzup24.ru/help/api/messages/);
- [вебхуки](https://wazzup24.ru/help/api/webhooks/);
- [контакты API v3](https://wazzup24.ru/help/api-ru/rabota-s-kontaktami/).
