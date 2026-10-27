# CampusAtlas — Documents

Working documents for the CampusAtlas project. Nothing here is Java source; the application code lives under [../src/](../src/).

## Contents

| File | Purpose |
|------|---------|
| [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md) | Working plan expanding the submitted PDF into concrete phases, tasks, and a coverage matrix. The primary document for anyone (team, supervisor, future reader) who wants to understand what we are building and in what order. |
| [db_setup.md](./db_setup.md) | How to set up a local Oracle 18c XE instance so you can develop against `localhost` instead of the campus server. One-time steps; teammates should follow this before touching code. |
| [supervisor_email.md](./supervisor_email.md) | Draft of the single email we need to send Marek Rychly covering the three open approval items. |
| `CampusAtlas_Project_Plan.pdf` | The project plan PDF submitted on 2026-10-21. **Still to be added to the repo** — drop the file into this folder with that exact filename. |
| `reviews/` | Weekly team-review notes, created from Phase 1 onward. One file per review, e.g. `reviews/2026-11-08.md`. |

## Conventions

- Everything in `docs/` is Markdown or PDF, no executables.
- Dates are ISO (`YYYY-MM-DD`) to match the project plan.
- Credentials, personal data and campus internal network details stay out of these documents — reference them by placeholder (`<PASSWORD>`, `<CAMPUS_USER>`) and keep the real values in local notes.
