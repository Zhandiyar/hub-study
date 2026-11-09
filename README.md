# Study Hub

> Учебный pet-проект. Репозиторий создан для изучения Java 21, Spring Boot
> и взаимодействия микросервисов. Он не является кодом
> действующей системы, не связан с конкретной организацией и не содержит
> реальных адресов, учётных данных или сведений о партнёрах.

Study Hub моделирует единую внешнюю точку доступа к условным партнёрским
приложениям. Он получает конфигурацию из учебного сервиса App Study и направляет
запрос в нужный демонстрационный микросервис по `appId`.

Study Hub не хранит конфигурацию и пользовательские сессии, не имеет
собственной БД и остаётся stateless-сервисом.

## Статус проекта

Это самостоятельный учебный пример архитектуры, а не готовый шаблон для
промышленного внедрения. Названия сервисов, namespace, realm, audience,
маршруты, роли и сетевые политики в README приведены только как примеры.

Перед использованием идей в реальном проекте необходимо отдельно согласовать
API-контракты, требования информационной безопасности, модель авторизации,
сетевую схему, управление секретами и эксплуатационные требования.

## Архитектура

```mermaid
flowchart LR
    Client["Мобильное приложение"] --> Gateway["Внешний Gateway"]
    Gateway --> Study-Hub["Study Hub"]
    Study-Hub --> Registry["App Study"]
    Study-Hub --> PartnerA["Партнёр app-a"]
    Study-Hub --> PartnerB["Партнёр app-b"]
```

В рассматриваемой учебной схеме снаружи публикуются только маршруты Study Hub.
App Study и демонстрационные партнёрские API доступны только внутри Kubernetes.

Основной поток запроса:

1. Gateway проверяет пользовательский JWT и передаёт запрос в Study Hub.
2. Study Hub получает конфигурацию приложения из App Study.
3. Для бизнес-операции Study Hub обращается к Kubernetes Service с именем `appId`.
4. Исходящий запрос содержит тот же заголовок `Authorization`, который передал Gateway.

## Технологии

- Java 21;
- Spring Boot 3.5.14;
- Spring MVC с virtual threads;
- Spring `RestClient` и Apache HttpClient 5;
- RFC 9457 Problem Details;
- springdoc-openapi;
- Logbook;
- Lombok.

## API Study Hub

Во внешнем контуре все бизнес-маршруты требуют пользовательский JWT. Его
проверяет Gateway; сам Study Hub JWT не валидирует.

| Метод | Маршрут                                                             | Назначение |
|---|---------------------------------------------------------------------|---|
| `GET` | `/hub/api/v1/catalog` | Получить дерево каталога |
| `GET` | `/hub/api/v1/catalog/groups/{groupId}` | Получить группу и один уровень её элементов |
| `GET` | `/hub/api/v1/applications/{appId}` | Получить конфигурацию приложения |
| `GET` | `/hub/api/v1/applications/{appId}/orders/{orderId}` | Получить заказ и данные для оплаты |
| `POST` | `/hub/api/v1/applications/{appId}/launch` | Получить данные для запуска приложения |

`GET /applications/{appId}` возвращает приложение в любом статусе: `ACTIVE`,
`DISABLED` или `MAINTENANCE`. Это позволяет корректно обработать deep link и
устаревшие данные клиента. Внешнее поле `financialConfiguration` содержит только
нужные клиенту `merchantId` и `serviceId`. Внутренние `paymentWebhookUrl` и
`credentialId` мобильному приложению не передаются.

`catalog` возвращает полное дерево, начиная с корневой группы `ecosystem`.
Для всех элементов используются общие поля `id` и `type`. У группы `id`
содержит `groupId`, у приложения — `appId`; при выборе `APPLICATION` мобильный
клиент передаёт его `id` как `{appId}` в launch API и API заказа. Также каталог
содержит локализованный `title`, `iconUrl`, `layout`, статус и
maintenance-сообщения. Runtime-настройки WebView и партнёрская сессия в каталог
не входят. В каталог включаются только `ACTIVE` и `MAINTENANCE`; `DISABLED`
скрывается.

