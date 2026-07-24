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
| База данных | SQLite |
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
   └── SQLite
```

Frontend отправляет запросы на относительные адреса `/api`. При разработке
запросы проксирует Vite, в Docker — Nginx. Backend обращается к Wazzup API и
хранит ключ подключения в локальной базе SQLite.

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

При первом запуске нужно указать API-ключ Wazzup. Файл SQLite хранится в Docker
volume, поэтому настройки сохраняются после остановки контейнеров.

Остановить приложение:

```bash
docker compose down
```

Остановить приложение и удалить локальную базу SQLite:

```bash
docker compose down -v
```

Если порт `8080` занят, можно выбрать другой:

```bash
APP_PORT=18080 docker compose up --build
```

## Установка на Ubuntu

Инструкция рассчитана на Ubuntu 22.04 LTS и 24.04 LTS. Java, Node.js, Maven и
SQLite отдельно устанавливать не требуется — приложение собирается и запускается
в Docker.

### 1. Установить необходимые пакеты

```bash
sudo apt update
sudo apt install -y ca-certificates curl git
```

Удалить конфликтующие пакеты, если они были установлены ранее:

```bash
for pkg in docker.io docker-doc docker-compose docker-compose-v2 podman-docker containerd runc; do
  sudo apt remove -y "$pkg"
done
```

Добавить официальный ключ и репозиторий Docker:

```bash
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}") stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
```

Установить Docker Engine и Docker Compose:

```bash
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io \
  docker-buildx-plugin docker-compose-plugin
```

После установки проверить Docker:

```bash
sudo systemctl enable --now docker
sudo docker run --rm hello-world
sudo docker compose version
```

Чтобы запускать Docker без `sudo`, добавить текущего пользователя в группу
`docker`:

```bash
sudo usermod -aG docker "$USER"
```

После этого необходимо выйти из SSH-сессии и подключиться заново.

### 2. Загрузить проект

```bash
sudo mkdir -p /opt/wazzup-contacts
sudo chown "$USER":"$USER" /opt/wazzup-contacts
git clone https://github.com/kirzhq/wazzap-form.git /opt/wazzup-contacts
cd /opt/wazzup-contacts
```

### 3. Настроить порт

```bash
cp .env.example .env
```

По умолчанию приложение использует порт `8080`. При необходимости значение
можно изменить в `.env`:

```dotenv
APP_PORT=8080
```

Если используется UFW, разрешить выбранный порт:

```bash
sudo ufw allow OpenSSH
sudo ufw allow 8080/tcp
sudo ufw enable
```

### 4. Собрать и запустить приложение

```bash
docker compose up -d --build
```

Проверить состояние контейнеров и backend:

```bash
docker compose ps
curl http://localhost:8080/api/health
```

Открыть приложение в браузере:

```text
http://SERVER_IP:8080
```

При первом запуске необходимо ввести API-ключ Wazzup через интерфейс, а затем
авторизоваться по номеру телефона активного сотрудника.

### 5. Обновить приложение

```bash
cd /opt/wazzup-contacts
git pull --ff-only
docker compose up -d --build
```

SQLite хранится в именованном Docker volume и не удаляется при обычном
обновлении или выполнении `docker compose down`.

Посмотреть журналы приложения:

```bash
docker compose logs --tail=200
```

Остановить приложение:

```bash
docker compose down
```

Команда `docker compose down -v` дополнительно удаляет SQLite. Использовать её
следует только для полного сброса приложения.

## Переменные окружения

| Переменная | Значение по умолчанию | Назначение |
|---|---|---|
| `APP_PORT` | `8080` | Внешний порт приложения |

Для backend доступны:

| Переменная | Значение по умолчанию | Назначение |
|---|---|---|
| `DB_URL` | `jdbc:sqlite:wazzup.db` | Путь к файлу SQLite |
| `SERVER_PORT` | `8080` | Порт backend |

## Локальная разработка

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
- файл SQLite не должен попадать в Git;
- backend доступен только во внутренней Docker-сети;
- сессионная cookie создаётся с флагами `HttpOnly` и `SameSite=Lax`;
- backend-контейнер запускается от непривилегированного пользователя.
