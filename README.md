# AI Interview Assistant

AI Interview Assistant is a single Git repository for an AI interview-training platform. The existing Spring Boot backend supports authenticated users, AI-generated questions, answer scoring, mistake review, study progress, per-user AI model selection, and true upstream SSE scoring output. The Vue frontend will be developed as an independent application in this repository.

## Current Status

- Core backend flows are available locally.
- The default model is DeepSeek `deepseek-v4-flash`.
- Change2Pro GPT-5.6 Luna is an optional model that users can select without restarting the backend.
- Question generation, normal scoring, SSE scoring, and answer-record persistence have been verified locally.
- Swagger/OpenAPI now documents authentication, model selection, interview training, legacy compatibility routes, JWT Bearer security, response schemas, and SSE behavior.
- Local Swagger UI is available at `http://localhost:8082/swagger-ui.html`.
- The independent Vue 3 frontend is implemented in `frontend/`; browser visual checks passed with mocked API responses, and the user has confirmed live local frontend-backend integration through Vite on `5173` and Spring Boot on `8082`.

## Technology Stack

- Java 17
- Spring Boot 2.7.18
- Maven
- MySQL 8
- Flyway
- MyBatis-Plus
- Redis
- Spring Security and JWT
- DeepSeek, Change2Pro, and DashScope AI clients
- Vue 3, Vite, TypeScript, Pinia, Axios, Element Plus, ECharts

## Project Layout

```text
backend/                         Spring Boot backend; open this folder in IntelliJ IDEA
  .mvn/                          Maven Wrapper files
  database/local/                Local-only destructive reset scripts
  pom.xml
  mvnw.cmd
  src/main/java/                 Application source
  src/main/resources/db/migration/ Flyway migrations
  src/main/resources/static/     Legacy static pages, not the production frontend
  src/test/                      Unit, MVC, security, client, service, and SSE tests

frontend/                        Vue 3 application; open this folder in VS Code
  src/                           API modules, stores, components, views, utilities, and styles
  AGENTS.md                      Frontend-specific collaboration and delivery rules
  README.md                      Frontend local development guide
docs/                            Cross-project architecture and integration notes
AGENTS.md                        Collaboration rules and current project status
README.md                        Repository entry point
```

## Prerequisites

- JDK 17
- MySQL 8 running locally or in a reachable development environment
- Redis running locally or in a reachable development environment
- A valid DeepSeek API key for the default model
- Maven Wrapper is included; a separate Maven installation is optional
- Node.js 22 and pnpm for the independent frontend

## Configuration

All secrets are external configuration. Do not commit passwords, JWT secrets, API keys, `.env` files, or local property files.

| Variable | Purpose | Required locally |
| --- | --- | --- |
| `MYSQL_URL` | JDBC connection URL | Defaults to the local `interview_db` URL |
| `MYSQL_USERNAME` | MySQL username | Defaults to `root` locally |
| `MYSQL_PASSWORD` | MySQL password | Yes |
| `REDIS_HOST` | Redis host | Defaults to `localhost` |
| `REDIS_PORT` | Redis port | Defaults to `6379` |
| `REDIS_PASSWORD` | Redis password | Optional |
| `JWT_SECRET` | HS256 signing secret | Yes |
| `DEEPSEEK_API_KEY` | DeepSeek credential | Required to use the default model |
| `DEEPSEEK_ENDPOINT` | DeepSeek Chat Completions URL | Defaults to the official endpoint |
| `CHANGE2PRO_API_KEY` | Change2Pro credential | Required only for the optional model |
| `CHANGE2PRO_ENDPOINT` | Full Change2Pro Responses API URL | Required only for the optional model |
| `CHANGE2PRO_REASONING_EFFORT` | Change2Pro reasoning level | Defaults to `low` |
| `CHANGE2PRO_DISABLE_RESPONSE_STORAGE` | Disable relay response storage | Defaults to `true` |
| `FLYWAY_ENABLED` | Enable Flyway migrations at startup | Defaults to `false` |
| `OPENAPI_ENABLED` | Enable the OpenAPI JSON document | Defaults to `true` locally and `false` in the production profile |
| `SWAGGER_UI_ENABLED` | Enable Swagger UI | Defaults to `true` locally and `false` in the production profile |

The application no longer uses `AI_PROVIDER`, `DEEPSEEK_MODEL`, or `CHANGE2PRO_MODEL`. Provider connection settings come from environment variables; selectable provider/model pairs come from the `ai_model` database allowlist and the user's saved preference.

