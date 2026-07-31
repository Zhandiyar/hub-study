# hub

Единая точка доступа к партнёрским приложениям.

Проект создан отдельно от `app-registry`. App-registry хранит конфигурацию,
а Hub использует её во время обработки запросов и направляет вызов в нужный
партнёрский микросервис по `appId`.

## Что уже реализовано

- Java 21 и Spring Boot 3.5.14;
- virtual threads;
- OAuth2 Resource Server с проверкой JWT;
- service-to-service OAuth2 `client_credentials` для вызова app-registry;
- короткие connect/read timeout для app-registry и token endpoint Keycloak;
- Apache HttpClient 5, настроенный средствами Spring Boot;
- передача correlation ID в app-registry;
- `GET /hub/api/v1/{appId}`;
- обработка статусов приложения `ACTIVE`, `DISABLED`, `MAINTENANCE`;
- безопасный внешний DTO без финансовой конфигурации и `credentialId`;
- безопасное правило `denyAll` для всех ещё не реализованных API;
- RFC 9457 Problem Details;
- correlation/trace ID;
- Logbook с маскированием `token`, `code` и secret-related полей;
- JSON-логи в production;
- Swagger вне production;
- health-check и graceful shutdown.

База данных, JPA и Flyway не добавлены. Hub остаётся stateless-сервисом,
а единственным владельцем конфигурации является `app-registry`.

## Реализованный API

```text
GET /hub/api/v1/{appId}
```

Поведение:

- найденное приложение в любом статусе — `200 OK`;
- приложение не найдено — `404 Not Found`;
- app-registry недоступен — `503 Service Unavailable`.

Ответ содержит `status`: `ACTIVE`, `DISABLED` или `MAINTENANCE`. Для
`MAINTENANCE` также возвращаются сообщения `ru`, `kk`, `en`; нужный язык
выбирает frontend.

Ответ содержит `Cache-Control: no-store`, поэтому изменение статуса в
app-registry не должно оставаться в HTTP-кэше. Финансовая конфигурация,
`paymentWebhookUrl` и `credentialId` наружу не возвращаются.

Конфигурационный GET только сообщает frontend текущее состояние. Будущие
операции с договором и сессией должны дополнительно проверять на backend, что
приложение находится в статусе `ACTIVE`.

## Следующие API

Их контракты нужно утверждать перед реализацией:

```text
GET  /hub/api/v1/groups/{groupId}
GET  /hub/api/v1/{appId}/{contractNumber}
POST /hub/api/v1/{appId}/session/token
POST /hub/api/v1/{appId}/session/code
```

Создание session token/code использует `POST`, потому что операция создаёт
чувствительное и потенциально одноразовое значение. Такие ответы должны иметь
`Cache-Control: no-store`.

## Как Hub обращается в app-registry

Пользовательский JWT не передаётся в app-registry. Hub получает отдельный
service token по `client_credentials` и вызывает:

```text
GET /internal/api/v1/applications/{appId}/configuration
```

В Keycloak нужны:

1. confidential client `hub` с включёнными `Client authentication` и
   `Service accounts`;
2. client role `hub-reader` у клиента `app-registry`;
3. роль `app-registry / hub-reader`, назначенная service account клиента `hub`;
4. audience `app-registry` в service token клиента `hub`.

Это именно **client role**, не realm role. App-registry читает её из:

```text
resource_access.app-registry.roles
```

Входящий пользовательский токен Hub проверяет повторно, даже если токен уже
проверил Gateway. Для текущего конфигурационного GET достаточно валидного JWT
с audience `hub`: отдельную пользовательскую роль мы пока не придумываем без
бизнес-требования. Для будущих операций с договором и сессией потребуется
отдельная проверка права пользователя на конкретные данные.

## Предстоящие шаги

1. Реализовать получение приложений группы.
2. Определить безопасное сопоставление `appId` с партнёрским микросервисом.
3. Утвердить контракт запроса по `contractNumber`.
4. Реализовать session flow с проверкой `sessionMode`.

## Локальный запуск

Требуется JDK 21:

```bash
./gradlew bootRun
```

По умолчанию Hub запускается на `8080` и обращается к App Registry на `8082`.
Поэтому для одновременного локального запуска App Registry нужно передать ему:

```bash
SERVER_PORT=8082 ./gradlew bootRun
```

Swagger UI:

```text
http://localhost:8080/swagger-ui
```

Для production обязательны:

```text
KEYCLOAK_ISSUER_URI
KEYCLOAK_JWK_SET_URI
KEYCLOAK_AUDIENCE
KEYCLOAK_TOKEN_URI
APP_REGISTRY_CLIENT_ID
APP_REGISTRY_CLIENT_SECRET
APP_REGISTRY_BASE_URL
APP_PROBLEMS_BASE_URI
```

Таймауты вызова app-registry можно настроить через:

```text
APP_REGISTRY_CONNECT_TIMEOUT
APP_REGISTRY_READ_TIMEOUT
```
