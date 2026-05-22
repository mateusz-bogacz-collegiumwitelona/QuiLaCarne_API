# WebSocket — dokumentacja dla frontendu

> **Dla kogo jest ten dokument:** Dla każdego kto chce podłączyć się do backendu i odbierać dane w czasie rzeczywistym. Zakładamy zero wiedzy o WebSocketach.

---

## Czym w ogóle jest ten WebSocket?

Normalny HTTP działa tak: frontend pyta → backend odpowiada → koniec. Musisz pytać co chwilę żeby wiedzieć co nowego.

WebSocket działa inaczej: otwierasz jedno stałe połączenie i backend **sam** ci wysyła wiadomość gdy coś się zmieni. Jak subskrypcja powiadomień.

Protokół który tu używamy to **STOMP** (prosty protokół komunikacji) transportowany przez **SockJS** (biblioteka która działa nawet gdy WebSocket jest zablokowany przez sieć — fallback na polling).

---

## Anatomia wiadomości — dokładnie co przychodzi

Każda wiadomość z backendu to JSON o tej samej strukturze. **Zawsze. Bez wyjątku.**

```json
{
  "eventType": "CREATED",
  "entityType": "ORDER",
  "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "payload": { ... },
  "timestamp": "2025-01-15T14:30:00.123Z"
}
```

### Każde pole z osobna

---

#### `eventType` — co się stało

Jedna z trzech wartości:

| Wartość   | Znaczenie                          | Co zrobić na froncie             |
|-----------|------------------------------------|----------------------------------|
| `CREATED` | Coś nowego pojawiło się w systemie | Dodaj do listy/store             |
| `UPDATED` | Coś istniejącego zostało zmienione | Znajdź po `token` i zaktualizuj  |
| `DELETED` | Coś zostało usunięte               | Znajdź po `token` i usuń z listy |

---

#### `entityType` — jakiego typu jest ta rzecz

String który mówi o czym jest wiadomość. Pełna lista:

| Wartość             | Co oznacza                          |
|---------------------|-------------------------------------|
| `DISH`              | Danie w menu                        |
| `CATEGORY`          | Kategoria dań                       |
| `ORDER`             | Zamówienie                          |
| `ORDER_ITEM`        | Pojedyncza pozycja w zamówieniu     |
| `ORDER_STATUS`      | Status zamówienia (słownik)         |
| `ORDER_ITEM_STATUS` | Status pozycji zamówienia (słownik) |
| `RESERVATION`       | Rezerwacja stolika                  |
| `TABLE`             | Stolik                              |
| `TABLE_STATUS`      | Status stolika (słownik)            |
| `EMPLOYEE`          | Pracownik                           |
| `BAN`               | Ban użytkownika                     |
| `REPORT`            | Zgłoszenie/raport                   |
| `ALLERGEN`          | Alergen (słownik)                   |
| `INGREDIENT`        | Składnik dania                      |

---

#### `token` — identyfikator konkretnej rzeczy

Unikalny string identyfikujący encję. Przy `UPDATED` i `DELETED` to jest twój klucz do znalezienia elementu w lokalnym stanie i zaktualizowania/usunięcia go.

**Ważne przy `DELETED`:** `payload` jest wtedy `null`. Jedyne co masz do dyspozycji to właśnie `token` i `entityType`.

---

#### `payload` — dane encji

Obiekt z danymi. Struktura zależy od `entityType` — pełna lista niżej w sekcji "Topici i ich payloady".

**Przy `DELETED` payload jest zawsze `null`.** Nie próbuj go czytać.

---

#### `timestamp` — kiedy to się wydarzyło

Data i czas w formacie ISO 8601, strefa UTC. Przykład: `2025-01-15T14:30:00.123Z`

Możesz użyć do sortowania eventów lub wyświetlania "ostatnio zaktualizowano".

---

## Lista wszystkich topicow

> Subskrybujesz pełny adres z `/topic/` na początku.

