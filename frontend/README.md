# Frontend

This directory contains the React frontend for Tech Community.

## Stack

- React 19
- TypeScript
- Vite
- React Router
- Axios
- React Markdown + Remark GFM + Rehype Highlight

## Commands

Install dependencies:

```bash
npm ci
```

Start local frontend development:

```bash
npm run dev
```

Build production assets:

```bash
npm run build
```

Lint:

```bash
npm run lint
```

Preview the production build:

```bash
npm run preview
```

## Build Output

Vite builds into:

```text
frontend/dist
```

The backend serves the deployed frontend from:

```text
src/main/resources/static/app
```

After rebuilding the frontend, sync `frontend/dist` into `src/main/resources/static/app`.

## Base Path

The frontend is built with this base path:

```text
/community/app/
```

That must stay aligned with the backend context path and Nginx routing.
