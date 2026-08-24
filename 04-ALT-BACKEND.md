# Vinilogs — Alternative Backend (Contingency Spec)

**Status:** Not selected. Firebase is the chosen backend for v1.0 (see ADR-1 in
`02-ARCHITECTURE.md`).

This document exists so the option is costed and specified rather than reinvented under
pressure. Do **not** implement it unless a trigger below fires.

## 1. When to revisit

Migrate only if one of these becomes true:

| Trigger | Threshold |
|---|---|
| Firestore bill outgrows a server | Sustained > ~20–50k daily active users, or a monthly bill above ~$150 |
| Search is the top user complaint | Prefix-only **user** search is rejected in feedback (own-collection search runs in Room and is unaffected) |
| Query shapes Firestore can't serve | Joins, aggregations, or full-text ranking across collections |
| The projection pattern becomes unmanageable | `publicRecords` fan-out costs or bugs outweigh the ops burden of a server |
| Vendor or compliance requirement | Data residency, or a client contract forbidding Google |

If only search hurts, the cheaper fix is bolting Typesense or Algolia onto Firestore — not
replacing the whole backend.

## 2. Stack

| Layer | Choice | Why |
|---|---|---|
| Language | Kotlin + Ktor | Same language as the app; domain models shared verbatim |
| Database | PostgreSQL 16 | Relational fits the data; `pg_trgm` and `tsvector` solve search outright, and column-level privacy replaces the projection hack |
| Migrations | Flyway | Versioned, reviewable SQL |
| Auth | JWT access (15 min) + rotating refresh token (30 days) | Stateless reads, revocable sessions |
| Password hashing | Argon2id | Current standard |
| File storage | Cloudflare R2 (S3 API), presigned uploads | Free egress |
| Email | Resend or Postmark | Verification and password reset |
| Deploy | Docker Compose on a single VPS, Caddy for TLS | One box, one command |
| Monitoring | Prometheus + Grafana Cloud free tier; Sentry | Alerts before users notice |

No WebSockets, no message broker, no push infrastructure. The app has no realtime
requirement — a REST API with delta sync covers everything in v1.0.

## 3. Postgres schema

```sql
users (
  id            uuid primary key,
  email         citext unique not null,
  password_hash text not null,
  display_name  text not null,
  avatar_url    text,
  bio           text,
  location      text,
  is_public     boolean not null default true,
  created_at    timestamptz not null default now(),
  deleted_at    timestamptz
);

refresh_tokens (
  id uuid primary key, user_id uuid references users on delete cascade,
  token_hash text not null, expires_at timestamptz not null,
  revoked_at timestamptz, device_info text
);

records (
  id uuid primary key,
  user_id uuid references users on delete cascade,
  artist text not null, title text not null,
  year int, label text, catalog_number text,
  format record_format not null, speed record_speed,
  condition record_condition,
  purchase_price numeric(10,2),      -- private
  purchase_date date,                -- private
  rating smallint check (rating between 1 and 5),  -- private
  notes text,                        -- private
  cover_url text, discogs_id bigint,
  tags text[] not null default '{}',
  search_vector tsvector generated always as (
    to_tsvector('simple', artist || ' ' || title || ' ' || coalesce(label,''))
  ) stored,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz             -- soft delete, for delta sync
);
create index on records using gin (search_vector);
create index on records using gin (artist gin_trgm_ops);   -- fuzzy match
create index on records (user_id, updated_at desc);

reports (id uuid primary key, reporter_id uuid, reported_id uuid, reason text,
         created_at timestamptz);
```

Privacy is a `SELECT` column list, not a duplicated projection collection. This alone removes
`onRecordWritten`, `onProfileUpdated` and ADR-4 from the design.

## 4. REST API

All routes under `/v1`. Bearer JWT except where noted. Errors use RFC 9457 problem+json.