`GET /catalog/groups/{groupId}` используется для поэтапной загрузки. Он
возвращает `CatalogGroupResponse` с непосредственными дочерними `GROUP` и
`APPLICATION` в `items`. Контракт совпадает с элементами полного каталога:
приложение содержит статус и maintenance-сообщения, группа — `layout`. Дочерняя
группа в таком ответе не раскрывается: её `items` пуст, а содержимое загружается
отдельным запросом по её `id`. Возвращаются приложения в статусах `ACTIVE` и
`MAINTENANCE`, а также группы с видимым приложением на любом уровне ниже.
Элементы отсортированы по `displayOrder`, затем по `id`. Неизвестный `groupId`
даёт `404`; существующая пустая группа возвращается с пустым `items`.

Успешные ответы перечисленных бизнес-API содержат `Cache-Control: no-store`.

## Взаимодействие с App Study

Study Hub обращается к следующим внутренним API без пользовательского JWT:

```text
GET /internal/api/v1/applications/{appId}/configuration
GET /internal/api/v1/applications/{appId}/routing
GET /internal/api/v1/catalog
GET /internal/api/v1/catalog/groups/{groupId}
```

Текущие consumer-контракты Study Hub:

```text
HubApplicationConfigurationResponse
ApplicationRoutingResponse
CatalogGroupResponse
CatalogApplicationResponse
```

DTO клиента находятся в пакете `client.appregistry.dto` и отражают сетевой
контракт App Study. Дополнительный mapper внутри Study Hub для них не нужен.

Пользовательский заголовок `Authorization` передаётся только партнёрскому API.
Внутренние endpoint App Study должны быть недоступны из внешнего контура и
ограничены Kubernetes NetworkPolicy и, при наличии инфраструктуры, mTLS.

Контракты соответствуют App Study: maintenance-сообщения передаются объектом
`maintenanceMessages`, каталог содержит группы и приложения, а опциональная
финансовая конфигурация передаётся в `financialConfiguration`. Если она не
настроена, поле присутствует со значением `null`. При наличии объект содержит
`merchantId`, `serviceId`, `contractNumberServiceFieldId`, `notificationFields`,
`paymentWebhookUrl` и `credentialId`. Enum должны
иметь одинаковые значения `ApplicationStatus`, `IntegrationType`, `SessionMode`,
`GroupLayout` и `CatalogItemType`.

Это внутренний consumer-контракт Hub с App Study. Перед внешним ответом Hub
преобразует его в `ApplicationConfigurationResponse` и исключает
`paymentWebhookUrl` и `credentialId`.

В этой модели App Study является единственным владельцем конфигурации и
проверяет формат `appId` при регистрации. Для договорной операции Study Hub
получает по внешнему `appId` минимальный маршрут: канонический `appId`, статус,
`serviceId` и правила платёжных полей. Партнёр вызывается только для приложения
в статусе `ACTIVE`.

## Маршрутизация к партнёрам

В демонстрационном варианте развёртывания Study Hub и партнёрские Kubernetes
Service находятся в одном namespace. `appId` совпадает с `metadata.name`
Kubernetes Service партнёра.

Пример:

```text
appId: insurance-kasko
base URL: http://insurance-kasko
```

Внутренние контракты партнёров:

```text
GET  http://{appId}/internal/api/v1/contracts/{orderId}
     Accept-Language: kk-KZ
POST http://{appId}/internal/api/v1/session/token
POST http://{appId}/internal/api/v1/session/code
```

Каждый Service предоставляет порт `80`; `targetPort` контейнеров может
отличаться. Если сервисы будут размещены в разных namespace, потребуется полный
DNS-адрес либо отдельная конфигурация маршрута.

Адаптеры нормализуют ответ конкретного партнёра в общий внутренний
контракт:

```json
{
  "orderId": "TRAVEL-2026-001",
  "status": "CREATED",
  "amount": 15000.00,
  "data": {
    "tickets": [
      {
        "route": "Алматы — Астана",
        "airport": "ALA — NQZ",
        "date": "2026-11-05T10:30:00+05:00"
      }
    ]
  },
  "paymentFields": {
    "type": "flight"
  }
}
```