| Topic do subskrypcji                    | Co tam leci                              | Entity type w wiadomości |
|-----------------------------------------|------------------------------------------|--------------------------|
| `/topic/menu/dishes`                    | Zmiany w daniach                         | `DISH`                   |
| `/topic/dictionary/dish-categories`     | Zmiany kategorii dań                     | `CATEGORY`               |
| `/topic/orders/updates`                 | Zmiany zamówień                          | `ORDER`                  |
| `/topic/orders/items`                   | Zmiany pozycji zamówień                  | `ORDER_ITEM`             |
| `/topic/dictionary/order-statuses`      | Zmiany statusów zamówień                 | `ORDER_STATUS`           |
| `/topic/dictionary/order-item-statuses` | Zmiany statusów pozycji                  | `ORDER_ITEM_STATUS`      |
| `/topic/reservations/updates`           | Zmiany rezerwacji                        | `RESERVATION`            |
| `/topic/tables/updates`                 | Zmiany stolików                          | `TABLE`                  |
| `/topic/dictionary/table-statuses`      | Zmiany statusów stolików                 | `TABLE_STATUS`           |
| `/topic/personnel/updates`              | Zmiany pracowników                       | `EMPLOYEE`               |
| `/topic/security/bans`                  | Bany użytkowników                        | `BAN`                    |
| `/topic/reports/updates`                | Raporty/zgłoszenia                       | `REPORT`                 |
| `/topic/dictionary/allergens`           | Zmiany alergenów                         | `ALLERGEN`               |
| `/topic/dictionary/sync`                | Dodanie składnika                        | `INGREDIENT`             |
| `/topic/menu/availability`              | Usunięcie składnika (dania mogą zniknąć) | `INGREDIENT`             |

---

## Payloady — co jest w `payload` dla każdego entity type

### DISH — danie

```json
{
  "token": "dish-abc-123",
  "name": "Margherita",
  "price": 2500,
  "isAvailable": true,
  "unavailableReason": null,
  "imageUrl": "https://cdn.example.com/margherita.jpg",
  "categoryToken": "cat-pizza-456",
  "ingredientTokens": ["ing-111", "ing-222", "ing-333"]
}
```

> `price` jest w **groszach**. 2500 = 25,00 zł. Dziel przez 100 przed wyświetleniem.
> Gdy `isAvailable: false` — pokaż `unavailableReason` użytkownikowi.

---

### CATEGORY — kategoria dań

```json
{
  "token": "cat-pizza-456",
  "nameEn": "Pizza",
  "namePl": "Pizza"
}
```

---

### ORDER — zamówienie

```json
{
  "token": "order-xyz-789",
  "reservationToken": "res-abc-111",
  "tableToken": "table-5-222",
  "waiterToken": "user-jan-333",
  "statusTokens": ["status-active-444"],
  "totalPrice": 7500,
  "createdAt": "2025-01-15T12:00:00Z",
  "updatedAt": "2025-01-15T12:30:00Z"
}
```

> `totalPrice` w groszach. `reservationToken` może być null jeśli zamówienie bez rezerwacji.

---

### ORDER_ITEM — pozycja w zamówieniu

```json
{
  "token": "item-aaa-111",
  "orderToken": "order-xyz-789",
  "productToken": "dish-abc-123",
  "statusTokens": ["status-bbb-222"],
  "quantity": 2,
  "priceAtTimeOfOrder": 2500,
  "note": "bez cebuli",
  "createdAt": "2025-01-15T12:00:00Z",
  "updatedAt": "2025-01-15T12:05:00Z"
}
```

> `priceAtTimeOfOrder` — cena z momentu złożenia zamówienia (w groszach), może różnić się od aktualnej ceny dania.

---

### ORDER_STATUS / ORDER_ITEM_STATUS / CATEGORY / TABLE_STATUS / ALLERGEN — słowniki

Wszystkie słowniki mają ten sam format:

```json
{
  "token": "status-token-123",
  "nameEn": "In progress",
  "namePl": "W trakcie"
}
```

---

### RESERVATION — rezerwacja

```json
{
  "token": "res-abc-111",
  "userToken": "user-jan-333",
  "tableToken": "table-5-222",
  "statusTokens": ["status-ccc-555"],
  "startTime": "2025-01-15T18:00:00Z",
  "endTime": "2025-01-15T20:00:00Z",
  "createdAt": "2025-01-15T10:00:00Z",
  "updatedAt": "2025-01-15T10:00:00Z"
}
```

---

### TABLE — stolik

```json
{
  "token": "table-5-222",
  "tableNumber": 5,
  "capacity": 4,
  "statusTokens": ["status-ddd-666"],
  "createdAt": "2025-01-01T08:00:00Z",
  "updatedAt": "2025-01-15T14:00:00Z"
}
```

---

### EMPLOYEE — pracownik

```json
{
  "token": "user-jan-333",
  "username": "jan.kowalski",
  "email": "jan@restauracja.pl",
  "isActive": true,
  "isStaff": true,
  "roleTokens": ["role-waiter-777"],
  "createdAt": "2025-01-01T08:00:00Z",
  "updatedAt": "2025-01-15T08:00:00Z"
}
```

