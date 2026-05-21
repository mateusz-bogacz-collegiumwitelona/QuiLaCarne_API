# Qui La Carme — Web

## Uruchomienie projektu

### 1. Konfiguracja zmiennych środowiskowych

Zmień nazwę pliku `.env.example` na `.env`, a następnie otwórz go w edytorze.

Wejdź na stronę [https://jwtsecrets.com/#generator](https://jwtsecrets.com/#generator), skopiuj wygenerowany klucz i
wklej go do zmiennej `API_JWT_SECRET_KEY`.

### 2. Uruchomienie kontenera

```bash
docker compose up --build
```

---

## Uruchamianie testów

Wykonaj następującą komendę

```bash
docker compose -f compose.test.yml run --rm test
```

---

## Dodawanie admin bez dostępu do jakiego kolwiek forntendu

1. Zatrzymaj kontener
2. Wykonaj tą komende `docker compose run --rm -it api --create-admin`
3. Uzupełij dane
4. Włącz na nowo kontener

## Przydatne adresy

| Usługa  | Adres                                       |
| ------- | ------------------------------------------- |
| Swagger | http://localhost:8080/swagger-ui/index.html |
| Mailpit | http://localhost:8025/                      |
| Minio   | http://localhost:9001/                      |

## Tabela komend java

| Komenada                     | Opis                                                                |
| ---------------------------- | ------------------------------------------------------------------- |
| `./gradlew spotlessApply`    | Automatycznie formatuje i naprawia styl kodu Javy.                  |
| `./gradlew spotlessCheck`    | Sprawdza poprawność formatowania (bez wprowadzania zmian).          |
| `./gradlew spotbugsMain`     | Skanuje kod w poszukiwaniu ukrytych błędów i luk.                   |
| `./gradlew check`            | Uruchamia zbiorczą weryfikację jakości projektu (m m.in. SpotBugs). |
| `./gradlew test`             | Uruchamia wszystkie testy jednostkowe.                              |
| `./gradlew jacocoTestReport` | Generuje raport pokazujący, ile kodu jest pokryte testami.          |
| `./gradlew bootRun`          | Uruchamia aplikację Spring Boot lokalnie (serwer deweloperski).     |
| `./gradlew bootJar`          | Buduje gotową paczkę aplikacji (plik `.jar`) do wdrożenia.          |
| `./gradlew clean`            | Usuwa stare, skompilowane pliki i raporty (czyści środowisko).      |

## Ważne info

1. **Po klucz do google zgłosić się do matiego**
2. **Dokumentacja WebSockets jest dostępna w pliku [WEBSOCKETS.md](WEBSOCKETS_2.md)**
3. **Dokumentacja api online [API](https://mateusz-bogacz-collegiumwitelona.github.io/QuiLaCarne_API/)**
