# Flowero Discover 🟢

> **Eureka Service Registry** for the [Panomete Platform](https://github.com/oat431/project_spec)
>
> Java 25 · Spring Boot 4.1 · Spring Cloud 2025.1 (Oakwood)

---

## What It Does

Flowero Discover is the **service registry** — every microservice registers itself on startup, and services discover each other by logical name instead of hardcoded URLs. It runs Spring Cloud Netflix Eureka in standalone mode.

| Aspect | Detail |
|--------|--------|
| **Service Type** | Foundation — Service Registry |
| **Technology** | Spring Cloud Netflix Eureka Server |
| **Stack** | Java 25 / Spring Boot 4.1 / Spring Cloud 2025.1.2 |
| **Ports** | 8999 (REST API) · 3999 (Dashboard, via Docker port mapping) |
| **Domain** | `discovery.panomete.com` (via Nginx) |
| **Database** | None — fully in-memory |
| **Mode** | Standalone (single node) |

---

## Architecture

```
                    ┌─ REST API ─── :8999 ─── service registration
                    │                          heartbeat (PUT /eureka/apps/...)
  flowero-discover ─┤                          discovery queries (GET /eureka/apps)
                    │
                    └─ Dashboard ─── :3999 ─── discovery.panomete.com
                                               (Nginx proxy → :3999 → :8999 dashboard)
```

### How Services Use It

```
1. Service boots → registers with Eureka (name, host, port, health URL)
2. Service sends heartbeat every 30s (PUT /eureka/apps/{app}/{instance})
3. Gate resolves lb://cute-gufo → queries Eureka → returns host:port
4. Dead instance: heartbeats fail → evicted after 90s
```

---

## Quick Start

### Prerequisites

- **JDK 25** ([Eclipse Temurin](https://adoptium.net/) recommended)
- **Gradle** (wrapper included — `./gradlew`)

### Build & Run Locally

```bash
# Build
./gradlew build

# Run
./gradlew bootRun
```

Eureka starts on **port 8999**:
- **Dashboard:** http://localhost:8999/
- **Health:** http://localhost:8999/actuator/health
- **Registry API:** http://localhost:8999/eureka/apps

### Docker

```bash
# Build image
docker build -t flowero-discover .

# Run container
docker run -p 8999:8999 -p 3999:8999 flowero-discover
```

---

## Configuration

Key settings in `application.yaml`:

| Property | Value | Why |
|----------|-------|-----|
| `server.port` | `8999` | REST API port |
| `eureka.client.register-with-eureka` | `false` | Standalone — don't self-register |
| `eureka.client.fetch-registry` | `false` | Standalone — don't fetch from self |
| `eureka.server.enable-self-preservation` | `true` | Keeps registry intact during network hiccups |
| `eureka.server.eviction-interval-timer-in-ms` | `5000` | Evict dead instances every 5s |
| `eureka.server.renewal-percent-threshold` | `0.85` | Self-preservation triggers below 85% heartbeats |

---

## API Reference

See the full [API Specification](F:\projects\project_spec\spec\flowero_discover\02_design\022_API_specification.md).

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/eureka/apps` | `GET` | List all registered applications |
| `/eureka/apps/{name}` | `GET` | Get instances for a specific service |
| `/eureka/apps/{name}` | `POST` | Register a new instance |
| `/eureka/apps/{name}/{instance}` | `PUT` | Heartbeat / renew lease |
| `/eureka/apps/{name}/{instance}` | `DELETE` | Deregister an instance |
| `/actuator/health` | `GET` | Health check (returns `{"status":"UP"}`) |
| `/` | `GET` | Eureka dashboard (HTML) |

---

## Related Services

| Service | How It Uses Discover |
|---------|---------------------|
| **Flowero Gate** | Resolves `lb://` routes to business services |
| **Flowero Guard** | Registers health for monitoring |
| **All business services** | Register on startup, discover peers by name |

---

## Design Decisions

| ADR | Decision |
|-----|----------|
| ADR-D001 | Standalone single-node Eureka |
| ADR-D003 | Dashboard via Nginx at `discovery.panomete.com` |
| ADR-D004 | Self-preservation enabled for homelab stability |
| ADR-D005 | Dual ports: 8999 (API) + 3999 (dashboard) |

Full ADRs: [021_architecture_decision_records.md](F:\projects\project_spec\spec\flowero_discover\02_design\021_architecture_decision_records.md)

---

## Testing

```bash
# Run all tests
./gradlew test

# Run with detailed output
./gradlew test --info
```

Tests cover:
- ✅ Context loads with Eureka server enabled
- ✅ Dashboard is reachable at `/`
- ✅ Actuator health returns UP
- ✅ `/eureka/apps` returns valid response (XML + JSON)
- ✅ No self-registration (standalone mode verified)

---

## Reference

- [Spring Cloud Netflix — Eureka Server](https://docs.spring.io/spring-cloud-netflix/reference/spring-cloud-netflix.html#spring-cloud-eureka-server)
- [Service Registration and Discovery Guide](https://spring.io/guides/gs/service-registration-and-discovery/)
- [Panomete Platform SAD](F:\projects\project_spec\spec\panomete_platform\02_design\025_software_architecture_document.md)
