# Dokumentacja WebSocket (STOMP) — Qui La Carne

Wszystkie powiadomienia w czasie rzeczywistym używają protokołu **STOMP** opartego na WebSocketach (z fallbackiem na
SockJS).

Aplikacja działa w architekturze **Push-to-Pull (Signal-and-Sync)**. Przez WebSockety NIE przesyłamy dużych obiektów
JSON z danymi. Serwer wysyła jedynie krótki sygnał (string) informujący o zmianie. Kiedy klient odbierze sygnał, jego
zadaniem jest wykonanie standardowego zapytania REST API (GET) w celu pobrania świeżych danych.

---

## 1. Nawiązywanie połączenia

**Adres endpointu:**

```
ws://<adres-serwera>/ws-qlc
```

> Jeśli korzystasz z biblioteki z obsługą SockJS (np. `stompjs`), użyj schematu `http://` zamiast `ws://`.

> **Autoryzacja:** Ponieważ nie jest to standardowy request HTTP, token JWT musisz podać w nagłówku ramki `CONNECT`.

**Przykład połączenia:**

```javascript
const client = new StompJs.Client({
    brokerURL: "ws://localhost:8080/ws-qlc",
    connectHeaders: {
        Authorization: "Bearer TWOJ_TOKEN_JWT",
    },
    onConnect: () => {
        client.subscribe("/topic/menu/updates", (message) => {
            console.log(message.body); // Akcja pobrania nowych danych
        });
    },
});
```

---

## 2. Kanały (Topics) — Kelner i Klient (React / Kotlin)

Te kanały są kluczowe do utrzymania spójności menu i mapy sali w trybie Offline.

### Menu i dostępność

| Topic                      | Kiedy                                                             | Payload                                                              |
|----------------------------|-------------------------------------------------------------------|----------------------------------------------------------------------|
| `/topic/menu/updates`      | Manager usunął, edytował lub dodał nowe danie.                    | `Dish removed: <token>` / `Dish updated: <token>` / `New dish added` |
| `/topic/menu`              | Zmieniono dostępność dania (np. brak składników, ręczna blokada). | `Dish <token> is now available/unavailable`                          |
| `/topic/menu/availability` | Usunięto składnik, co lawinowo zablokowało powiązane dania.       | `Multiple dishes disabled due to ingredient removal: <token>`        |

### Słowniki (alergeny, składniki, kategorie)

| Topic                         | Kiedy                                            | Payload                           |
|-------------------------------|--------------------------------------------------|-----------------------------------|
| `/topic/dictionary/sync`      | Zmiany w kategoriach menu lub liście składników. | `dish_categories` / `ingredients` |
| `/topic/dictionary/allergens` | Dodano lub usunięto alergen.                     | `Allergen list updated`           |

### Statusy stolików na sali

| Topic                  | Kiedy                                                               | Payload                                    |
|------------------------|---------------------------------------------------------------------|--------------------------------------------|
| `/topic/tables`        | Stolik zmienił swój status (np. do sprzątania, wyłączony z użytku). | `Table <token> status changed to <status>` |
| `/topic/tables/layout` | Manager dodał nowy fizyczny stolik na salę lub go usunął.           | `Table layout changed`                     |

---

## 3. Kanały (Topics) — Kuchnia i Manager (Monitor zamówień .NET)

### Zamówienia (kafelki na kuchni)

| Topic                   | Kiedy                                                                     | Payload                                                                                                                                     |
|-------------------------|---------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| `/topic/orders`         | Klient złożył zupełnie nowe zamówienie do rezerwacji (Pre-order).         | `New order for table: <tableToken>`                                                                                                         |
| `/topic/orders/updates` | Kelner domówił danie, anulował pozycję lub goście nie przyszli (No Show). | `Item added to order: <reservationToken>` / `Item removed from order: <reservationToken>` / `Order cancelled (No Show): <reservationToken>` |

### Rezerwacje

| Topic                         | Kiedy                                                                                                               | Payload               |
|-------------------------------|---------------------------------------------------------------------------------------------------------------------|-----------------------|
| `/topic/reservations/updates` | Ktoś stworzył nową rezerwację lub ją anulował; kelner został przypisany do stolika (zmiana statusu na IN_PROGRESS). | `Reservation changed` |

---

## 4. Kanały (Topics) — Bezpieczeństwo i Zarządzanie Personelem

### Zgłoszenia (wzywanie managera)

| Topic                    | Kiedy                                                                                           | Payload                                 |
|--------------------------|-------------------------------------------------------------------------------------------------|-----------------------------------------|
| `/topic/reports`         | Kelner zgłasza w aplikacji problematycznego gościa (kierownik sali dostaje alert na desktopie). | `New report from waiter: <waiterToken>` |
| `/topic/reports/updates` | Manager rozpatrzył zgłoszenie kelnera (zaakceptował lub odrzucił).                              | `Report resolved: <reportToken>`        |

### Bany (czarne listy)

| Topic                  | Kiedy                                                                                                        | Payload                                           |
|------------------------|--------------------------------------------------------------------------------------------------------------|---------------------------------------------------|
| `/topic/security/bans` | Klient dostał bana od managera lub ban naturalnie wygasł. Krytyczne dla urządzeń mobilnych w trybie offline. | `User banned: <token>` / `User unbanned: <token>` |

### Personel

| Topic                      | Kiedy                                                                                                      | Payload                  |
|----------------------------|------------------------------------------------------------------------------------------------------------|--------------------------|
| `/topic/personnel/updates` | Manager dodał nowego pracownika, edytował jego dane, zmienił rolę, zablokował konto lub usunął pracownika. | `Personnel list changed` |