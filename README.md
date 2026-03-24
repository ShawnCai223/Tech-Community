# Tech Community

Tech Community is a Spring Boot 3 + React application for posts, comments, likes, follows, direct messages, notifications, and search under the `/community` context path.

## Stack

- Backend: Spring Boot 3.1, MyBatis, MySQL, Redis, Kafka, Elasticsearch, Quartz, WebSocket, JWT
- Frontend: React 19, TypeScript, Vite
- Runtime: Java 17, Node 20+

## Project Layout

- `src/main/java`: Spring Boot application code
- `src/main/resources`: application config, MyBatis mappers, built frontend assets
- `frontend`: React frontend source
- `scripts`: local and production helper scripts
- `data/community-init-sql-1.5`: database seed data
- `deploy`: deployment-related config such as Nginx snippets

## Quick Start

The application defaults to the `dev` profile.

Start the full local development flow:

```bash
make dev-up
```

Useful local commands:

```bash
make dev-up
make dev-verify
make dev-down
make dev-reset
make dev-seed-likes
```

If you only want to start the backend directly:

```bash
mvn spring-boot:run
```

The app runs at:

```text
http://localhost:8080/community
```

## Frontend

Frontend source lives in `frontend/`.

Useful commands:

```bash
cd frontend
npm run dev
npm run build
npm run lint
```

The built frontend is deployed from:

```text
src/main/resources/static/app
```

## Docs

- Local development: [HELP.md](HELP.md)
- Testing: [TESTING.md](TESTING.md)
- Deployment: [DEPLOY.md](DEPLOY.md)
- Frontend notes: [frontend/README.md](frontend/README.md)
