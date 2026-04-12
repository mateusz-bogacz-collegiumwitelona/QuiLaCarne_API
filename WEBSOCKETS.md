# Dokumentacja WebSocket (STOMP) — Qui La Carne

Wszystkie powiadomienia w czasie rzeczywistym używają protokołu **STOMP** opartego na WebSocketach (z fallbackiem na
SockJS).

---

## 1. Architektura: Event-Driven Data

Aplikacja przesyła kompletne, płaskie obiekty zmian (DTO). Klient **nie musi** wykonywać dodatkowego zapytania GET do
REST API po odebraniu zdarzenia – pełne i aktualne dane znajdują się bezpośrednio w payloadzie wiadomości.

### Struktura Koperty — `WebSocketEvent<T>`

Każda wiadomość przesyłana przez WebSocket jest opakowana w standardową, generyczną kopertę:

| Pole         | Typ      | Opis                                                                                      |
|:-------------|:---------|:------------------------------------------------------------------------------------------|
| `eventType`  | `String` | Rodzaj operacji: `CREATED`, `UPDATED` lub `DELETED`.                                      |
| `entityType` | `String` | Typ encji, np. `ORDER`, `TABLE`, `RESERVATION`, `INGREDIENT`.                             |
| `token`      | `String` | Unikalny identyfikator (token) zmienionego zasobu.                                        |
| `payload`    | `Object` | Płaski obiekt danych DTO (np. `SyncOrderResponse`). Zawsze `null` dla operacji `DELETED`. |
| `timestamp`  | `String` | Czas wygenerowania zdarzenia w formacie ISO (UTC).                                        |

---

## 2. Nawiązywanie Połączenia

**Adres endpointu:**

```
ws://<adres-serwera>/ws-qlc
```

> **Uwaga:** Przy korzystaniu z SockJS (np. `stompjs`), użyj schematu `http://` zamiast `ws://`. Token JWT autoryzujący
> użytkownika należy przekazać w nagłówku ramki `CONNECT`.

---

## 3. Wykaz Kanałów (Topics) i Payloadów

Poniżej znajduje się lista wszystkich dostępnych kanałów, na które klient może się zasubskrybować, wraz z informacją o
klasie danych przesyłanej w polu `payload` w przypadku operacji tworzenia lub aktualizacji.

### 3.1 Rezerwacje i Stoliki

| Topic                         | Kiedy występuje                                                                                        | Klasa Payloadu (DTO)      |
|:------------------------------|:-------------------------------------------------------------------------------------------------------|:--------------------------|
| `/topic/reservations/updates` | Tworzenie nowej rezerwacji, anulowanie, przypisanie kelnera (IN_PROGRESS) lub oznaczenie jako NO_SHOW. | `SyncReservationResponse` |
| `/topic/tables/updates`       | Utworzenie stolika, usunięcie lub zmiana jego statusu (np. na CLEANING, OUT_OF_SERVICE).               | `SyncTableResponse`       |

### 3.2 Zamówienia

| Topic                   | Kiedy występuje                                                                                                       | Klasa Payloadu (DTO)    |
|:------------------------|:----------------------------------------------------------------------------------------------------------------------|:------------------------|
| `/topic/orders/updates` | Tworzenie zamówienia do rezerwacji, aktualizacja całego zamówienia (np. zmiana całkowitej ceny, przypisanie kelnera). | `SyncOrderResponse`     |
| `/topic/orders/items`   | Dodawanie nowych pozycji do zamówienia lub zmiana statusów konkretnych dań.                                           | `SyncOrderItemResponse` |

### 3.3 Słowniki i Menu (Dictionary Sync)

Kanały te służą do synchronizacji słowników referencyjnych oraz menu.