---

### BAN — ban użytkownika

```json
{
  "token": "ban-eee-888",
  "userToken": "user-klient-999",
  "bannedByToken": "user-jan-333",
  "statusTokens": ["status-active-444"],
  "reason": "Nieodpowiednie zachowanie",
  "expiresAt": "2025-02-15T00:00:00Z",
  "isActive": true,
  "createdAt": "2025-01-15T12:00:00Z",
  "updatedAt": "2025-01-15T12:00:00Z"
}
```

> `expiresAt` może być null — wtedy ban jest permanentny (lub do ręcznego zdjęcia).
> Gdy ban wygaśnie — backend sam wyśle `UPDATED` z `isActive: false`.

---

### REPORT — zgłoszenie

```json
{
  "token": "report-fff-000",
  "guestToken": "user-klient-999",
  "reporterToken": "user-jan-333",
  "statusTokens": ["status-in-progress-111"],
  "reason": "Agresywne zachowanie wobec personelu",
  "createdAt": "2025-01-15T13:00:00Z",
  "updatedAt": "2025-01-15T13:00:00Z"
}
```

---

### INGREDIENT — składnik

**Przy dodaniu** (`CREATED`, topic: `/topic/dictionary/sync`):

```json
{
  "token": "ing-111",
  "nameEn": "Tomato",
  "namePl": "Pomidor",
  "allergenTokens": ["allergen-222"]
}
```

**Przy usunięciu** (`DELETED`, topic: `/topic/menu/availability`):

```json
payload: null
```

> Po `DELETED` na tym topicu — dania zawierające ten składnik mogą zmienić `isAvailable` na `false`. Spodziewaj się eventów `UPDATED` na `/topic/menu/dishes`.

---

## Implementacja — JavaScript / TypeScript (React)

### Instalacja

```bash
npm install @stomp/stompjs sockjs-client
npm install -D @types/sockjs-client  # jeśli TypeScript
```

### Hook

```typescript
// hooks/useWebSocket.ts
import { useEffect, useRef } from "react";
import { Client, IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";

const API_URL = "http://localhost:8080";

export interface WebSocketEvent<T = unknown> {
  eventType: "CREATED" | "UPDATED" | "DELETED";
  entityType: string;
  token: string;
  payload: T | null;
  timestamp: string;
}

const ALL_TOPICS = [
  "/topic/menu/dishes",
  "/topic/dictionary/dish-categories",
  "/topic/orders/updates",
  "/topic/orders/items",
  "/topic/dictionary/order-statuses",
  "/topic/dictionary/order-item-statuses",
  "/topic/reservations/updates",
  "/topic/tables/updates",
  "/topic/dictionary/table-statuses",
  "/topic/personnel/updates",
  "/topic/security/bans",
  "/topic/reports/updates",
  "/topic/dictionary/allergens",
  "/topic/dictionary/sync",
  "/topic/menu/availability",
];

export function useWebSocket(
  token: string | null,
  onEvent: (event: WebSocketEvent) => void
) {
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_URL}/ws-qlc`),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,

      onConnect: () => {
        console.log("WebSocket połączony");
        ALL_TOPICS.forEach((topic) => {
          client.subscribe(topic, (message: IMessage) => {
            const event: WebSocketEvent = JSON.parse(message.body);
            onEvent(event);
          });
        });
      },

      onDisconnect: () => console.log("WebSocket rozłączony"),
      onStompError: (frame) =>
        console.error("STOMP error:", frame.headers["message"]),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [token]);
}
```

### Użycie

```typescript
// App.tsx
import { useWebSocket, WebSocketEvent } from "./hooks/useWebSocket";

function App() {
  const token = useAuthToken(); // twój JWT skądkolwiek

  useWebSocket(token, (event: WebSocketEvent) => {
    // Krok 1: co to za encja?
    switch (event.entityType) {
      case "ORDER":
        handleOrderEvent(event);
        break;
      case "TABLE":
        handleTableEvent(event);
        break;
      // ...itd
    }
  });
}

