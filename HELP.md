# Local Runbook

This document covers local development, local dependency setup, and Elasticsearch-specific workflows.

## Default Local Development

The application defaults to the `dev` profile through `application.properties`.

Start the backend directly:

```bash
mvn spring-boot:run
```

Or use the provided shortcuts:

```bash
make dev-up
make dev-verify
make dev-down
make dev-reset
make dev-seed-likes
```

These are backed by:

- `scripts/dev-up.sh`: start local dependencies, wait for health, and run the app with `dev,es-local`
- `scripts/dev-verify.sh`: verify the local Elasticsearch flow
- `scripts/dev-down.sh`: stop the local dependency stack
- `scripts/dev-reset.sh`: remove local dependency volumes for a clean re-import
- `scripts/dev-seed-likes.sh`: seed deterministic like counts into Redis

Application URL:

```text
http://localhost:8080/community
```

## Local Dependency Stack

The repeatable local stack is defined in `docker-compose.es-local.yml`.

It starts:

- MySQL `8.0` on `localhost:3306`
- Redis `7` on `localhost:6379`
- Elasticsearch `8.13.4` with `analysis-ik` on `localhost:9200`

Start dependencies only:

```bash
docker compose -f docker-compose.es-local.yml up -d --build
```

Stop them:

```bash
docker compose -f docker-compose.es-local.yml down
```

One-command startup:

```bash
./scripts/dev-up.sh
```

The startup flow also seeds Redis like counts so fresh local pages do not all show `0` likes.

## Local Reset

If you want a clean local reset that re-imports database seed data on the next startup:

```bash
./scripts/dev-reset.sh
```

Or:

```bash
make dev-reset
```

Notes:

- On first MySQL startup, the container imports `init_schema.sql`, `init_data.sql`, and `03-localize_english.sql`
- If your machine already has MySQL, Redis, or Elasticsearch on `3306`, `6379`, or `9200`, stop them first or change the published ports
- Docker volumes persist data, so simple restarts do not trigger a re-import

## Elasticsearch Workflow

An `es-local` profile is available for local search debugging.

Start the app with Elasticsearch enabled:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev,es-local
```

Expected local services:

- MySQL on `localhost:3306`
- Redis on `localhost:6379`
- Elasticsearch on `localhost:9200`

After startup, create the index and sync posts.

These admin endpoints require an authenticated admin `ticket` cookie. Without it, the server returns JSON `403`.

Example:

```bash
export ADMIN_TICKET=your_admin_ticket_here
```

Create a standard analyzer index:

```bash
curl -i --cookie "ticket=${ADMIN_TICKET}" \
  -X POST "http://localhost:8080/community/admin/elasticsearch/index?analyzer=standard"
```

Sync posts:

```bash
curl -i --cookie "ticket=${ADMIN_TICKET}" \
  -X POST http://localhost:8080/community/admin/elasticsearch/sync
```

## IK Analyzer Workflow

To test better Chinese tokenization, recreate the index with IK:

```bash
curl -i --cookie "ticket=${ADMIN_TICKET}" \
  -X POST "http://localhost:8080/community/admin/elasticsearch/index?analyzer=ik"
```

Then sync again:

```bash
curl -i --cookie "ticket=${ADMIN_TICKET}" \
  -X POST http://localhost:8080/community/admin/elasticsearch/sync
```

Verify the plugin directly:

```bash
curl -i -sS -H 'Content-Type: application/json' \
  -X POST http://localhost:9200/_analyze \
  -d '{"analyzer":"ik_smart","text":"互联网求职"}'
```

Expected tokens include values such as `互联网` and `求职`.

Recommended manual checks:

1. Open `http://localhost:8080/community/search?keyword=互联网`
2. Confirm partial Chinese matches are returned
3. Confirm highlights are wrapped in `<em>...</em>`
4. Create a post containing `互联网` and verify it becomes searchable
5. Delete a post and verify it disappears from results after the delete event is handled

## Elasticsearch Test Command

Run the Elasticsearch integration suite with optional IK coverage:

```bash
mvn -q -Dcommunity.es.integration=true -Dtest=ElasticsearchIntegrationTests test
mvn -q -Dcommunity.es.integration=true -Dcommunity.es.ik.integration=true -Dtest=ElasticsearchIntegrationTests test
```
