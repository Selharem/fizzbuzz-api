# FizzBuzz API

A production-oriented REST service that generates configurable FizzBuzz sequences and persists the most frequently requested parameter set.

## Technology

- Java 25 (current LTS release)
- Spring Boot 4.1.1
- PostgreSQL 18 with Flyway migrations
- Gradle 9.8 with dependency locking and checksum verification
- JUnit 6, AssertJ, Mockito, and Testcontainers 2
- Actuator health probes and Prometheus metrics on a separate management port
- OpenAPI 3 specification and Swagger UI (springdoc)
- Structured JSON logs (Elastic Common Schema) in the container image
- ArchUnit, Checkstyle, JaCoCo, Gitleaks, and Trivy quality and security gates

The application uses virtual threads, graceful shutdown, response compression, RFC 9457 problem details, database constraints, and an atomic PostgreSQL upsert for request statistics.

## Architecture

The service follows a hexagonal (ports and adapters) architecture. The business core is plain Java with no framework dependencies; Spring, HTTP, and SQL live only in adapters.

```text
io.github.selharem.fizzbuzz
├── domain
│   ├── model                  FizzBuzzParameters, RequestStatistics (immutable, self-validating)
│   └── service                FizzBuzzGenerator (pure FizzBuzz logic)
├── application
│   ├── port/in                GenerateFizzBuzzUseCase, GetMostFrequentRequestUseCase
│   ├── port/out               RequestStatisticsPort
│   └── service                FizzBuzzService (implements the use cases)
├── adapter
│   ├── in/web                 REST controller, request/response DTOs, error handling
│   └── out/persistence        PostgreSQL implementation of RequestStatisticsPort
└── config                     Composition root: wires the core into Spring, OpenAPI metadata
```

Dependencies point inward: adapters depend on ports, and the core never depends on adapters or Spring. `HexagonalArchitectureTest` (ArchUnit) enforces these rules in the build.

Examples of changes that touch a single area:

- Change the FizzBuzz rules: `domain/service`
- Store statistics in Redis instead of PostgreSQL: add an adapter implementing `RequestStatisticsPort`
- Expose the use cases over gRPC or a CLI: add an inbound adapter calling the use-case interfaces

## Requirements

- Docker with the Compose plugin for the simplest setup
- Java 25 only when running outside Docker

Gradle downloads the required Java toolchain automatically when a compatible JDK is not installed.

## Run with Docker Compose

Generate a local database password. It is stored in `secrets/`, which is ignored by Git and Docker:

```sh
mkdir -p secrets
openssl rand -hex 24 | tr -d '\n' > secrets/db_password
```

Build and start the service:

```sh
docker compose up --build
```

The API is available at <http://localhost:8080> and Swagger UI at <http://localhost:8080/swagger-ui.html>. Set `APP_PORT` to publish it on another host port. Stop it with:

```sh
docker compose down
```

Add `--volumes` only when you intentionally want to delete the local database.

## Run from Gradle

Start PostgreSQL, then run the application with the same password:

```sh
docker compose up -d db
DB_PASSWORD="$(cat secrets/db_password)" ./gradlew bootRun
```

Database connection settings can be overridden with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_POOL_MAX_SIZE`, and `DB_POOL_MIN_IDLE`. Ports are configured with `SERVER_PORT` (default `8080`) and `MANAGEMENT_PORT` (default `8081`). Set `API_DOCS_ENABLED=false` to disable the OpenAPI endpoints.

## Secrets

No credentials are stored in the repository, and the application has no default password: it refuses to start when `DB_PASSWORD` is missing.

The password can be provided in either of two ways:

- **Mounted file (preferred):** the application imports every file in `/run/secrets/` (override with `SECRETS_DIR`) as a property named after the file. A file named `DB_PASSWORD` sets the password. Docker Compose, Docker Swarm, and Kubernetes secret volumes all use this mechanism, so the value never appears in the environment or in `docker inspect`.
- **Environment variable:** `DB_PASSWORD`, for platforms that inject secrets as variables.

In production, keep the value in a secret manager (for example Vault, AWS Secrets Manager, or Kubernetes Secrets) and let the platform mount it. The CI pipeline runs [Gitleaks](https://github.com/gitleaks/gitleaks) on every push and pull request to block committed secrets.

## API

### Generate a sequence

`GET /api/v1/fizzbuzz`

```sh
curl --fail-with-body \
  'http://localhost:8080/api/v1/fizzbuzz?int1=3&int2=5&limit=16&str1=fizz&str2=buzz'