function handleOrderEvent(event: WebSocketEvent) {
  // Krok 2: co się stało?
  switch (event.eventType) {
    case "CREATED":
      // event.payload ma dane nowego zamówienia
      addOrderToStore(event.payload);
      break;

    case "UPDATED":
      // znajdź po tokenie i zaktualizuj
      updateOrderInStore(event.token, event.payload);
      break;

    case "DELETED":
      // payload jest null! używaj tylko event.token
      removeOrderFromStore(event.token);
      break;
  }
}
```

---

## Implementacja — .NET (C#)

### Instalacja NuGet

```bash
dotnet add package Microsoft.AspNetCore.SignalR.Client
# STOMP przez SockJS w .NET najlepiej przez:
dotnet add package Stomp.Net
```

> W .NET najwygodniej używać czystego WebSocket + STOMP ręcznie, lub biblioteki `Stomp.Net`. Poniżej przykład z `System.Net.WebSockets` + prosta obsługa STOMP.

```csharp
// WebSocketService.cs
using System.Net.WebSockets;
using System.Text;
using System.Text.Json;

public class WebSocketEvent<T>
{
    public string EventType { get; set; }   // "CREATED" | "UPDATED" | "DELETED"
    public string EntityType { get; set; }  // "ORDER" | "TABLE" | itd.
    public string Token { get; set; }
    public T? Payload { get; set; }
    public DateTime Timestamp { get; set; }
}

public class RestaurantWebSocketService
{
    private ClientWebSocket _ws = new();
    private readonly string _jwtToken;
    private readonly string _serverUrl = "ws://localhost:8080/ws-qlc/websocket";

    public event Action<string>? OnRawMessage;

    public RestaurantWebSocketService(string jwtToken)
    {
        _jwtToken = jwtToken;
    }

    public async Task ConnectAsync(CancellationToken ct = default)
    {
        // STOMP wymaga nagłówka Authorization przy CONNECT frame
        _ws.Options.SetRequestHeader("Authorization", $"Bearer {_jwtToken}");
        await _ws.ConnectAsync(new Uri(_serverUrl), ct);

        // Wyślij CONNECT frame (STOMP handshake)
        await SendStompFrame("CONNECT", new Dictionary<string, string>
        {
            ["accept-version"] = "1.1,1.2",
            ["Authorization"] = $"Bearer {_jwtToken}"
        }, ct);

        // Poczekaj na CONNECTED od serwera
        await ReceiveFrameAsync(ct);

        // Subskrybuj topici
        var topics = new[]
        {
            "/topic/orders/updates",
            "/topic/menu/dishes",
            "/topic/tables/updates",
            "/topic/reservations/updates",
            "/topic/personnel/updates",
            "/topic/security/bans",
            "/topic/reports/updates",
            "/topic/orders/items",
            "/topic/dictionary/order-statuses",
            "/topic/dictionary/order-item-statuses",
            "/topic/dictionary/dish-categories",
            "/topic/dictionary/table-statuses",
            "/topic/dictionary/allergens",
            "/topic/dictionary/sync",
            "/topic/menu/availability",
        };

        foreach (var (topic, i) in topics.Select((t, i) => (t, i)))
        {
            await SendStompFrame("SUBSCRIBE", new Dictionary<string, string>
            {
                ["id"] = $"sub-{i}",
                ["destination"] = topic
            }, ct);
        }
    }

    public async Task ListenAsync(CancellationToken ct = default)
    {
        while (_ws.State == WebSocketState.Open && !ct.IsCancellationRequested)
        {
            var raw = await ReceiveFrameAsync(ct);
            if (raw.StartsWith("MESSAGE"))
            {
                // Wyciągnij body (po podwójnym \n)
                var body = raw[(raw.IndexOf("\n\n") + 2)..].TrimEnd('\0');
                OnRawMessage?.Invoke(body);

                // Zdekoduj i obsłuż
                HandleMessage(body);
            }
        }
    }

