# WebSocket contract

Ten dokument opisuje aktualne kanały WebSocket/STOMP w backendzie oraz format wiadomości, które frontend może odbierać po subskrypcji.

## 1. Połączenie

Backend wystawia jeden endpoint STOMP + SockJS:

- `/ws-qlc`

### Connect

Połączenie autoryzuje się przez nagłówek `Authorization` wysłany w ramce `CONNECT`:

```text
CONNECT
Authorization:Bearer <JWT>
accept-version:1.2
heart-beat:10000,10000

<END>
```

### Subskrypcje

Wiadomości publikowane są przez broker STOMP na kanały z prefiksem `/topic`.

Przykład subskrypcji:

```text
SUBSCRIBE
id:sub-orders
destination:/topic/orders/updates

<END>
```

### Ważne uwagi

- W kodzie backendu nie znalazłem aktualnie używanych handlerów `/app/...` do wysyłania wiadomości z frontendu do backendu.
- Prefiksy `/queue` i `/user` są skonfigurowane, ale w obecnym kodzie nie znalazłem aktywnych publikacji do tych kanałów.
- Backend publikuje wiadomości przez helper `NotificationServices.sendEventToTopic(...)`, więc frontend zawsze subskrybuje pełną ścieżkę z prefiksem `/topic`.

## 2. Wspólny format wiadomości

Każdy event ma wspólny envelope:

```json
{
  "eventType": "CREATED | UPDATED | DELETED",
  "entityType": "STRING",
  "token": "STRING",
  "payload": {},
  "timestamp": "2026-05-13T12:34:56Z"
}
```

### Pola

- `eventType` — typ zdarzenia:
  - `CREATED`
  - `UPDATED`
  - `DELETED`
- `entityType` — typ biznesowy encji, np. `ORDER`, `TABLE`, `EMPLOYEE`
- `token` — token obiektu, którego dotyczy zdarzenie
- `payload` — dane szczegółowe; przy `DELETED` jest `null`
- `timestamp` — czas wygenerowania eventu, w UTC

## 3. Lista wszystkich aktywnych kanałów

| Kanał subskrypcji                       | Co oznacza                          | Kiedy przychodzi update                                                                    | `entityType`        | Payload                             |
|-----------------------------------------|-------------------------------------|--------------------------------------------------------------------------------------------|---------------------|-------------------------------------|
| `/topic/reports/updates`                | Zgłoszenia użytkowników             | dodanie reportu, zmiana statusu reportu                                                    | `REPORT`            | `SyncReportResponse`                |
| `/topic/dictionary/allergens`           | Słownik alergenów                   | dodanie / usunięcie alergenu                                                               | `ALLERGEN`          | `SyncDictionaryResponse` lub `null` |
| `/topic/tables/updates`                 | Stoliki                             | dodanie stolika, zmiana statusu stolika, usunięcie stolika                                 | `TABLE`             | `SyncTableResponse` lub `null`      |
| `/topic/dictionary/table-statuses`      | Słownik statusów stolików           | dodanie / usunięcie statusu stolika                                                        | `TABLE_STATUS`      | `SyncDictionaryResponse` lub `null` |
| `/topic/menu/availability`              | Dostępność menu                     | usunięcie składnika, które wpływa na dostępność dań                                        | `INGREDIENT`        | `null`                              |
| `/topic/dictionary/sync`                | Słownik składników                  | dodanie składnika                                                                          | `INGREDIENT`        | `SyncIngredientResponse`            |
| `/topic/dictionary/dish-categories`     | Kategorie dań                       | dodanie / usunięcie kategorii dań                                                          | `DISH_CATEGORY`     | `SyncDictionaryResponse` lub `null` |
| `/topic/menu/dishes`                    | Dania w menu                        | dodanie, edycja, zmiana dostępności, usunięcie dania                                       | `DISH`              | `SyncDishResponse` lub `null`       |
| `/topic/orders/updates`                 | Zamówienia                          | utworzenie zamówienia, update zamówienia, usunięcie zamówienia, zmiana statusów zamówienia | `ORDER`             | `SyncOrderResponse` lub `null`      |
| `/topic/orders/items`                   | Pozycje zamówienia                  | utworzenie / update pozycji zamówienia                                                     | `ORDER_ITEM`        | `SyncOrderItemResponse`             |
| `/topic/dictionary/order-statuses`      | Słownik statusów zamówień           | dodanie / usunięcie statusu zamówienia                                                     | `ORDER_STATUS`      | `SyncDictionaryResponse` lub `null` |
| `/topic/dictionary/order-item-statuses` | Słownik statusów pozycji zamówienia | dodanie / usunięcie statusu pozycji zamówienia                                             | `ORDER_ITEM_STATUS` | `SyncDictionaryResponse` lub `null` |
| `/topic/reservations/updates`           | Rezerwacje                          | utworzenie rezerwacji, update rezerwacji                                                   | `RESERVATION`       | `SyncReservationResponse`           |
| `/topic/personnel/updates`              | Personel / pracownicy               | utworzenie, update, usunięcie pracownika                                                   | `EMPLOYEE`          | `SyncUserResponse` lub `null`       |
| `/topic/security/bans`                  | Bany użytkowników                   | nałożenie bana, automatyczne wygaśnięcie bana                                              | `BAN`               | `SyncBanResponse`                   |