```

Response:

```json
["1","2","fizz","4","buzz","fizz","7","8","fizz","buzz","11","fizz","13","14","fizzbuzz","16"]
```

Validation rules:

| Field | Rule |
|---|---|
| `int1`, `int2` | Positive integers |
| `limit` | Integer from 1 to 10,000 |
| `str1`, `str2` | Non-empty strings of at most 100 characters; whitespace is preserved |

All parameters are required. URL-encode special characters (for example, a space as `%20`). The `limit` and string length caps bound the response size (about 2 MB at most). Validation failures use `application/problem+json` and include a field-level `errors` object.

### Most frequent request

`GET /api/v1/statistics`

```sh
curl --fail-with-body http://localhost:8080/api/v1/statistics
```

Example response:

```json
{"int1":3,"int2":5,"limit":16,"str1":"fizz","str2":"buzz","hits":4}
```

The endpoint returns `204 No Content` until the first generation request is recorded. Ties are resolved in favor of the parameter set recorded first. Counts are updated atomically and persist across restarts.

### API documentation

| Endpoint | Purpose |
|---|---|
| `/swagger-ui.html` | Interactive Swagger UI |
| `/v3/api-docs` | OpenAPI 3 specification (JSON) |

## Operations

Actuator endpoints are served on the management port (`8081` by default), not on the public API port. The Compose file does not publish this port; use it for container health checks and from your internal network only.

| Endpoint | Purpose |
|---|---|
| `/actuator/health` | Aggregate health |
| `/actuator/health/liveness` | Container liveness |
| `/actuator/health/readiness` | Traffic readiness |
| `/actuator/info` | Application metadata |
| `/actuator/prometheus` | Prometheus metrics |

Only these Actuator endpoints are exposed. Health responses do not reveal component details.

### Logging

The container image sets `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs`, so logs are written to standard output as one JSON object per line in the [Elastic Common Schema](https://www.elastic.co/guide/en/ecs/current/index.html), ready for any log collector. `./gradlew bootRun` keeps human-readable logs; set the same variable to get JSON locally.

## Production considerations

These are deliberate trade-offs for the scope of this exercise; each lists what to add before exposing the service publicly.

- **Authentication:** the API is public, as the specification does not mention users. Put it behind an API gateway or add OAuth2 resource server support (`spring-boot-starter-oauth2-resource-server`) when access must be restricted.
- **Rate limiting:** not implemented in the application. It belongs in the API gateway, ingress, or load balancer, where limits are shared across replicas.
- **Statistics growth:** each distinct parameter set is one row, so callers can create many rows by varying parameters. Rate limiting bounds this in practice; for stronger guarantees, add a retention job that deletes rarely used rows or move counting to a bounded store.
- **Statistics on the request path:** the hit is recorded in the same request as the generation. If PostgreSQL is down, `/api/v1/fizzbuzz` returns `503` instead of an unrecorded result, which keeps the statistics accurate. Recording asynchronously (for example through a message queue) would favour availability over accuracy.
- **Response size:** `limit` is capped at 10,000 and strings at 100 characters, so a response is at most about 2 MB.
- **Infrastructure:** terminate TLS at the ingress or load balancer, keep the management port private, and configure CPU, memory, and replica counts in the deployment platform.

## Supply chain security

- Dependency versions are locked (`gradle.lockfile`) and verified against SHA-256 checksums (`gradle/verification-metadata.xml`).
- Docker base images are pinned by digest, and GitHub Actions are pinned by commit SHA.
- Dependabot opens weekly update pull requests for Gradle dependencies, GitHub Actions, the Dockerfile, and the Compose file.
- CI builds the Docker image and scans it with [Trivy](https://trivy.dev/); the build fails on fixable `HIGH` or `CRITICAL` vulnerabilities.
- `build.gradle` overrides a few transitive versions (Tomcat, Jackson) to fix known CVEs until the next Spring Boot patch release. Remove each override once the Spring Boot BOM includes an equal or newer version.

After changing dependencies, refresh the lock and checksum files:

```sh
./gradlew dependencies --write-locks --write-verification-metadata sha256
```

## Continuous integration

The GitHub Actions workflow (`.github/workflows/ci.yml`) runs three jobs on every push to `main` and every pull request:

| Job | What it does |
|---|---|
| `secret-scan` | Scans the full Git history for committed secrets with Gitleaks |
| `build` | Runs `./gradlew check bootJar` (tests, Checkstyle, coverage, ArchUnit) |
| `container` | Builds the Docker image and scans it with Trivy |

## Test and verify

Docker must be running because integration tests use a real PostgreSQL 18 container:

```sh
./gradlew check
```

On Rancher Desktop:

```sh
export DOCKER_HOST="unix://${HOME}/.rd/docker.sock"
```

If Ryuk cannot start in a particular Rancher Desktop configuration, set `TESTCONTAINERS_RYUK_DISABLED=true`. Testcontainers still stops the PostgreSQL container after a normal test run, but interrupted runs may require manual cleanup.

The `check` lifecycle runs compilation with all Java lint warnings treated as errors, Checkstyle, unit and integration tests, JaCoCo reporting, an 85% coverage threshold, and ArchUnit architecture rules.

Build the executable artifact with:

```sh
./gradlew bootJar
```