    private void HandleMessage(string json)
    {
        // Najpierw zdekoduj bazowy event żeby sprawdzić entityType
        var baseEvent = JsonSerializer.Deserialize<WebSocketEvent<JsonElement>>(json,
            new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

        if (baseEvent == null) return;

        Console.WriteLine($"[WS] {baseEvent.EventType} | {baseEvent.EntityType} | {baseEvent.Token}");

        switch (baseEvent.EntityType)
        {
            case "ORDER":
                var orderEvent = JsonSerializer.Deserialize<WebSocketEvent<OrderPayload>>(json,
                    new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                HandleOrderEvent(orderEvent!);
                break;

            case "TABLE":
                var tableEvent = JsonSerializer.Deserialize<WebSocketEvent<TablePayload>>(json,
                    new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                HandleTableEvent(tableEvent!);
                break;

            // ...dodaj kolejne case'y wg potrzeb
        }
    }

    private void HandleOrderEvent(WebSocketEvent<OrderPayload> e)
    {
        switch (e.EventType)
        {
            case "CREATED":
                // e.Payload ma dane zamówienia
                Console.WriteLine($"Nowe zamówienie: {e.Token}, kwota: {e.Payload?.TotalPrice / 100m} zł");
                break;
            case "UPDATED":
                Console.WriteLine($"Aktualizacja zamówienia: {e.Token}");
                break;
            case "DELETED":
                // e.Payload jest null!
                Console.WriteLine($"Usunięto zamówienie: {e.Token}");
                break;
        }
    }

    private void HandleTableEvent(WebSocketEvent<TablePayload> e)
    {
        switch (e.EventType)
        {
            case "UPDATED":
                Console.WriteLine($"Zmiana stolika nr {e.Payload?.TableNumber}: token {e.Token}");
                break;
        }
    }

    private async Task SendStompFrame(string command, Dictionary<string, string> headers,
        CancellationToken ct)
    {
        var sb = new StringBuilder();
        sb.AppendLine(command);
        foreach (var (k, v) in headers) sb.AppendLine($"{k}:{v}");
        sb.Append("\n\0");

        var bytes = Encoding.UTF8.GetBytes(sb.ToString());
        await _ws.SendAsync(bytes, WebSocketMessageType.Text, true, ct);
    }

    private async Task<string> ReceiveFrameAsync(CancellationToken ct)
    {
        var buffer = new byte[65536];
        var result = await _ws.ReceiveAsync(buffer, ct);
        return Encoding.UTF8.GetString(buffer, 0, result.Count);
    }
}

// Modele payloadów
public record OrderPayload(
    string Token,
    string? ReservationToken,
    string TableToken,
    string WaiterToken,
    List<string> StatusTokens,
    int TotalPrice,
    DateTime CreatedAt,
    DateTime UpdatedAt
);

public record TablePayload(
    string Token,
    int TableNumber,
    int Capacity,
    List<string> StatusTokens,
    DateTime CreatedAt,
    DateTime UpdatedAt
);

// Program.cs — użycie
var service = new RestaurantWebSocketService(jwtToken);
service.OnRawMessage += raw => Console.WriteLine($"Raw: {raw}");
await service.ConnectAsync();
await service.ListenAsync();
```

---

## Implementacja — Kotlin (Android / JVM)

### Gradle dependencies

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.hildan.krossbow:krossbow-stomp-core:7.1.0")
    implementation("org.hildan.krossbow:krossbow-websocket-okhttp:7.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

```kotlin
// WebSocketEvent.kt
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WebSocketEvent(
    val eventType: String,      // "CREATED" | "UPDATED" | "DELETED"
    val entityType: String,     // "ORDER" | "TABLE" | itd.
    val token: String,
    val payload: JsonElement?,  // null przy DELETED
    val timestamp: String
)

// Payloady
@Serializable
data class OrderPayload(
    val token: String,
    val reservationToken: String?,
    val tableToken: String,
    val waiterToken: String,
    val statusTokens: List<String>,
    val totalPrice: Int,        // w groszach!
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class TablePayload(
    val token: String,
    val tableNumber: Int,
    val capacity: Int,
    val statusTokens: List<String>,
    val createdAt: String,
    val updatedAt: String
)
```

```kotlin
// RestaurantWebSocketService.kt
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient

class RestaurantWebSocketService(
    private val jwtToken: String,
    private val serverUrl: String = "http://10.0.2.2:8080/ws-qlc" // 10.0.2.2 = localhost na emulatorze Android
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val allTopics = listOf(
        "/topic/orders/updates",
        "/topic/menu/dishes",
        "/topic/tables/updates",
        "/topic/reservations/updates",
        "/topic/personnel/updates",
        "/topic/security/bans",
        "/topic/reports/updates",
        "/topic/orders/items",
        "/topic/dictionary/order-statuses",
        "/topic/dictionary/order-item-statuses",
        "/topic/dictionary/dish-categories",
        "/topic/dictionary/table-statuses",
        "/topic/dictionary/allergens",
        "/topic/dictionary/sync",
        "/topic/menu/availability",
    )

    // Publiczny flow — zbiera eventy ze wszystkich topicow w jeden strumień
    private val _events = MutableSharedFlow<WebSocketEvent>()
    val events: SharedFlow<WebSocketEvent> = _events.asSharedFlow()

    suspend fun connect() {
        val client = StompClient(OkHttpWebSocketClient())

        val session = client.connect(
            url = serverUrl,
            customStompConnectHeaders = mapOf(
                "Authorization" to "Bearer $jwtToken"
            )
        )

        // Subskrybuj wszystkie topici równolegle
        coroutineScope {
            allTopics.forEach { topic ->
                launch {
                    session.subscribeText(topic).collect { rawMessage ->
                        try {
                            val event = json.decodeFromString<WebSocketEvent>(rawMessage)
                            _events.emit(event)
                        } catch (e: Exception) {
                            println("Błąd parsowania wiadomości: ${e.message}")
                        }
                    }
                }
            }
        }
    }
}
```

```kotlin
// ViewModel — jak używać
class RestaurantViewModel : ViewModel() {
    private val wsService = RestaurantWebSocketService(jwtToken = getJwt())
    private val json = Json { ignoreUnknownKeys = true }

    init {
        viewModelScope.launch {
            wsService.connect()
        }

        viewModelScope.launch {
            wsService.events.collect { event ->
                handleEvent(event)
            }
        }
    }

    private fun handleEvent(event: WebSocketEvent) {
        when (event.entityType) {
            "ORDER"  -> handleOrderEvent(event)
            "TABLE"  -> handleTableEvent(event)
            "DISH"   -> handleDishEvent(event)
            // ...reszta encji
        }
    }

    private fun handleOrderEvent(event: WebSocketEvent) {
        when (event.eventType) {
            "CREATED" -> {
                val order = json.decodeFromJsonElement(OrderPayload.serializer(), event.payload!!)
                // dodaj do listy zamówień
                println("Nowe zamówienie: ${order.token}, ${order.totalPrice / 100.0} zł")
            }
            "UPDATED" -> {
                val order = json.decodeFromJsonElement(OrderPayload.serializer(), event.payload!!)
                // znajdź po token i zaktualizuj
                println("Aktualizacja zamówienia: ${event.token}")
            }
            "DELETED" -> {
                // payload jest null — używaj event.token
                println("Usunięto zamówienie: ${event.token}")
            }
        }
    }

    private fun handleTableEvent(event: WebSocketEvent) {
        when (event.eventType) {
            "UPDATED" -> {
                val table = json.decodeFromJsonElement(TablePayload.serializer(), event.payload!!)
                println("Stolik ${table.tableNumber} zaktualizowany")
            }
        }
    }

    private fun handleDishEvent(event: WebSocketEvent) {
        when (event.eventType) {
            "UPDATED" -> {
                // sprawdź czy isAvailable zmieniło się na false
                println("Danie ${event.token} zaktualizowane")
            }
            "DELETED" -> {
                println("Danie ${event.token} usunięte")
            }
        }
    }
}
```

---

## Najczęstsze błędy

| Problem                          | Przyczyna                       | Rozwiązanie                                                |
|----------------------------------|---------------------------------|------------------------------------------------------------|
| Brak połączenia / 401            | Token JWT wygasł lub go nie ma  | Odśwież token i reconnect                                  |
| `payload` jest `null`            | Event to `DELETED` — tak ma być | Używaj tylko `token` do identyfikacji                      |
| Nie dostaję eventów              | Zły adres topicu                | Sprawdź tabelę wyżej — prefiks `/topic/` jest obowiązkowy  |
| Dostaje event ale nie wiem co to | `entityType` nieznany           | Dodaj `else`/`default` w switchu który loguje nieznany typ |
| Połączenie spada co chwilę       | Timeout / sieć                  | `reconnectDelay: 5000` wznowi automatycznie                |
| Zduplikowane subskrypcje         | Subscribe wywołany wielokrotnie | Subskrybuj tylko raz, w callbacku `onConnect`              |

---

## Jak to działa od środka (dla ciekawskich)

```
Twoja aplikacja              Backend
      |                         |
      |-- CONNECT + JWT -------->|  Backend weryfikuje token JWT
      |<-- CONNECTED ------------|
      |                         |
      |-- SUBSCRIBE /topic/orders/updates
      |                         |
      |                         |  (ktoś składa zamówienie)
      |<-- MESSAGE --------------|  { eventType: "CREATED", entityType: "ORDER", ... }
      |                         |
      |                         |  (kelner zmienia status)
      |<-- MESSAGE -------------|  { eventType: "UPDATED", entityType: "ORDER", ... }
```

Backend wysyła eventy **asynchronicznie** — dostajesz wiadomość gdy coś się dzieje, nie musisz pytać co chwilę.