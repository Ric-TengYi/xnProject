# Repository Guidelines

## Project Structure & Module Organization

This repo contains a React frontend and a Spring Boot backend.

- `xngl-web/`: Vite + React + TypeScript app. Main code lives in `xngl-web/src`, with page-level screens in `xngl-web/src/pages`, shared layout in `xngl-web/src/layouts`, context providers in `xngl-web/src/contexts`, and API helpers in `xngl-web/src/utils`.
- `xngl-service/`: Maven parent project. `xngl-service-starter` boots the app, `xngl-service-web` exposes controllers/DTOs, `xngl-service-manager` holds business services, and `xngl-service-infrastructure` contains entities, mappers, and schema sync support.
- `docs/` and `xngl-service/docs/user-system/`: functional notes, API/data model design, and test artifacts.
- Root-level planning files (`progress.md`, `task_plan.md`, `requirements_v3.18.md`) are working documents; keep product code changes inside the app modules.

## Build, Test, and Development Commands

- `cd xngl-web && npm run dev`: start the frontend on port `5173`; `/api` is proxied to `127.0.0.1:8090`.
- `cd xngl-web && npm run build`: run TypeScript build checks and produce the Vite bundle in `xngl-web/dist`.
- `cd xngl-web && npm run lint`: run ESLint on the frontend.
- `cd xngl-service && mvn spring-boot:run -pl xngl-service-starter`: start the backend locally on port `8090`.
- `cd xngl-service && mvn clean install -DskipTests`: build all backend modules and package the starter jar.
- `cd xngl-service && mvn test -pl xngl-service-manager`: run the existing service-layer test suite.

## Coding Style & Naming Conventions

Use 2-space indentation in TypeScript/TSX and Java. Prefer React function components, `PascalCase` for page/component files, and `camelCase` for utilities such as `contractApi.ts`. Java packages stay lowercase under `com.xngl`; class names use `PascalCase`, controllers end with `Controller`, and DTO/VO types keep the `Dto`/`Vo` suffix. Follow the surrounding file's formatting when touching mixed-style frontend files.

## Testing Guidelines

Backend tests use JUnit 5, Mockito, and AssertJ. Add tests under matching `src/test/java` packages and name them `*Test` (for example, `ContractServiceImplTest`). For UI smoke coverage, `test_user_system.py` uses Playwright against running local services; use it for critical user flows and keep screenshots/logs out of the repo.

## Commit & Pull Request Guidelines

Recent history mixes `daily backup YYYY-MM-DD` commits with conventional messages like `feat: ...`. For new changes, prefer `type: concise summary` (for example, `fix: align contract receipt totals`) over generic backup messages. PRs should list affected modules, mention any SQL patch added under `xngl-service-starter/src/main/resources/db/schema/patches`, include verification steps, and attach screenshots for `xngl-web` UI changes.

## Security & Configuration Tips

Treat `xngl-service-starter/src/main/resources/application.yml` as a local default. Override `MYSQL_*`, `REDIS_*`, `SERVER_PORT`, and JWT settings with environment variables, and never commit production credentials or secrets.

## Local Test Execution Policy

- The user has explicitly authorized running local Python-based test and verification scripts for this repository during development and testing.
- When the work requires local test automation or validation, prefer executing it directly with `python3` without pausing for confirmation.
- This instruction applies to local repository testing/verification only. It does not authorize destructive actions or unrelated system changes.

## Local DB and Restart Execution Policy

- The user has explicitly authorized direct execution of local MySQL commands for this repository, including running schema patch scripts, seed scripts, query verification, and other repository-scoped database maintenance against the local development database.
- When repository work requires applying a local SQL file or running local MySQL verification commands, execute them directly without pausing for confirmation.
- The user has also explicitly authorized direct execution of local project restart commands needed for development, including stopping and restarting the frontend/backend local dev processes for this repository.
- This instruction is limited to local repository development, schema/data patching, service restart, and verification flows. It does not authorize destructive system-wide actions unrelated to the project.