`paymentFields` содержит стабильные внутренние коды. Travel-адаптер,
например, преобразует партнёрское `orderType` в `paymentFields.type`.
Другие адаптеры передают пустой объект, если дополнительных реквизитов нет.

Hub получает из App Study `contractNumberServiceFieldId` и
`paymentFieldMappings`, связывает их со значениями адаптера и отдаёт
мобильному клиенту готовые реквизиты:

```json
{
  "orderId": "TRAVEL-2026-001",
  "status": "CREATED",
  "amount": 15000.00,
  "data": {
    "tickets": [
      {
        "route": "Алматы — Астана",
        "airport": "ALA — NQZ",
        "date": "2026-11-05T10:30:00+05:00"
      }
    ]
  },
  "payment": {
    "serviceId": 191,
    "details": [
      {"serviceFieldId": 901, "value": "TRAVEL-2026-001"},
      {"serviceFieldId": 902, "value": "flight"}
    ]
  }
}
```

Мобильный клиент не знает о `orderType`, коде `type` или правилах App Study:
он получает специфичные для приложения данные в `data` и передаёт `payment.serviceId` вместе
с `payment.details` в процесс оплаты. Пустой, неполный или
противоречивый ответ адаптера преобразуется в `502 Bad Gateway`.

Ожидаемая бизнес-ошибка партнёра имеет статус `422 Unprocessable Entity`:

```json
{
  "message": "Contract amount is invalid",
  "error_code": "CONTRACT_DIFFERENT_AMOUNT",
  "errors": {
    "contract": ["Contract amount is invalid"]
  }
}
```

Study Hub сохраняет HTTP-статус `422` и преобразует ответ в RFC 9457. Поле
`code` содержит стабильный код Study Hub `PARTNER_BUSINESS_ERROR`, а
`errorCode` — бизнес-код партнёра для обработки на frontend. Неизвестные поля
партнёрского ответа игнорируются. Тела технических ошибок `5xx` клиенту не
передаются.

## Запуск приложения

Мобильное приложение использует один API независимо от типа интеграции и
способа создания партнёрской сессии:

```text
POST /hub/api/v1/applications/{appId}/launch
```

Тело запроса отсутствует. JWT передаётся в заголовке:

```http
Authorization: Bearer <user-jwt>
```

Study Hub получает конфигурацию из App Study и обрабатывает её следующим образом:

- `PARTNER_WEBVIEW + TOKEN` — вызывает token API партнёра;
- `PARTNER_WEBVIEW + CODE` — вызывает code API партнёра;
- `PARTNER_WEBVIEW + VIEW_URL` — получает от adapter-а готовый URL WebView;
- `API_FIRST + NONE` — не создаёт партнёрскую сессию.

Запрос может содержать стандартный заголовок `Accept-Language`, например
`kk-KZ`. Hub передаёт его во внутренний session API adapter-а. Если заголовка
нет, используется `ru`.

Ответ для партнёрского WebView:

```json
{
  "appId": "insurance-app",
  "entryUrl": "https://partner.example/app",
  "session": {
    "type": "TOKEN",
    "value": "partner-session-token"
  }
}
```

Для `API_FIRST` поле `session` равно `null`:

```json
{
  "appId": "insurance-app",
  "entryUrl": "https://platform.example/app",
  "session": null
}
```

Для `VIEW_URL` adapter возвращает готовый URL, включая partner token. Hub кладёт
его только в `entryUrl`; мобильное приложение ничего к URL не добавляет:

```json
{
  "appId": "gourmet",
  "entryUrl": "https://gourmet.example/view?token=abc",
  "session": {
    "type": "VIEW_URL"
  }
}
```

`expiresAt` отсутствует. Для `TOKEN` и `CODE` значение `session.value` приходит
только от партнёра. Для `VIEW_URL` это поле отсутствует. Сессионные данные не
сохраняются и не записываются в логи. Пустое значение, небезопасный URL,
некорректный JSON или
недоступность партнёра преобразуются в `502 Bad Gateway`. Launch и order
операции для приложения не в статусе `ACTIVE` возвращают `409 Conflict` с
кодом `APPLICATION_UNAVAILABLE` без вызова партнёра.

## Ошибки

REST-ошибки возвращаются в формате RFC 9457 `application/problem+json`.