```
POST   /auth/register            (public)  → {accessToken, refreshToken, user}
POST   /auth/login               (public)
POST   /auth/refresh             (public)
POST   /auth/logout
POST   /auth/password/forgot     (public)  → always 202
POST   /auth/password/reset      (public)
DELETE /auth/account                       → soft delete + purge job

GET    /me
PATCH  /me                                 → displayName, bio, location, isPublic
POST   /me/avatar                          → presigned R2 upload URL

GET    /records?q&format&condition&decade&rating&tag&sort&cursor
GET    /records/sync?since                 → delta sync, includes tombstones
POST   /records
GET    /records/{id}
PATCH  /records/{id}
DELETE /records/{id}
POST   /records/{id}/cover                 → presigned R2 upload URL
GET    /records/stats
GET    /records/export.csv
GET    /catalog/search?q&page              → Discogs proxy, server-side cached

GET    /users?q&cursor                     → public profiles only
GET    /users/{id}
GET    /users/{id}/records?cursor          → public columns only, enforced in SQL
GET    /users/{id}/shared?cursor           → records in common, computed server-side
POST   /reports
```

Pagination is cursor-based (opaque base64 of `sort_key,id`) everywhere — no offsets.

## 5. Client-side impact

Only `core:data` changes. The three repository interfaces in `02-ARCHITECTURE.md` §4 stay
byte-identical — that is the whole point of the boundary.

| Replace | With |
|---|---|
| Firebase Auth SDK | Ktor/Retrofit auth client + `Authenticator` that refreshes on 401 |
| Firestore listeners (collection) | REST + Room, with `updated_at` delta sync via `/records/sync` |
| Firestore reads (discovery) | REST, session-cached, unchanged shape |
| Firestore offline persistence | Room is already the source of truth — no change |
| Security rules | Server-side authorization |
| `publicRecords` projection | Deleted — replaced by column selection in SQL |

Feature modules, ViewModels and Compose screens require **zero** changes.

## 6. Task list (only if triggered)

| ID | Task | Notes |
|---|---|---|
| B-01 | Ktor skeleton, Docker Compose, Flyway, CI | |
| B-02 | Schema + migrations | §3 |
| B-03 | Auth: register, login, refresh rotation, Argon2id, reset emails | Highest-risk task — requires human security review |
| B-04 | Records CRUD + filtering, sorting, cursor pagination, delta sync | |
| B-05 | Full-text and trigram search | The reason to migrate at all |
| B-06 | Discogs proxy with server-side cache | Removes the API key from the client |
| B-07 | R2 presigned uploads + image resize worker | |
| B-08 | User search and public profiles, privacy enforced in SQL | |
| B-09 | Shared-records endpoint | |
| B-10 | Reports and moderation queries | |
| B-11 | Rate limiting, request logging, Sentry, Prometheus metrics | |
| B-12 | Nightly backups to R2 + a tested restore runbook | Untested backups are not backups |
| B-13 | Account deletion purge job | |
| C-01 | Client: swap `core:data` implementations behind the same interfaces | |
| C-02 | Dual-write migration + one-off Firestore→Postgres export script | §7 |

Add ~3–4 weeks of calendar time. B-03 and B-12 need human sign-off regardless of how well the
agents perform.

## 7. Migration path (if it ever happens)

1. Stand up the new backend and run it empty in parallel.
2. Export Firestore to Postgres with a one-off script; verify row counts and spot-check
   records.
3. Ship a client release that dual-writes to both backends, reads from Firebase.
4. Re-run the export for the delta, then flip reads to Postgres behind a remote-config flag.
5. Hold Firebase read-only for 30 days as a rollback, then decommission.

Users are never asked to re-register: password hashes cannot be exported from Firebase Auth,
so the first login after the flip triggers a silent one-time password reset email, or you
seed accounts via Firebase's `auth:export` with its supported hash parameters.

Note that with no chat, there is no realtime state to migrate — records are the only mutable
data, and they already survive an offline gap by design. The cutover is far less risky than
it would have been.

## 8. Cost comparison

| | Firebase (chosen) | Self-hosted |
|---|---|---|
| Month 0–6, small user base | **$0** | ~$10–70/mo from day one |
| ~5k DAU | ~$8/mo | ~$25–70/mo |
| ~50k DAU | ~$90/mo | ~$80–150/mo + ops time |
| Time to a working app | Days | Weeks |
| Ops burden | None | Patching, backups, on-call, forever |
| Own-collection search | Room SQL — identical either way | Room SQL — identical either way |
| User search quality | Prefix only | Full-text + fuzzy |
| Field-level privacy | Projection collection + Cloud Function | `SELECT` column list |
| Security ownership | Google | You |

Firestore costs dropped against the earlier estimate once chat was removed — messages and
their listeners were the majority of projected reads. The crossover is now well above where
v1.0 will land. Build on Firebase; keep this document current only if a trigger in §1 starts
approaching.
