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

Wykonaj kroki konfiguracyjne opisane powyżej (sekcja 1), a następnie uruchom testy:

```bash
docker compose run --rm test
```

---

## Dodawanie admin bez dostępu do jakiego kolwiek forntendu

1. Zatrzymaj kontener
2. Wykonaj tą komende `docker compose run --rm -it api --create-admin`
3. Uzupełij dane
4. Włącz na nowo kontener

## Przydatne adresy

| Usługa  | Adres                                       |
|---------|---------------------------------------------|
| Swagger | http://localhost:8080/swagger-ui/index.html |
| Mailpit | http://localhost:8025/                      |
| Minio   | http://localhost:9001/                      |

### Po klucz do google zgłosić się do matiego