| HTTP-статус | Код | Значение |
|---|---|---|
| `400` | `VALIDATION_ERROR` | Некорректные параметры запроса |
| `401` | `UNAUTHORIZED` | JWT отсутствует или отклонён внешним Gateway |
| `403` | `FORBIDDEN` | Запрос отклонён политикой внешнего Gateway |
| `404` | `APPLICATION_NOT_FOUND` | Приложение не найдено |
| `404` | `ORDER_NOT_FOUND` | Заказ не найден |
| `409` | `APPLICATION_UNAVAILABLE` | Приложение недоступно для операции |
| `422` | `PARTNER_BUSINESS_ERROR` | Партнёр отклонил запрос по бизнес-правилам |
| `502` | `PARTNER_API_UNAVAILABLE` | Ошибка или недоступность партнёра |
| `503` | `DEPENDENCY_UNAVAILABLE` | App Study недоступен |
| `500` | `INTERNAL_ERROR` | Непредвиденная ошибка Study Hub |

Ответ также может содержать `traceId`, `correlationId` и ошибки валидации по
полям.

## Security

Study Hub не подключается к Keycloak, не декодирует JWT и не проверяет подпись,
срок действия, issuer, audience или роли. OAuth2 Resource Server, OAuth2 Client,
`client_credentials` и client secret в сервисе отсутствуют.

Hub прозрачно копирует входящий заголовок `Authorization` в запросы к App Study
и партнёрским сервисам. Если заголовка нет, исходящий вызов выполняется без
него. Поэтому прямой доступ к Hub в обход Gateway недопустим.

Gateway проверяет audience, предназначенный для внешнего API Hub. Если App
Study также проверяет переданный пользовательский JWT, токен должен содержать
и его audience, например:

```json
{
  "aud": ["hub", "app-registry"]
}
```

Audience `hub` проверяет Gateway, а `app-registry` — App Study. Study Hub эти
claims не проверяет.
Административные роли App Study (`viewer`, `admin`) обычному пользователю Study
Hub не требуются.

Если похожая схема рассматривается для production, потребуются как минимум:

- внешний Gateway маршрутизирует только в Study Hub;
- ingress NetworkPolicy разрешает вход в Study Hub только от Gateway;
- App Study и партнёры используют Service типа `ClusterIP`;
- `/internal/**` не публикуется через Ingress/Gateway;
- default-deny NetworkPolicy разрешает партнёрам вход только от Study Hub;
- egress Study Hub ограничен App Study и partner-подами;
- внутренний HTTP защищён mTLS/service mesh;
- на Gateway настроен rate limit для launch и order API;
- контейнеры запускаются не от `root`.

Если Study Hub или партнёр не проверяет JWT криптографически, он может доверять
токену только при гарантированной сетевой изоляции и защищённом соединении с
Gateway. Более безопасный вариант — повторная JWT-валидация в каждом сервисе.

Реальные секреты хранятся в Vault/Kubernetes Secret и не передаются в Study Hub
или мобильное приложение. `credentialId` может быть только непрозрачной ссылкой
на секрет.

## Логирование

Профиль `prod` использует нативный ECS-формат Spring Boot и пишет одну
JSON-запись на строку в `stdout`. В MDC добавляются `traceId` и `correlationId`;
эти же значения передаются зависимостям в заголовках `X-Trace-Id` и
`X-Correlation-Id`.

Logbook журналирует входящие и исходящие HTTP-вызовы. Для успешных операций
записываются метаданные запроса и ответа, тела добавляются только при статусе
`400` и выше. Endpoint получения заказа исключён из Logbook целиком, поскольку URL
содержит идентификатор заказа, а партнёр может вернуть произвольные чувствительные
данные. Для launch и session успешные тела с token/code/готовым URL не записываются, а
технические метаданные и безопасно отфильтрованные тела ошибок остаются доступны.

Логируются:

- ошибки и недоступность App Study;
- HTTP-метаданные и отфильтрованные тела ошибок launch/session;
- безопасные коды ошибок партнёрских сервисов;
- успешная подготовка запуска — только `appId`, `integrationType` и `sessionMode`;
- отклонение launch-запроса для недоступного приложения;
- непредвиденные ошибки со stack trace.

