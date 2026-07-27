# AI Interview Assistant

AI Interview Assistant is a Spring Boot backend for Java interview practice. It supports authenticated users, AI-generated questions, answer scoring, mistake review, study progress, per-user AI model selection, and true upstream SSE scoring output.

## Current Status

- Core backend flows are available locally.
- The default model is DeepSeek `deepseek-v4-flash`.
- Change2Pro GPT-5.6 Luna is an optional model that users can select without restarting the backend.
- Question generation, normal scoring, SSE scoring, and answer-record persistence have been verified locally.
- Swagger/OpenAPI and the independent Vue frontend have not been added yet.

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

## Project Layout

```text
src/main/java/com/example/aiinterviewassistant/
  client/       AI provider clients and SSE event parsing
  common/       Shared API response model
  config/       Spring, security, AI, HTTP, and SSE configuration
  controller/   HTTP and SSE entry points
  dto/          Request and response DTOs
  entity/       MySQL table mappings
  exception/    Business exceptions and global error handling
  mapper/       MyBatis-Plus data access interfaces
  model/        Internal immutable models
  security/     JWT authentication and security error handling
  service/      Business services and implementations
  sse/          SSE transport adapter
  utils/        Existing JWT and request-user utilities

src/main/resources/
  db/migration/ Flyway migrations
  static/       Legacy static HTML pages; not the future production frontend

src/test/       Unit, MVC, security, client, service, and SSE tests
database/local/ Local-only destructive reset scripts
docs/           Architecture and upgrade notes
```

## Prerequisites

- JDK 17
- MySQL 8 running locally or in a reachable development environment
- Redis running locally or in a reachable development environment
- A valid DeepSeek API key for the default model
- Maven Wrapper is included; a separate Maven installation is optional

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

The application no longer uses `AI_PROVIDER`, `DEEPSEEK_MODEL`, or `CHANGE2PRO_MODEL`. Provider connection settings come from environment variables; selectable provider/model pairs come from the `ai_model` database allowlist and the user's saved preference.

## Local Startup

Set the required variables in the IntelliJ IDEA run configuration or PowerShell. Use placeholders only; never paste real secrets into source files.

```powershell
$env:MYSQL_PASSWORD = "<local-mysql-password>"
$env:JWT_SECRET = "<jwt-secret-with-at-least-32-bytes>"
$env:DEEPSEEK_API_KEY = "<deepseek-api-key>"
$env:FLYWAY_ENABLED = "true"

.\mvnw.cmd spring-boot:run
```

The local server listens on `http://localhost:8082` by default.

Use `FLYWAY_ENABLED=true` only when the target database is intentionally prepared for the versioned migrations. Do not modify already-applied migration files or manually alter production schema in Navicat.

## Tests

Run the full test suite without applying Flyway migrations:

```powershell
.\mvnw.cmd -Dspring.flyway.enabled=false test
```

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

Most APIs require `Authorization: Bearer <token>`. The current legacy SSE endpoint accepts its JWT through a `token` query parameter for static-page compatibility; this is not the intended production authentication design for the future frontend.

## Database Migrations

Flyway migrations are the only source of truth for production schema changes:

- `V1__create_core_schema.sql`: core user and answer-record schema
- `V2__add_user_ai_model_selection.sql`: model allowlist, policy, and user preference
- `V3__correct_deepseek_v4_flash_model_code.sql`: corrected the DeepSeek model identifier in place

`database/local/reset_interview_db.sql` is local-only and destructive. Never use it against a cloud or production database.

## Documentation

- [Collaboration and current project status](AGENTS.md)
- [Documentation index](docs/README.md)
- [Backend architecture notes](docs/backend-architecture.md)
- [Frontend architecture notes](docs/frontend-architecture.md)
- [Frontend-backend integration notes](docs/frontend-backend-integration.md)
- [Upgrade plan](docs/project-upgrade-plan.md)

The architecture notes are being refreshed as the API contract is finalized. Swagger/OpenAPI is the next planned backend documentation step.

## Development Rules

- Keep secrets out of Git, logs, static pages, and documentation examples.
- Add a new Flyway migration for every schema change; never edit an applied migration.
- Keep controllers, business services, mappers, AI clients, and SSE transport responsibilities separate.
- Add or update focused tests when business behavior changes.
- Use Git commits to keep each coherent change traceable.