| Topic                                   | Kiedy występuje                                                                 | Klasa Payloadu (DTO)                |
|:----------------------------------------|:--------------------------------------------------------------------------------|:------------------------------------|
| `/topic/dictionary/sync`                | Dodanie nowego składnika do słownika.                                           | `SyncIngredientResponse`            |
| `/topic/menu/availability`              | Usunięcie składnika (Soft Delete), które powoduje dezaktywację powiązanych dań. | Zdarzenie `DELETED` (brak payloadu) |
| `/topic/dictionary/allergens`           | Dodanie lub usunięcie alergenu ze słownika.                                     | `SyncDictionaryResponse`            |
| `/topic/dictionary/dish-categories`     | Dodanie lub usunięcie kategorii dań.                                            | `SyncDictionaryResponse`            |
| `/topic/dictionary/table-statuses`      | Dodanie lub usunięcie statusu stolika.                                          | `SyncDictionaryResponse`            |
| `/topic/dictionary/order-statuses`      | Dodanie/usunięcie statusu całego zamówienia.                                    | `SyncDictionaryResponse`            |
| `/topic/dictionary/order-item-statuses` | Dodanie/usunięcie statusu pojedynczej pozycji zamówienia.                       | `SyncDictionaryResponse`            |

### 3.4 Bezpieczeństwo i Zgłoszenia (Security & Reports)

| Topic                    | Kiedy występuje                                                                     | Klasa Payloadu (DTO) |
|:-------------------------|:------------------------------------------------------------------------------------|:---------------------|
| `/topic/reports/updates` | Kelner zgłasza klienta lub manager rozpatruje (akceptuje/odrzuca) takie zgłoszenie. | `SyncReportResponse` |
| `/topic/security/bans`   | Nadanie bana klientowi, usunięcie bana lub jego automatyczne wygaśnięcie (CRON).    | `SyncBanResponse`    |

### 3.5 Zarządzanie Menu (Dania)

Kanały odpowiedzialne za główne pozycje w menu restauracji.

| Topic                 | Kiedy występuje                                                                                     | Klasa Payloadu (DTO) |
|:----------------------|:----------------------------------------------------------------------------------------------------|:---------------------|
| `/topic/menu/updates` | Dodanie nowego dania, edycja jego ceny, zmiana zdjęcia, lub całkowite usunięcie z menu.             | `SyncDishResponse`   |
| `/topic/menu`         | Zmiana dostępności dania (np. ręczne wyłączenie przez managera z powodu braku składnika na kuchni). | `SyncDishResponse`   |

### 3.6 Zarządzanie Personelem (Users / Staff)

Kanał synchronizujący konta pracowników (kelnerów, managerów, adminów).

| Topic                      | Kiedy występuje                                                                     | Klasa Payloadu (DTO) |
|:---------------------------|:------------------------------------------------------------------------------------|:---------------------|
| `/topic/personnel/updates` | Dodanie nowego pracownika, zmiana jego roli, edycja danych, lub zablokowanie konta. | `SyncUserResponse`   |

---

## 4. Przykłady Struktury Komunikatów (JSON)

Poniżej znajdują się przykłady reprezentujące komunikaty dla różnych typów operacji (`eventType`).

### Przykład A: Tworzenie lub Aktualizacja (z pełnym payloadem)

Gdy zasób zostaje utworzony (`CREATED`) lub zaktualizowany (`UPDATED`), pole `payload` zawiera pełny płaski obiekt (w
tym wypadku zaktualizowany stolik).

```json
{
  "eventType": "UPDATED",
  "entityType": "TABLE",
  "token": "TABLE-XYZ-123",
  "payload": {
    "token": "TABLE-XYZ-123",
    "tableNumber": 12,
    "capacity": 4,
    "statusTokens": [
      "CLEANING"
    ],
    "createdAt": "2026-03-01T10:00:00Z",
    "updatedAt": "2026-04-12T22:59:00Z"
  },
  "timestamp": "2026-04-12T22:59:01Z"
}
```

### Przykład B: Usunięcie zasobu (brak payloadu)

Dla operacji `DELETED` obiekt `payload` jest zawsze pusty (`null`). Klient powinien usunąć zasób o podanym `token` ze
swojego lokalnego stanu (np. ze store'a Redux / kontekstu).

```json
{
  "eventType": "DELETED",
  "entityType": "ALLERGEN",
  "token": "ALLERGEN-NUTS-99",
  "payload": null,
  "timestamp": "2026-04-12T23:05:00Z"
}
```