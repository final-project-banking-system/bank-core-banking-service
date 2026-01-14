# Core Banking Service (Сервис Core Banking)

Core Banking Service — сервис ядра банковской системы.  
Отвечает за управление банковскими счетами, выполнение переводов и операций по балансу, хранение истории транзакций, а 
также за ежедневное начисление процентов.  
Все доменные события публикуются в Kafka через Outbox-паттерн.

---

## Основные возможности

- Создание банковских счетов (для текущего пользователя)
- Просмотр списка счетов и детальной информации по счету
- Получение баланса по счету
- Изменение статуса счета (ACTIVE / BLOCKED / CLOSED)
- Закрытие счета
- Операции пополнения и снятия средств
- Переводы между счетами (с валидацией бизнес-правил)
- История транзакций
- Ежедневное начисление процентов по активным счетам
- Публикация событий в Kafka через Outbox-паттерн
- JWT-валидация через JWKS endpoint Auth Service

---

## Используемые технологии

- Java 17
- Spring Boot 3
- Spring Security (OAuth2 Resource Server, JWT)
- Spring Data JPA
- PostgreSQL
- Liquibase
- Apache Kafka
- MapStruct
- Docker / Docker Compose

---

## Переменные окружения

Пример `.env` файла:

```env
DB_NAME=core_db
DB_USER=core_user
DB_PASSWORD=TO_CHANGE

SPRING_DATASOURCE_URL=jdbc:postgresql://auth_db:5432/core_db
SPRING_DATASOURCE_USERNAME=core_user
SPRING_DATASOURCE_PASSWORD=TO_CHANGE

SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```

## Используемые Kafka топики

- `banking.accounts` — события по счетам (ACCOUNT_CREATED, ACCOUNT_STATUS_CHANGED, ACCOUNT_CLOSED)
- `banking.transactions` — события по транзакциям (DEPOSIT_COMPLETED, WITHDRAWAL_COMPLETED, INTEREST_APPLIED и др.)
- `banking.transfers` — события переводов (TRANSFER_COMPLETED)
- `system.errors` — системные ошибки сервиса (SYSTEM_ERROR)

---

## Как запустить локально

### Запуск через Docker Compose

1. Поднять инфраструктуру и сервисы:

```bash
docker compose up -d
```

2. Проверить логи Core Banking Service:

```bash
docker logs -f core-banking-service
```

## API Endpoints

Все запросы требуют JWT access token.

Заголовок авторизации:

```http
Authorization: Bearer <access_token>
```

### Создать банковский счёт

**POST** `/accounts`

Тело запроса:

```json
{
  "currency": "EUR"
}
```

### Получить список счетов

**GET** `/accounts`

### Получить счет по id

**GET** `/accounts/{id}`

### Получить баланс по счету

**GET** `/accounts/{id}/balance`

### Изменить статус счета

**PATCH** `/accounts/{id}/status`

```json
{
  "status": "BLOCKED"
}
```

### Закрыть счет

**DELETE** `/accounts/{id}`

### Пополнить счет

**POST** `/accounts/{id}/deposit`

```json
{
  "amount": 100.00
}
```

### Снять средства со счета

**POST** `/accounts/{id}/withdraw`

```json
{
  "amount": 50.00
}
```

### Перевод между счетами

**POST** `/transfers`

```json
{
  "fromAccountId": "11111111-1111-1111-1111-111111111111",
  "toAccountId": "22222222-2222-2222-2222-222222222222",
  "amount": 10.00
}
```

### История транзакций по счету

**GET** `/transactions?accountId=<uuid>`

#### Параметры пагинации
(опционально, стандартные параметры Spring `Pageable`):

- `page` — номер страницы (начиная с `0`)
- `size` — размер страницы
- `sort` — сортировка (например: `createdAt,desc`)

#### Пример запроса

```http
GET /transactions?accountId=11111111-1111-1111-1111-111111111111&page=0&size=20&sort=createdAt,desc
Authorization: Bearer <access_token>
```

## Примечания

- Сервис работает в **stateless**-режиме (`SessionCreationPolicy.STATELESS`) и проверяет JWT как **OAuth2 Resource Server**.

- Для операций изменения баланса используются блокировки на уровне базы данных:
    - `PESSIMISTIC_WRITE` — для операций по конкретному счёту
    - `SERIALIZABLE` — транзакция для переводов между счетами  
      (с упорядочиванием блокировок по `UUID` для предотвращения deadlock)

- Liquibase используется как **единственный источник истины** схемы базы данных.

- Доменные события публикуются через **Outbox-паттерн**  
  (таблица `outbox_events` + фоновые задачи отправки в Kafka).

- Ежедневное начисление процентов запускается по cron-расписанию:  
  `0 0 2 * * *` — каждый день в **02:00**.