## 4. Szczegółowy opis kanałów

### 4.1 `/topic/reports/updates`

Przychodzi, gdy:

- report zostaje dodany do użytkownika,
- report zmienia status na `ACCEPTED` albo `REJECTED`.

Typy eventów:

- `CREATED` — przy dodaniu reportu
- `UPDATED` — przy zmianie statusu reportu

Payload: `SyncReportResponse`

```json
{
  "eventType": "CREATED",
  "entityType": "REPORT",
  "token": "rep_123",
  "payload": {
    "token": "rep_123",
    "guestToken": "user_guest_1",
    "reporterToken": "user_waiter_7",
    "statusTokens": ["in_progress"],
    "reason": "Zachowanie przy stoliku",
    "createdAt": "2026-05-13T12:00:00Z",
    "updatedAt": "2026-05-13T12:00:00Z"
  },
  "timestamp": "2026-05-13T12:00:01Z"
}
```

### 4.2 `/topic/dictionary/allergens`

Przychodzi, gdy:

- dodano alergen,
- usunięto alergen.

Typy eventów:

- `CREATED`
- `DELETED`

Payload:

- przy `CREATED` / `UPDATED`: `SyncDictionaryResponse`
- przy `DELETED`: `null`

```json
{
  "eventType": "CREATED",
  "entityType": "ALLERGEN",
  "token": "allergen_gluten",
  "payload": {
    "token": "allergen_gluten",
    "nameEn": "Gluten",
    "namePl": "Gluten"
  },
  "timestamp": "2026-05-13T12:00:01Z"
}
```

### 4.3 `/topic/tables/updates`

Przychodzi, gdy:

- dodano stolik,
- zmieniono status stolika na cleaning / out of service / available,
- usunięto stolik.

Typy eventów:

- `CREATED`
- `UPDATED`
- `DELETED`

Payload:

- przy `CREATED` / `UPDATED`: `SyncTableResponse`
- przy `DELETED`: `null`

```json
{
  "eventType": "UPDATED",
  "entityType": "TABLE",
  "token": "table_12",
  "payload": {
    "token": "table_12",
    "tableNumber": 12,
    "capacity": 4,
    "statusTokens": ["cleaning"],
    "createdAt": "2026-05-13T10:00:00Z",
    "updatedAt": "2026-05-13T12:10:00Z"
  },
  "timestamp": "2026-05-13T12:10:01Z"
}
```

### 4.4 `/topic/dictionary/table-statuses`

Przychodzi, gdy:

- dodano status stolika,
- usunięto status stolika.

Typy eventów:

- `CREATED`
- `DELETED`

Payload:

- `SyncDictionaryResponse` albo `null`

```json
{
  "eventType": "CREATED",
  "entityType": "TABLE_STATUS",
  "token": "cleaning",
  "payload": {
    "token": "cleaning",
    "nameEn": "Cleaning",
    "namePl": "Sprzątanie"
  },
  "timestamp": "2026-05-13T12:00:01Z"
}
```

### 4.5 `/topic/menu/availability`

Przychodzi, gdy:

- usunięto składnik,
- backend oznacza wtedy powiązane dania jako niedostępne.

W aktualnym kodzie kanał ten wysyła event `DELETED` dla encji `INGREDIENT`.

Payload: `null`

```json
{
  "eventType": "DELETED",
  "entityType": "INGREDIENT",
  "token": "ing_55",
  "payload": null,
  "timestamp": "2026-05-13T12:00:01Z"
}
```

### 4.6 `/topic/dictionary/sync`

Przychodzi, gdy:

- dodano składnik.

Typ eventu:

- `CREATED`

Payload: `SyncIngredientResponse`

```json
{
  "eventType": "CREATED",
  "entityType": "INGREDIENT",
  "token": "ing_55",
  "payload": {
    "token": "ing_55",
    "nameEn": "Tomato",
    "namePl": "Pomidor",
    "allergenTokens": ["allergen_1"]
  },
  "timestamp": "2026-05-13T12:00:01Z"
}
```

