# Frontend

This directory is reserved for the independent Vue 3 frontend of AI Interview Assistant.

The frontend has not been scaffolded yet. When implementation begins, initialize the Vite project in this directory and keep the existing root Git repository; do not run `git init` here.

Use the OpenAPI document served by the backend as the API contract:

- Swagger UI: `http://localhost:8082/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8082/v3/api-docs`

The Spring Boot backend lives in `../backend`. Run Maven commands from that directory. Keep frontend build output and dependencies out of Git; the root `.gitignore` already excludes `node_modules/` and `dist/`.