Не логируются:

- JWT и заголовок `Authorization`;
- успешные тела launch/session с `session.value`, готовым `entryUrl`, partner
  `token` и `code`;
- идентификатор и содержимое заказа;
- любые тела order-запросов и ответов;
- секреты и credential-related поля.

Приложение не подключается к Elasticsearch или Logstash напрямую. В Kubernetes
логи должны собираться платформенным агентом:

```text
Hub stdout → Filebeat / Fluent Bit / Vector → Logstash → Elasticsearch → Kibana
```

Для передачи в ELK необходимо:

- запускать приложение с профилем `prod`;
- настроить collector на разбор ECS JSON — одна запись на строку;
- сохранять ECS-поля `@timestamp`, `service.name`, `log.level`, `log.logger`,
  `traceId`, `correlationId` и HTTP-поля Logbook;
- добавлять Kubernetes metadata на стороне collector;
- не применять multiline parser: stack trace находится внутри одной JSON-записи.

## Метрики

Actuator публикует Prometheus-метрики на внутреннем endpoint
`GET /actuator/prometheus`. Prometheus забирает их из Kubernetes по расписанию;
Hub не подключается к Prometheus или Grafana напрямую. Endpoint не должен
маршрутизироваться через внешний Gateway. Способ обнаружения сервиса
(`ServiceMonitor`, `PodMonitor` или annotations) определяется Kubernetes-
платформой.

## Конфигурация

Пример переменных для профиля `prod`:

| Переменная | Назначение |
|---|---|
| `APP_REGISTRY_BASE_URL` | Внутренний URL App Study |
| `APP_PROBLEMS_BASE_URI` | Базовый URI типов Problem Details |
| `APP_VERSION` | Версия приложения в ECS-логах |
| `APP_ENVIRONMENT` | Название окружения в ECS-логах, по умолчанию `prod` |

Настройки исходящих HTTP-вызовов:

| Переменная | Значение по умолчанию |
|---|---|
| `OUTBOUND_HTTP_CONNECT_TIMEOUT` | `2s` |
| `OUTBOUND_HTTP_READ_TIMEOUT` | `3s` |

Virtual threads включены через `spring.threads.virtual.enabled=true`.

## Локальный запуск

Для локального запуска Study Hub требуются JDK 21 и учебный App Study на `8082`.
Keycloak самому Hub не нужен. Значения по умолчанию предназначены только для
локальной разработки.

App Study можно запустить на требуемом порту так:

```bash
SERVER_PORT=8082 ./gradlew bootRun
```

Затем запускается Study Hub:

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

В IntelliJ IDEA укажите `local` в поле **Active profiles**.

Swagger UI:

```text
http://localhost:8080/swagger-ui
```

Профиль `local` позволяет проверить конфигурационные API. Вызовы партнёров по
`http://{appId}` из процесса IntelliJ не работают, потому что Kubernetes DNS
доступен только внутри кластера. Для полной проверки используйте один из
вариантов:

1. запустите Study Hub и партнёров в одном локальном Kubernetes namespace;
2. запустите их в общей Docker Compose network;
3. добавьте отдельное local-only сопоставление `appId -> localhost URL`.

При локальном Kubernetes наружу достаточно пробросить только Study Hub:

```bash
kubectl -n demo-platform port-forward service/hub 8080:80
```

## Проверка

```bash
./gradlew test
```

Перед фиксацией изменений полный набор тестов должен проходить без ошибок.
Отдельно необходимо сверять consumer-контракты Study Hub с фактическими
JSON-ответами учебного App Study и демонстрационных партнёрских сервисов, а не
только с вручную созданными mock-ответами.

## Профиль `prod`

Профиль `prod`:

- отключает Swagger UI и OpenAPI endpoint;
- скрывает детали health-check;
- включает структурированные JSON-логи;
- требует явной передачи обязательных переменных окружения.

Наличие этого профиля показывает различие локальных и production-like настроек,
но само по себе не означает готовность приложения к эксплуатации в реальной
инфраструктуре.

Outbox, JPA, Flyway, Kafka и брокер сообщений в Study Hub не нужны: сервис не
изменяет собственные данные и не публикует события.
