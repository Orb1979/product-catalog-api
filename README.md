# Product catalog API

A product catalog assignment built incrementally as two Spring Boot applications.

## Requirements
- JDK 21
- Docker with a running daemon (for PostgreSQL and Testcontainers tests)

## Modules

| Module | Purpose | Default port |
| --- | --- | --- |
| `catalog-api` | Product catalog REST API | 8080 |
| `mock-pricing-system` | Mock external pricing system | 8081 |


## Build and test

```bash
./gradlew clean build
```

Run a single module's tests:

```bash
./gradlew :catalog-api:test
./gradlew :mock-pricing-system:test
```

## Run locally

Start PostgreSQL and wait until it is healthy:

```bash
# start containers
docker compose up -d --wait postgres
```

stop containers or processes on ports 8080 and 8081 (if necessary):
```
# stop containers
docker-compose down

# stop containers and remove volumes
docker-compose down -v

# kill any processes on ports 8080 and 8081
lsof -ti :8080,8081 | xargs kill -9
```

In separate terminals:

```bash
./gradlew :mock-pricing-system:bootRun
```

```bash
./gradlew :catalog-api:bootRun
```

The catalog applies Flyway migrations on startup and validates its JPA mapping against the schema. 
The database includes 10,000 demo products.
The pricing synchronization cursor starts at the Unix epoch.

## API

```bash
curl 'http://localhost:8080/api/products?page=0&size=20'
curl 'http://localhost:8080/api/products?q=blue%20jacket&page=0&size=20'
curl http://localhost:8080/api/products/SKU-000001
```

Pages are zero-based and limited to 100 products (default 20).

Without `q`, or with blank input, products are sorted by SKU. Searches are ranked by relevance, then SKU. 
Search covers name, brand, category, description, and JSON attribute keys/values, not SKU, price, or currency.

#### PostgreSQL’s full-text search functions
Search is case-insensitive and token-based (not substring or typo matching). Words are combined with AND.\
use `"city lamp"` for a phrase \
use `lamp OR jacket` for alternatives \
use `jacket -blue`to exclude a term


Missing products return 404; invalid pagination returns 400, both as ProblemDetail. \
Swagger UI: http://localhost:8080/swagger-ui/index.html

To check search response time locally:

```bash
curl -s -o /dev/null -w '%{time_total}s\n' 'http://localhost:8080/api/products?q=blue%20jacket'
```

### Database configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/catalog` | Catalog JDBC URL |
| `DB_PORT` | `5432` | Compose host port and default catalog JDBC port |
| `DB_USER` | `catalog` | Catalog and Compose database user |
| `DB_PASSWORD` | `catalog` | Catalog and Compose database password |

The defaults are local demo credentials only, not production secrets.
PostgreSQL is published on loopback (`127.0.0.1:5432`). 
Set the same environment variables if you need to override credentials
(PostgreSQL initialization variables only take effect when the data volume is first created)

## Mock pricing system

Run it locally with `:mock-pricing-system:bootRun` (above), or in Docker:

```bash
docker compose up -d --build mock-pricing
curl http://localhost:8081/api/prices
curl 'http://localhost:8081/api/prices?since=2026-01-01T12:00:00Z'
```

Do not run the local and containerized mock together on port 8081. \
If that port is occupied, use `MOCK_PRICING_PORT=18081 docker compose up -d mock-pricing`
and access the containerized mock on port 18081 instead.

The mock seeds 10,000 EUR prices and increases the first 10 SKUs by €0.10 every
five seconds. (`PRICING_CHANGE_INTERVAL`) 
Its in-memory prices reset on restart.

Responses contain `watermark` and `changes` (`sku`, `price`, `currency`, `updatedAt`). \
Omitting `since` loads all prices. \
For subsequent requests, use the previous response's **watermark**, not the client clock. 

Timestamps use microsecond precision to match PostgreSQL. \
Invalid timestamps or cursors ahead of the source watermark return 400.


### Authentication

Applications are currently unauthenticated. For this local assignment demo,
bind them to loopback when running outside an isolated development environment:

```bash
SERVER_ADDRESS=127.0.0.1 ./gradlew :catalog-api:bootRun
```
