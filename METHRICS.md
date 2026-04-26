# Observability — Monitoring & Logi

Stack: **Grafana Alloy → Prometheus + Loki → Grafana**

## Struktura plików

```
config/
├── alloy/
│   └── config.alloy        # Alloy — zbiera metryki i logi
├── grafana/
│   └── datasources.yml     # Auto-provisioning datasources
└── prometheus/
    └── prometheus.yml      # Konfiguracja Prometheusa
```

---

## Uruchomienie

```bash
docker compose up -d
```

---

## Weryfikacja — czy wszystko działa?

### 1. Spring Boot Actuator
```
http://localhost:8080/actuator/prometheus
```
Powinna pojawić się ściana metryk zaczynających się od `jvm_`, `http_server_requests_` itp.

### 2. Prometheus — Targets
```
http://localhost:9090/targets
```
Target `spring-boot-app` musi mieć status **UP**.
Jeśli jest **DOWN z błędem 403** — sprawdź czy `/actuator/**` jest odblokowany w `SecurityConfig`.

### 3. Alloy UI
```
http://localhost:12345
```
Wszystkie węzły w grafie powinny być zielone. Węzeł `prometheus.scrape.spring_boot` i `loki.source.docker.docker_logs` są kluczowe.

### 4. Grafana
```
http://localhost:3000
```
Login: wartości z `.env` → `GF_SECURITY_ADMIN_USER` / `GF_SECURITY_ADMIN_PASSWORD`

---

## Konfiguracja Grafany (pierwsze uruchomienie)

### Datasources

Wejdź w `Connections → Data sources` i dodaj ręcznie lub zweryfikuj czy auto-provisioning zadziałał:

| Datasource | URL | Default |
|------------|-----|---------|
| Prometheus | `http://prometheus:9090` | ✅ tak |
| Loki | `http://loki:3100` | ❌ nie |

> ⚠️ Ważne: URL musi wskazywać na nazwę serwisu Docker (`prometheus`, `loki`) — **nie** `localhost`.

Po dodaniu kliknij **Save & test** — oba powinny zwrócić zielony status.

### Dashboardy

`Dashboards → New → Import` → wpisz ID → wybierz datasource → **Import**

| ID | Dashboard | Datasource |
|----|-----------|------------|
| `19004` | Spring Boot 3.x (JVM, HTTP, Hikari, GC) | Prometheus |
| `763` | Redis | Prometheus |
| `13639` | Loki Logs Explorer | Loki |

---

## Porty

| Serwis | Port | URL |
|--------|------|-----|
| Spring Boot API | 8080 | http://localhost:8080 |
| Prometheus | 9090 | http://localhost:9090 |
| Grafana | 3000 | http://localhost:3000 |
| Loki | 3100 | http://localhost:3100 |
| Alloy UI | 12345 | http://localhost:12345 |
| Redis Exporter | 9121 | http://localhost:9121/metrics |

Porty można nadpisać przez zmienne w `.env`.

---

## Zmienne środowiskowe (.env)

```env
PROMETHEUS_PORT=9090
LOKI_PORT=3100
ALLOY_PORT=12345
GF_PORT=3000
GF_SECURITY_ADMIN_USER=admin
GF_SECURITY_ADMIN_PASSWORD=admin
```