## Local Startup

Set the required variables in the IntelliJ IDEA run configuration or PowerShell. Use placeholders only; never paste real secrets into source files.

```powershell
$env:MYSQL_PASSWORD = "<local-mysql-password>"
$env:JWT_SECRET = "<jwt-secret-with-at-least-32-bytes>"
$env:DEEPSEEK_API_KEY = "<deepseek-api-key>"
$env:FLYWAY_ENABLED = "true"

Set-Location backend
.\mvnw.cmd spring-boot:run
```

The local server listens on `http://localhost:8082` by default.

Use `FLYWAY_ENABLED=true` only when the target database is intentionally prepared for the versioned migrations. Do not modify already-applied migration files or manually alter production schema in Navicat.

## Frontend Startup

With the backend running on port `8082`, start the Vue application in another terminal:

```powershell
Set-Location frontend
pnpm install
pnpm dev
```

Open `http://127.0.0.1:5173`. Vite proxies `/api` to `http://localhost:8082` by default. Set `VITE_BACKEND_TARGET` before starting Vite only when the backend uses another address.

## Tests

Run the full test suite without applying Flyway migrations:

```powershell
Set-Location backend
.\mvnw.cmd -Dspring.flyway.enabled=false test
```

Run frontend quality checks:

```powershell
Set-Location frontend
pnpm format:check
pnpm lint
pnpm test:run
pnpm build
```

## OpenAPI and Swagger

With the local application running, open:

- Swagger UI: `http://localhost:8082/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8082/v3/api-docs`

Swagger UI and the OpenAPI document are public documentation endpoints. Business APIs remain protected. Use the login endpoint first, then click `Authorize` in Swagger UI and enter the JWT value; Swagger adds the `Bearer` prefix for the protected JSON APIs.

The production profile disables both documentation endpoints by default. Enable them only in an intentionally protected environment by setting both `OPENAPI_ENABLED=true` and `SWAGGER_UI_ENABLED=true`.

The JSON interview APIs are the formal frontend contract. The old `GET /api/question/ask`, `GET /api/question/score`, and `GET /api/question/score/stream` endpoints are marked deprecated in Swagger and remain only for the legacy static pages.

## Main API Areas

All business APIs use the response shape below:

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

- `POST /api/auth/register` and `POST /api/auth/login`
- `GET /api/ai/models`
- `GET` and `PUT /api/users/me/ai-preference`
- `POST /api/question/ask`
- `POST /api/question/score`
- `GET /api/question/score/stream`
- `GET /api/mistakes`
- `GET /api/progress`

Most APIs require `Authorization: Bearer <token>`. The legacy SSE endpoint also recognizes a deprecated `token` query parameter only for the old static page. New frontend code must not put JWTs in URLs. Its successful stream contains ordinary `data:` text chunks followed by `event: done` with an `AiScoreResult` JSON payload; failures are delivered as `event: error` text. An unauthenticated stream sends an in-stream login message and completes instead of returning a normal HTTP `401` response.

## Database Migrations

Flyway migrations are the only source of truth for production schema changes:

- `V1__create_core_schema.sql`: core user and answer-record schema
- `V2__add_user_ai_model_selection.sql`: model allowlist, policy, and user preference
- `V3__correct_deepseek_v4_flash_model_code.sql`: corrected the DeepSeek model identifier in place

`backend/database/local/reset_interview_db.sql` is local-only and destructive. Never use it against a cloud or production database.

## Documentation

- [Collaboration and current project status](AGENTS.md)
- [Documentation index](docs/README.md)
- [Backend architecture notes](docs/backend-architecture.md)
- [Frontend architecture notes](docs/frontend-architecture.md)
- [Frontend-backend integration notes](docs/frontend-backend-integration.md)
- [Upgrade plan](docs/project-upgrade-plan.md)

The OpenAPI contract is the source of truth for frontend API integration. The independent Vue frontend has completed local live integration verification; the next planned phase is a separately reviewed retirement of the legacy static pages and their compatibility-only backend routes.

## Development Rules

- Keep secrets out of Git, logs, static pages, and documentation examples.
- Add a new Flyway migration for every schema change; never edit an applied migration.
- Keep controllers, business services, mappers, AI clients, and SSE transport responsibilities separate.
- Add or update focused tests when business behavior changes.
- Use Git commits to keep each coherent change traceable.
