# Frontend Status (2026-07-28)

- Streaming scoring uses `POST /api/question/score/stream` with JSON and Bearer authentication.
- The frontend no longer sends question or answer content in the URL.
- Unused auth/interview getters and `AsyncStatus` were removed.
- Verification passed: `pnpm lint`, `pnpm test:run` with 6 tests, and `pnpm build`.
- Next: authenticated local verification of streaming success, validation failure, and cancellation.