### 4.7 `/topic/dictionary/dish-categories`

Przychodzi, gdy:

- dodano kategorię dań,
- usunięto kategorię dań.

Typy eventów:

- `CREATED`
- `DELETED`

Payload:

- `SyncDictionaryResponse` albo `null`

```json
{
  "eventType": "DELETED",
  "entityType": "DISH_CATEGORY",
  "token": "cat_1",
  "payload": null,
  "timestamp": "2026-05-13T12:00:01Z"
}
```

### 4.8 `/topic/menu/dishes`

Przychodzi, gdy:

- dodano danie,
- edytowano danie,
- zmieniono dostępność dania,
- usunięto danie,
- usunięto kategorię dania i backend przepiął dania na kategorię fallback.

Typy eventów:

- `CREATED`
- `UPDATED`
- `DELETED`

Payload:

- przy `CREATED` / `UPDATED`: `SyncDishResponse`
- przy `DELETED`: `null`

```json
{
  "eventType": "UPDATED",
  "entityType": "DISH",
  "token": "dish_9",
  "payload": {
    "token": "dish_9",
    "name": "Margherita",
    "price": 3200,
    "isAvailable": true,
    "unavailableReason": null,
    "imageUrl": "https://minio.example.com/bucket/pizza.png",
    "categoryToken": "cat_pizza",
    "ingredientTokens": ["ing_tomato", "ing_cheese"]
  },
  "timestamp": "2026-05-13T12:00:01Z"
}
```

### 4.9 `/topic/orders/updates`

Przychodzi, gdy:

- utworzono zamówienie,
- zamówienie zostało zaktualizowane,
- zamówienie zostało anulowane / oznaczone jako nieobecność / zmieniono jego status,
- status zamówienia został usunięty i zamówienia zostały przepięte na fallback,
- zamówienie zostało usunięte, jeśli zostanie użyty dedykowany publisher.

Typy eventów:

- `CREATED`
- `UPDATED`
- `DELETED`

Payload:

- przy `CREATED` / `UPDATED`: `SyncOrderResponse`
- przy `DELETED`: `null`

```json
{
  "eventType": "UPDATED",
  "entityType": "ORDER",
  "token": "order_1001",
  "payload": {
    "token": "order_1001",
    "reservationToken": "res_20",
    "tableToken": "table_12",
    "waiterToken": "user_waiter_7",
    "statusTokens": ["in_progress"],
    "totalPrice": 12800,
    "createdAt": "2026-05-13T11:00:00Z",
    "updatedAt": "2026-05-13T12:15:00Z"
  },
  "timestamp": "2026-05-13T12:15:01Z"
}
```

### 4.10 `/topic/orders/items`

Przychodzi, gdy:

- utworzono pozycję zamówienia,
- zaktualizowano pozycję zamówienia,
- dodano lub odjęto ilość pozycji w zamówieniu.

Typy eventów:

- `CREATED`
- `UPDATED`

Payload: `SyncOrderItemResponse`

```json
{
  "eventType": "CREATED",
  "entityType": "ORDER_ITEM",
  "token": "order_item_501",
  "payload": {
    "token": "order_item_501",
    "orderToken": "order_1001",
    "productToken": "dish_9",
    "statusTokens": ["pending"],
    "quantity": 2,
    "priceAtTimeOfOrder": 3200,
    "note": "bez cebuli",
    "createdAt": "2026-05-13T11:00:00Z",
    "updatedAt": "2026-05-13T11:00:00Z"
  },
  "timestamp": "2026-05-13T11:00:01Z"
}
```

### 4.11 `/topic/dictionary/order-statuses`

Przychodzi, gdy:

- dodano status zamówienia,
- usunięto status zamówienia,
- zamówienia zostały przepięte na fallback status.

Typy eventów:

- `CREATED`
- `UPDATED`
- `DELETED`

Payload:

- `SyncDictionaryResponse` albo `null`

```json
{
  "eventType": "CREATED",
  "entityType": "ORDER_STATUS",
  "token": "in_progress",
  "payload": {
    "token": "in_progress",
    "nameEn": "In progress",
    "namePl": "W realizacji"
  },
  "timestamp": "2026-05-13T12:00:01Z"
}
```

### 4.12 `/topic/dictionary/order-item-statuses`

Przychodzi, gdy:

- dodano status pozycji zamówienia,
- usunięto status pozycji zamówienia,
- pozycje zamówienia zostały przepięte na fallback status.

Typy eventów:

- `CREATED`
- `UPDATED`
- `DELETED`

Payload:

- `SyncDictionaryResponse` albo `null`

```json
{
  "eventType": "UPDATED",
  "entityType": "ORDER_ITEM_STATUS",
  "token": "in_progress",
  "payload": {
    "token": "in_progress",
    "nameEn": "In progress",
    "namePl": "W realizacji"
  },
  "timestamp": "2026-05-13T12:00:01Z"
}
```

### 4.13 `/topic/reservations/updates`

Przychodzi, gdy:

- utworzono rezerwację,
- zaktualizowano rezerwację,
- anulowano rezerwację,
- oznaczono rezerwację jako no-show,
- przypisano kelnera do rezerwacji.

Typy eventów:

- `CREATED`
- `UPDATED`

Payload: `SyncReservationResponse`

```json
{
  "eventType": "UPDATED",
  "entityType": "RESERVATION",
  "token": "res_20",
  "payload": {
    "token": "res_20",
    "userToken": "user_client_1",
    "tableToken": "table_12",
    "statusTokens": ["cancelled"],
    "startTime": "2026-05-13T19:00:00Z",
    "endTime": "2026-05-13T21:00:00Z",
    "createdAt": "2026-05-13T10:00:00Z",
    "updatedAt": "2026-05-13T12:20:00Z"
  },
  "timestamp": "2026-05-13T12:20:01Z"
}
```

### 4.14 `/topic/personnel/updates`

Przychodzi, gdy:

- dodano pracownika,
- zmieniono dane pracownika,
- zmieniono rolę pracownika,
- zmieniono aktywność pracownika,
- usunięto pracownika.

Typy eventów:

- `CREATED`
- `UPDATED`
- `DELETED`

Payload:

- przy `CREATED` / `UPDATED`: `SyncUserResponse`
- przy `DELETED`: `null`

```json
{
  "eventType": "UPDATED",
  "entityType": "EMPLOYEE",
  "token": "user_emp_3",
  "payload": {
    "token": "user_emp_3",
    "username": "marek.nowak",
    "email": "marek@example.com",
    "isActive": true,
    "isStaff": true,
    "roleTokens": ["role_waiter"],
    "createdAt": "2026-05-13T09:00:00Z",
    "updatedAt": "2026-05-13T12:25:00Z"
  },
  "timestamp": "2026-05-13T12:25:01Z"
}
```

### 4.15 `/topic/security/bans`

Przychodzi, gdy:

- nałożono bana na użytkownika,
- ban wygasł automatycznie i użytkownik został odblokowany.

Typy eventów:

- `CREATED`
- `UPDATED`

Payload: `SyncBanResponse`

```json
{
  "eventType": "CREATED",
  "entityType": "BAN",
  "token": "ban_77",
  "payload": {
    "token": "ban_77",
    "userToken": "user_client_9",
    "bannedByToken": "user_manager_1",
    "statusTokens": ["active"],
    "reason": "Naruszenie regulaminu",
    "expiresAt": "2026-05-20T12:00:00Z",
    "isActive": true,
    "createdAt": "2026-05-13T12:30:00Z",
    "updatedAt": "2026-05-13T12:30:00Z"
  },
  "timestamp": "2026-05-13T12:30:01Z"
}
```

## 5. Szybka ściąga dla frontendu

Jeśli frontend chce reagować na konkretne działania, to w praktyce wygląda to tak:

- przypisanie kelnera do rezerwacji → subskrybuj `/topic/reservations/updates` oraz `/topic/orders/updates`
- zmiana statusu stolika → subskrybuj `/topic/tables/updates`
- dodanie / usunięcie składnika → subskrybuj `/topic/dictionary/sync` i `/topic/menu/availability`
- dodanie / edycja / usunięcie dania → subskrybuj `/topic/menu/dishes`
- zmiana statusu raportu → subskrybuj `/topic/reports/updates`
- zmiany personelu → subskrybuj `/topic/personnel/updates`
- bany / odbanowanie → subskrybuj `/topic/security/bans`

## 6. Krótki przykład klienta

Frontend powinien:

1. połączyć się z `/ws-qlc`,
2. wysłać `CONNECT` z JWT w nagłówku `Authorization`,
3. zasubskrybować potrzebne kanały ` /topic/...`,
4. parsować envelope `WebSocketEvent<T>`.

Przykład dla STOMP JS:

```ts
const socket = new SockJS('/ws-qlc');
const client = Stomp.over(socket);

client.connect(
  { Authorization: `Bearer ${token}` },
  () => {
    client.subscribe('/topic/orders/updates', (message) => {
      const event = JSON.parse(message.body);
      console.log(event.eventType, event.entityType, event.payload);
    });
  }
);
```
