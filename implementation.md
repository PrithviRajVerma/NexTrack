# NexTrack — Phased Engineering Implementation Guide & Master Tracker

> **A Step-by-Step Production Roadmap for the NexTrack Modular Monolith**  
> *Track progress across development, testing, schema migrations, and performance benchmarking.*

---

## 📊 Master Implementation Dashboard

| Phase | Phase Name | Primary Technical Scope | Target Deliverable | Status |
|:---:|---|---|---|:---:|
| **0** | **Baseline & Design Freeze** | UI prototype SPA, architecture spec, ADR documentation | Unified UI & design freeze | `COMPLETED` ✅ |
| **1** | **Backend & Frontend Scaffolding** | Spring Boot 3, Java 21, Vite/React, Docker Compose, Flyway | Runnable skeleton & CI | `COMPLETED` ✅ |
| **2** | **Identity, Workspace & Project** | Multi-tenant RBAC, RS256 JWT, Refresh Token Rotation | Auth & Project Management | `COMPLETED` ✅ |
| **3** | **Issue Domain & Atomic Counter** | Atomic monotonic numbering (`NT-101`), Issue CRUD | Atomic Issue Allocation | `READY TO START` ⏳ |
| **4** | **Workflow FSM & Concurrency** | Fixed status FSM, JPA `@Version`, 409 Conflict | Concurrency & Transitions | `NOT STARTED` ⏹️ |
| **5** | **Interactive Kanban Board UI** | Board projections (`v_board_cards`), Drag-and-drop | Optimistic UI Board | `NOT STARTED` ⏹️ |
| **6** | **Notifications & Transactional Outbox** | Outbox pattern (`outbox_events`), `SKIP LOCKED` worker | Reliable Async Events | `NOT STARTED` ⏹️ |
| **7** | **Attachments & Search Filtering** | Magic-byte MIME checks, compensating storage | Safe File Pipeline | `NOT STARTED` ⏹️ |
| **8** | **GitHub App & Webhook Inbox** | Raw-byte HMAC filter, 2-phase claim, 5-min lease | Resilient Webhook Queue | `NOT STARTED` ⏹️ |
| **9** | **Issue-to-Code Traceability** | Regex key extraction, M:N links, manual linking | Branches, Commits, PRs | `NOT STARTED` ⏹️ |
| **10** | **CI/CD Builds & Deployments** | Dual-field status mapping, deployment timelines | CI/CD Build Badges | `NOT STARTED` ⏹️ |
| **11** | **Observability & Benchmarks** | k6 load tests (10k+ issues), Prometheus/Grafana | Empirical Verification | `NOT STARTED` ⏹️ |

---

## 🎯 Current Sprint & Active Focus
- **Current Phase:** `Phase 3 — Core Issue Domain & Atomic Sequential Numbering`
- **Immediate Task:** Implement the Issue aggregate root, `V003__create_issues_and_counters.sql`, atomic row-counter sequential numbering (`UPDATE project_issue_counters SET last_issue_number = last_issue_number + 1 RETURNING last_issue_number`), assignee project membership validation guard, and 50-thread concurrent numbering tests.
- **Blockers:** None.

---

## 🛠️ Phase-by-Phase Implementation Specifications

---

### Phase 0: Baseline & Design Freeze
**Goal:** Freeze architecture, establish UI design tokens, and document all technical patterns before writing application code.
- **Status:** `COMPLETED` ✅
- **Dependencies:** None.

#### Detailed Task Checklist
- [x] Complete interactive 8-view Single-Page Prototype (`index.html` / `nextrack_ui_refined.html`).
- [x] Create comprehensive architecture document (`architecture.md`) with 12 ADRs and Mermaid diagrams.
- [x] Establish Git commit policy and safety rules (`AGENTS.md`).
- [x] Conduct architecture gap review and define resume-focused improvements.

#### Deliverables & Artifacts
- `index.html` (Interactive SPA UI Prototype)
- `architecture.md` (System Design & ADRs)
- `README.md` (Project Overview)

---

### Phase 1: Project Scaffolding & Infrastructure Baseline
**Goal:** Establish a runnable local development environment with Spring Boot 3, Java 21, PostgreSQL, Flyway migrations, and React 18 + Vite.
- **Status:** `COMPLETED` ✅
- **Dependencies:** Phase 0 complete.

#### Detailed Task Checklist
- [x] **Docker Environment Setup:**
  - [x] Create `docker-compose.yml` defining PostgreSQL 16+ with health checks and persistent volume.
  - [x] Add `.env.example` with local defaults (database credentials, JWT secrets, encryption master key).
  - [x] Create a root `Makefile` with commands (`make dev`, `make test`, `make db`, `make clean`).
- [x] **Spring Boot 3 Backend Initialization (`backend/`):**
  - [x] Generate Gradle wrapper (`gradlew`) with Java 21 LTS and Spring Boot 3.3.x.
  - [x] Add primary starter dependencies:
    - `spring-boot-starter-web`
    - `spring-boot-starter-data-jpa`
    - `spring-boot-starter-validation`
    - `spring-boot-starter-security`
    - `spring-boot-starter-actuator`
    - `org.flywaydb:flyway-database-postgresql`
    - `org.postgresql:postgresql`
    - `org.springframework.modulith:spring-modulith-starter-core`
    - `org.springframework.modulith:spring-modulith-starter-test` (test scope)
    - `org.testcontainers:postgresql` (test scope)
  - [x] Configure `application.yml` and `application-local.yml` with HikariCP connection pool settings.
  - [x] Configure global RFC 7807 / RFC 9457 Problem Details handling (`GlobalExceptionHandler`).
  - [x] Create baseline Flyway migration `V001__init_schema.sql` (testing Flyway pipeline).
- [x] **React Frontend Initialization (`frontend/`):**
  - [x] Scaffold Vite project with React 18 and TypeScript.
  - [x] Install Tailwind CSS, `@tanstack/react-query`, `lucide-react`, and `react-router-dom`.
  - [x] Port design tokens and typography (Inter + JetBrains Mono) from `index.html`.
  - [x] Configure Vite proxy forwarding `/api` requests to `http://localhost:8080`.
- [x] **CI Pipeline Setup:**
  - [x] Create `.github/workflows/ci.yml` running `mvn verify` and `npm test` on PRs.

#### Definition of Done
- `docker compose up -d` starts PostgreSQL and passes health checks.
- `./gradlew test` runs cleanly and connects to a Testcontainers PostgreSQL instance.
- Frontend builds cleanly via `npm run build`.

---

### Phase 2: Identity, Multi-Tenant Workspace & Project Management
**Goal:** Implement secure authentication, workspace multi-tenancy, and configurable project keys.
- **Status:** `COMPLETED` ✅
- **Dependencies:** Phase 1 complete.

#### Detailed Task Checklist
- [x] **Database Migrations:**
  - [x] Create `V002__create_identity_and_workspaces.sql`:
    - `users` table (`id UUID PK`, `email UK`, `password_hash`, `display_name`, `status`).
    - `refresh_tokens` table (`id UUID PK`, `user_id FK`, `token_hash UK`, `token_family`, `is_revoked`, `expires_at`).
    - `workspaces` table (`id UUID PK`, `name`, `slug UK`, `owner_user_id FK`).
    - `workspace_memberships` table (`workspace_id FK`, `user_id FK`, `role`, `PK(workspace_id, user_id)`).
    - `projects` table (`id UUID PK`, `workspace_id FK`, `name`, `project_key`, `description`, `created_by FK`, `CONSTRAINT uq_projects_workspace_project_key UNIQUE (workspace_id, project_key)`).
    - `project_memberships` table (`project_id FK`, `user_id FK`, `role`, `PK(project_id, user_id)`).
    - `project_issue_counters` table (`project_id PK, FK`, `last_issue_number BIGINT DEFAULT 0`).
- [x] **Security & Token Infrastructure:**
  - [x] Implement RS256 key pair generator / loader (`RsaKeyProvider`).
  - [x] Implement `JwtTokenService` generating 15-minute access tokens with subject and workspace claims.
  - [x] Implement `RefreshTokenService` with family-based theft detection and rotation.
  - [x] Configure Spring Security filter chain: JWT Bearer token authentication, HttpOnly refresh cookie handler.
  - [x] Add Bucket4j rate limiter filter: 5 login attempts per minute per IP.
- [x] **Domain Services & REST APIs:**
  - [x] `AuthController`: `/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/auth/refresh`, `/api/v1/auth/logout`.
  - [x] `WorkspaceService` & `WorkspaceController`: Create workspace, invite members, enforce `WORKSPACE_ADMIN`.
  - [x] `ProjectService` & `ProjectController`:
    - Validate project key format against regex `^[A-Z][A-Z0-9]{1,9}$`.
    - Atomically initialize `project_issue_counters` with `(project_id, 0)` upon project creation.
    - Check membership inheritance (project membership requires workspace membership).
- [x] **Automated Testing:**
  - [x] Testcontainers integration test: Register, login, acquire access token, rotate refresh token.
  - [x] Token theft test: Presenting an already-revoked refresh token revokes the entire token family.
  - [x] Duplicate project key test: Creating two projects with key `NT` in the same workspace returns `409 Conflict`.
  - [x] Spring Modulith architectural boundary tests verifying zero package leakage.

#### Definition of Done
- Users can register, log in, create workspaces, and create projects with validated uppercase keys.
- Refresh token rotation works with HttpOnly cookies and detects theft attempts.

---

### Phase 3: Core Issue Domain & Atomic Sequential Numbering
**Goal:** Implement the issue aggregate root, atomic row-counter sequential numbering (`NT-101`), and assignee eligibility validation.
- **Status:** `READY TO START` ⏳
- **Dependencies:** Phase 2 complete.

#### Detailed Task Checklist
- [ ] **Database Migrations:**
  - [ ] Create `V003__create_issues_and_counters.sql`:
    - `issues` table (`id UUID PK`, `project_id FK`, `issue_number BIGINT`, `title VARCHAR(255)`, `description TEXT`, `issue_type VARCHAR(32)`, `status VARCHAR(32)`, `priority VARCHAR(32)`, `reporter_id FK`, `assignee_id FK NULL`, `due_date TIMESTAMPTZ NULL`, `story_points INT NULL`, `version INT DEFAULT 0`, `created_at`, `updated_at`, `archived_at`).
    - Add constraint: `CONSTRAINT uq_issues_project_issue_number UNIQUE (project_id, issue_number)`.
    - Partial indexes on `(project_id, status, updated_at DESC)` and `(project_id, assignee_id)`.
- [ ] **Atomic Issue Number Counter Implementation:**
  - [ ] Create `ProjectIssueCounterRepository` with atomic row increment query:
    ```sql
    UPDATE project_issue_counters 
    SET last_issue_number = last_issue_number + 1 
    WHERE project_id = :projectId 
    RETURNING last_issue_number;
    ```
  - [ ] Wrap counter allocation and issue creation within the same `@Transactional` method in `IssueService`.
  - [ ] Assert that a rollback restores `last_issue_number` to its prior value.
- [ ] **Assignee Eligibility Guard:**
  - [ ] Before saving an issue, assert that `assignee_id` has an active record in `project_memberships` for that `project_id`.
  - [ ] Reject ineligible assignments with `400 Bad Request` (`ASSIGNEE_NOT_IN_PROJECT`).
- [ ] **Issue REST API (`/api/v1/projects/{projectId}/issues`):**
  - [ ] Create issue (`POST`), Get issue (`GET /{id}`), List issues with bounded pagination (`GET ?page=0&size=25`).
- [ ] **Concurrency & Rollback Verification Tests:**
  - [ ] Testcontainers multi-threaded test: 50 concurrent worker threads creating issues in Project A. Assert strictly monotonic sequential numbering (`1..50`) with 0 duplicate key violations.
  - [ ] Rollback test: Force an exception after counter allocation but before commit. Assert `last_issue_number` rolls back and the subsequent issue receives the restored number.

#### Definition of Done
- 50 concurrent threads generate sequential issue numbers without race conditions or duplicates.
- Assignee validation enforces project membership boundaries.

---

### Phase 4: Issue Workflow State Machine & Concurrency Control
**Goal:** Enforce the immutable 5-state finite state machine (`BACKLOG` -> `TO_DO` -> `IN_PROGRESS` -> `IN_REVIEW` -> `DONE`), JPA `@Version` optimistic locking, and atomic audit logs.
- **Status:** `NOT STARTED` ⏹️
- **Dependencies:** Phase 3 complete.

#### Detailed Task Checklist
- [ ] **Database Migrations:**
  - [ ] Create `V004__create_workflow_and_audit.sql`:
    - `comments` table (`id UUID PK`, `issue_id FK`, `author_id FK`, `body TEXT`, `created_at`, `updated_at`, `deleted_at`).
    - `audit_events` table (`id UUID PK`, `actor_id FK`, `entity_type`, `entity_id UUID`, `action`, `before_payload JSONB`, `after_payload JSONB`, `trace_id`, `created_at`).
- [ ] **Fixed Workflow State Machine:**
  - [ ] Create enum `IssueStatus`: `BACKLOG`, `TO_DO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`, `ARCHIVED`.
  - [ ] Implement `IssueStatusTransitionRule`:
    - Define permitted state transitions.
    - Reject disallowed transitions with `400 Bad Request` (`WORKFLOW_TRANSITION_DENIED`).
  - [ ] Create `POST /api/v1/issues/{id}/transitions` requiring `targetStatus` and expected `version`.
- [ ] **Optimistic Locking Concurrency:**
  - [ ] Add `@Version private Integer version;` to `Issue` entity.
  - [ ] Catch `OptimisticLockException` in `GlobalExceptionHandler` and map to `409 Conflict` (RFC 7807 with error code `ISSUE_VERSION_CONFLICT`).
- [ ] **Atomic Audit Logging:**
  - [ ] Within the status transition transaction, insert an audit row capturing `from_status`, `to_status`, `actor_id`, and timestamp.
- [ ] **Automated Tests:**
  - [ ] Unit test: Verify all valid transitions pass; verify invalid transitions (e.g., `BACKLOG` -> `DONE`) are rejected.
  - [ ] Testcontainers concurrency test: Two concurrent requests submit version 1. Assert exactly one succeeds with 200 OK and one receives 409 Conflict.

#### Definition of Done
- Invalid status transitions are rejected at domain level.
- Version conflicts return RFC 7807 409 Conflict without data corruption.

---

### Phase 5: Interactive Kanban Board UI
**Goal:** Build the React 18 Kanban board with drag-and-drop transitions, TanStack Query state caching, read-optimized board card views, and optimistic UI updates with rollback.
- **Status:** `NOT STARTED` ⏹️
- **Dependencies:** Phase 4 complete.

#### Detailed Task Checklist
- [ ] **Read-Optimized Board Projections:**
  - [ ] Create Flyway migration `V005__create_board_view.sql` creating view `v_board_cards`.
  - [ ] Implement `BoardCardDto` projection query in `IssueRepository` returning minimal card data with lateral comment and dev counts.
  - [ ] Expose `GET /api/v1/projects/{projectId}/board` returning cards grouped by column status.
- [ ] **React Kanban Board Component (`frontend/src/features/board/`):**
  - [ ] Implement 5-column layout: `BACKLOG`, `TO_DO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`.
  - [ ] Integrate HTML5 Drag & Drop or `@hello-pangea/dnd`.
  - [ ] Add Board Toolbar: search filter, assignee filter, and priority filter.
- [ ] **Optimistic UI with Conflict Rollback:**
  - [ ] Configure TanStack Query `useMutation` on card drag:
    - `onMutate`: Snapshot current query cache; optimistically update card to target status.
    - `onError`: Revert card back to snapshotted column; trigger toast: *"Transition failed: Issue modified by another user"*.
    - `onSettled`: Invalidate and refetch board query.
- [ ] **Theme & Design Consistency:**
  - [ ] Implement dual-theme (Dark / Light) with smooth CSS variable transitions (`Cmd+T` shortcut).
  - [ ] Implement keyboard navigation (1-7 view switcher, `C` create issue modal, `[` toggle sidebar).

#### Definition of Done
- Dragging cards moves columns instantaneously.
- Simulating a server 409 or 400 error cleanly reverts the card to its original column with a descriptive toast.

---

### Phase 6: In-App Notifications & Transactional Outbox Pattern
**Goal:** Implement reliable, at-least-once in-app notifications using the Transactional Outbox pattern without external message brokers.
- **Status:** `NOT STARTED` ⏹️
- **Dependencies:** Phase 5 complete.

#### Detailed Task Checklist
- [ ] **Database Migrations:**
  - [ ] Create `V006__create_outbox_and_notifications.sql`:
    - `notifications` table (`id UUID PK`, `recipient_id FK`, `event_type`, `payload JSONB`, `dedup_key UK`, `read_at TIMESTAMPTZ NULL`, `created_at`).
    - `outbox_events` table (`id UUID PK`, `event_type`, `payload JSONB`, `status VARCHAR(16)`, `retry_count INT DEFAULT 0`, `next_retry_at TIMESTAMPTZ NULL`, `created_at`, `published_at TIMESTAMPTZ NULL`).
    - Partial index on `outbox_events (status, next_retry_at) WHERE status = 'PENDING'`.
- [ ] **Transactional Outbox Publisher:**
  - [ ] On issue assignment or mention, insert an `outbox_events` row in the same transaction as the domain change.
  - [ ] Create `OutboxWorker` polling pending events every 500ms using `SELECT ... FOR UPDATE SKIP LOCKED`.
  - [ ] Worker dispatches notification to `notifications` table using `dedup_key` to guarantee idempotent delivery.
  - [ ] Implement exponential backoff with jitter on delivery failure; cap retries at 5 attempts before marking `FAILED`.
- [ ] **Notification Center REST API & UI:**
  - [ ] `GET /api/v1/notifications` (unread list + count).
  - [ ] `PATCH /api/v1/notifications/{id}/read` (mark read).
  - [ ] React Notification Drawer with real-time unread counter badge.

#### Definition of Done
- Outbox events are committed atomically with business changes.
- Worker processes pending outbox rows reliably with zero duplicate user-visible notifications.

---

### Phase 7: Attachments & Rich Search/Filtering
**Goal:** Implement secure file attachment processing with magic-byte MIME validation, opaque storage keys, compensating storage cleanup, and multi-field issue search.
- **Status:** `NOT STARTED` ⏹️
- **Dependencies:** Phase 6 complete.

#### Detailed Task Checklist
- [ ] **Database Migrations:**
  - [ ] Create `V007__create_attachments.sql`:
    - `attachments` table (`id UUID PK`, `issue_id FK`, `uploader_id FK`, `original_filename`, `storage_key UK`, `content_type`, `size_bytes BIGINT`, `checksum`, `created_at`, `deleted_at`).
- [ ] **Secure Attachment Pipeline:**
  - [ ] Implement `StorageAdapter` interface with `LocalStorageAdapter` (storing files outside web root).
  - [ ] Limit upload stream to 10 MB max before memory buffering.
  - [ ] Validate file magic bytes (Apache Tika or explicit signature bytes) against allowed whitelist (JPEG, PNG, GIF, PDF, TXT, ZIP).
  - [ ] Generate opaque server-side storage keys (`UUIDv4`).
  - [ ] Implement compensating cleanup: if database metadata insertion fails, delete the saved file stream immediately.
  - [ ] Expose authorized streaming download endpoint (`GET /api/v1/issues/{id}/attachments/{attachId}`) verifying project read permissions.
- [ ] **Search & Filtering API:**
  - [ ] Implement `IssueSpecification` (Spring Data JPA Specifications) supporting combined filtering: text search on title, status, priority, assignee, reporter, due date range, and labels.

#### Definition of Done
- Uploading spoofed MIME types (e.g. `.exe` renamed to `.png`) is rejected.
- Download verifies project membership; path traversal attacks (`../../`) are impossible.

---

### Phase 8: GitHub App & Webhook Inbox Queue
**Goal:** Implement the primary GitHub App integration, raw-byte HMAC-SHA256 signature verification filter, and PostgreSQL-backed Webhook Inbox queue with 2-phase claim transactions and supervisor recovery.
- **Status:** `NOT STARTED` ⏹️
- **Dependencies:** Phase 2 and 3 complete.

#### Detailed Task Checklist
- [ ] **Database Migrations:**
  - [ ] Create `V008__create_github_integrations_and_inbox.sql`:
    - `integration_installations` table (`id UUID PK`, `workspace_id FK`, `provider VARCHAR(32)`, `installation_id BIGINT`, `account_name`, `encrypted_webhook_secret`, `created_at`, `CONSTRAINT uq_integration_installation UNIQUE (provider, installation_id)`).
    - `connected_repositories` table (`id UUID PK`, `installation_id FK`, `provider`, `external_repo_id`, `name`, `full_name`, `html_url`, `default_branch`, `is_active BOOLEAN`, `last_synced_at`, `CONSTRAINT uq_connected_repo UNIQUE (installation_id, external_repo_id)`).
    - `project_connected_repositories` join table (`project_id FK`, `repository_id FK`, `connected_by FK`, `connected_at`, `PRIMARY KEY (project_id, repository_id)`).
    - `webhook_inbox_events` table (`id UUID PK`, `provider`, `delivery_id`, `event_type`, `payload JSONB`, `status VARCHAR(16)`, `lease_owner_token UUID NULL`, `processing_lease_expires_at TIMESTAMPTZ NULL`, `retry_count INT DEFAULT 0`, `next_retry_at TIMESTAMPTZ NULL`, `error_message TEXT NULL`, `received_at`, `processed_at TIMESTAMPTZ NULL`, `CONSTRAINT uq_webhook_provider_delivery UNIQUE (provider, delivery_id)`).
    - Partial index: `idx_webhook_inbox_pending ON webhook_inbox_events (status, received_at) WHERE status = 'PENDING'`.
    - Partial index: `idx_webhook_inbox_lease ON webhook_inbox_events (status, processing_lease_expires_at) WHERE status = 'PROCESSING'`.
- [ ] **GitHub App Auth Service (`GitHubAppAuthService`):**
  - [ ] Parse RSA private key (supporting PKCS#1 and PKCS#8 formats via BouncyCastle).
  - [ ] Sign ephemeral RS256 JWTs (10-minute validity).
  - [ ] Exchange JWT for installation access tokens; cache in-memory with proactive 50-minute refresh.
- [ ] **Raw-Byte Webhook HMAC Signature Filter (`WebhookSignatureFilter`):**
  - [ ] Intercept `POST /api/v1/integrations/github/webhooks`.
  - [ ] Stream bytes with a strict 2 MB limit (`BoundedInputStream`); return 413 if exceeded.
  - [ ] Compute HMAC-SHA256 over exact raw bytes; compare with `X-Hub-Signature-256` using constant-time comparison (`MessageDigest.isEqual`).
  - [ ] Reject missing or invalid signatures with `401 Unauthorized` before JSON parsing.
  - [ ] Wrap verified request in `CachedBodyHttpServletRequest` for downstream controller.
- [ ] **Webhook Ingestion Controller:**
  - [ ] Parse JSON; reject malformed JSON with 400 Bad Request without writing to inbox.
  - [ ] Deduplicate on `(provider, delivery_id)`; return 200 OK if duplicate.
  - [ ] Insert fresh event with `status = 'PENDING'` and return `202 Accepted` (< 30ms latency target).
- [ ] **Asynchronous Webhook Inbox Worker (`WebhookInboxWorker`):**
  - [ ] Phase 1 (Claim): Select up to 25 events via `FOR UPDATE SKIP LOCKED`, set `status = 'PROCESSING'`, `lease_owner_token = UUID.randomUUID()`, `lease_expires = now() + 5 min`, and COMMIT immediately.
  - [ ] Phase 2 (Process): For each claimed event, process in an isolated `@Transactional` method.
  - [ ] Lease check before commit: Assert worker still owns lease token; if 0 rows updated, rollback and abort to prevent stale overwrites.
  - [ ] Phase 3 (Failure): On exception, rollback domain writes; increment retry count; compute exponential backoff with jitter; mark `FAILED` after 5 retries.
- [ ] **Stuck Event Supervisor:**
  - [ ] `@Scheduled` task scanning events stuck in `PROCESSING` past lease expiration and resetting to `PENDING`.
- [ ] **Automated Reliability Tests:**
  - [ ] Forged HMAC signature returns 401 Unauthorized; zero DB records created.
  - [ ] Repeated delivery of same `X-GitHub-Delivery` returns 200 without duplicate inbox rows.
  - [ ] Poison pill webhook retries 5 times and transitions to `FAILED` without blocking worker.

#### Definition of Done
- Webhooks are cryptographically authenticated against raw bytes and acknowledged in < 30ms.
- Background worker processes events with lease safety, poison-pill defense, and replay deduplication.

---

### Phase 9: Issue-to-Code Traceability (Branches, Commits, PRs & Manual Linking)
**Goal:** Automatically extract issue keys (`NT-101`) from Git telemetry, maintain M:N associations with repositories, and provide a manual linking fallback modal.
- **Status:** `NOT STARTED` ⏹️
- **Dependencies:** Phase 8 complete.

#### Detailed Task Checklist
- [ ] **Database Migrations:**
  - [ ] Create `V009__create_development_artifacts.sql`:
    - `development_branches` (`id UUID PK`, `repository_id FK`, `name`, `last_commit_sha`, `CONSTRAINT uq_branch_repo_name UNIQUE (repository_id, name)`).
    - `development_commits` (`id UUID PK`, `repository_id FK`, `commit_sha`, `message`, `author_name`, `author_email`, `commit_url`, `committed_at`, `CONSTRAINT uq_commit_repo_sha UNIQUE (repository_id, commit_sha)`).
    - `pull_requests` (`id UUID PK`, `repository_id FK`, `provider_pr_id`, `pr_number`, `title`, `description`, `source_branch`, `target_branch`, `pr_url`, `status`, `review_status`, `is_draft`, `merged_at`, `CONSTRAINT uq_pr_repo_num UNIQUE (repository_id, pr_number)`).
    - `issue_development_branches` (`issue_id FK`, `branch_id FK`, `link_type`, `linked_by FK NULL`, `linked_at`, `PRIMARY KEY (issue_id, branch_id)`).
    - `issue_development_commits` (`issue_id FK`, `commit_id FK`, `link_type`, `linked_by FK NULL`, `linked_at`, `PRIMARY KEY (issue_id, commit_id)`).
    - `issue_pull_requests` (`issue_id FK`, `pull_request_id FK`, `link_type`, `linked_by FK NULL`, `linked_at`, `PRIMARY KEY (issue_id, pull_request_id)`).
- [ ] **Regex Issue-Key Parser:**
  - [ ] Implement regex parser `\b([A-Z][A-Z0-9]{1,9})-(\d+)\b`.
  - [ ] Extract keys from branch names, commit messages, and PR titles/descriptions.
  - [ ] Validate matched keys against existing issues in projects linked via `project_connected_repositories`.
  - [ ] Insert association rows with `link_type = 'AUTO_EXTRACTED'` using `ON CONFLICT DO NOTHING`.
- [ ] **Manual Linking Fallback API:**
  - [ ] `POST /api/v1/issues/{id}/development/links`: Manually link a branch, commit, or PR (`link_type = 'MANUAL'`, `linked_by = currentUser`).
  - [ ] `DELETE /api/v1/issues/{id}/development/links/{linkId}`: Remove manual association without deleting repository record.
- [ ] **Frontend Development Panels:**
  - [ ] `IssueDevelopmentPanel` on Issue Detail view (displaying linked branches, commits, PR status pills, and `Auto` vs `Manual` link badges).
  - [ ] `ProjectDevelopmentView` (listing connected repositories, sync status, and commit stream).
  - [ ] Manual Link Modal allowing search by commit SHA or PR number.

#### Definition of Done
- Pushing a commit with message `NT-101: Fix authentication bug` automatically links the commit to issue `NT-101`.
- Manual link modal allows linking unformatted commits with audit tracking.

---

### Phase 10: CI/CD Build & Deployment Pipeline Telemetry
**Goal:** Track GitHub Actions workflow runs and environment deployment statuses linked directly to project issues.
- **Status:** `NOT STARTED` ⏹️
- **Dependencies:** Phase 9 complete.

#### Detailed Task Checklist
- [ ] **Database Migrations:**
  - [ ] Create `V010__create_builds_and_deployments.sql`:
    - `builds` table (`id UUID PK`, `repository_id FK`, `external_build_id`, `build_number`, `pipeline_name`, `commit_sha`, `branch_name`, `status`, `build_url`, `started_at`, `completed_at`, `duration_seconds`, `CONSTRAINT uq_build_repo_ext UNIQUE (repository_id, external_build_id)`).
    - `deployments` table (`id UUID PK`, `repository_id FK`, `external_deployment_id`, `environment`, `commit_sha`, `status`, `deployment_url`, `deployed_by`, `started_at`, `completed_at`, `CONSTRAINT uq_deploy_repo_ext UNIQUE (repository_id, external_deployment_id)`).
    - `issue_builds` join table (`issue_id FK`, `build_id FK`, `link_type`, `linked_at`, `PRIMARY KEY (issue_id, build_id)`).
    - `issue_deployments` join table (`issue_id FK`, `deployment_id FK`, `link_type`, `linked_at`, `PRIMARY KEY (issue_id, deployment_id)`).
- [ ] **Dual-Field Build Status Ingestion:**
  - [ ] Ingest GitHub Actions `workflow_run` webhooks.
  - [ ] Map `status` and `conclusion` accurately:
    - `status=queued` -> `QUEUED`
    - `status=in_progress` -> `IN_PROGRESS`
    - `status=completed` + `conclusion=success` -> `SUCCESS`
    - `status=completed` + `conclusion=failure` -> `FAILED`
    - `status=completed` + `conclusion=cancelled` -> `CANCELLED`
- [ ] **Deployment Ingestion:**
  - [ ] Ingest GitHub `deployment_status` webhooks (`DEVELOPMENT`, `STAGING`, `PRODUCTION`).
  - [ ] Correlate builds and deployments to issues via commit SHA and branch associations.
- [ ] **UI Status Badges:**
  - [ ] Render CI build status chips and deployment environment pills on Issue Detail panel and Development tab.

#### Definition of Done
- GitHub Actions pipeline runs and deployment milestones display accurately on associated issues with execution timings and provider links.

---

### Phase 11: Observability, Performance Benchmarking & Hardening
**Goal:** Seed 10,000+ realistic issues, conduct reproducible k6 load testing, pre-configure Prometheus/Grafana dashboards, and add ShedLock distributed scheduler locks.
- **Status:** `NOT STARTED` ⏹️
- **Dependencies:** Phase 10 complete.

#### Detailed Task Checklist
- [ ] **Demo Seed Data Runner:**
  - [ ] Create Spring Boot seed runner (`--spring.profiles.active=seed`) populating:
    - 2 Workspaces, 3 Projects, 10 Users.
    - 10,000+ Issues across all 5 statuses with realistic titles and labels.
    - 500+ Comments, 20 Connected Repositories, 1,000+ Commits, and 200+ PRs.
- [ ] **Prometheus & Grafana Setup:**
  - [ ] Configure Spring Boot Actuator `/actuator/prometheus`.
  - [ ] Add Prometheus and Grafana services to `docker-compose.yml`.
  - [ ] Commit pre-built dashboard JSON (`docker/grafana/dashboards/nextrack-overview.json`):
    - Webhook ingestion rate & pending inbox count gauge.
    - HTTP request latency percentiles (p50, p95, p99).
    - HikariCP active vs idle connection gauge.
- [ ] **Distributed Scheduler Coordination:**
  - [ ] Add ShedLock (`net.javacrumbs.shedlock:shedlock-spring`).
  - [ ] Create Flyway migration `V011__create_shedlock.sql` (`shedlock` table).
  - [ ] Annotate `WebhookInboxWorker.poll()` and `StuckEventSupervisor.scan()` with `@SchedulerLock`.
- [ ] **Repeatable k6 Benchmark Suite (`scripts/load-tests/`):**
  - [ ] `webhook-ingestion.js`: 100 concurrent virtual users posting signed webhooks; assert < 30ms p95 latency.
  - [ ] `board-listing.js`: Concurrent users fetching `/board` with 10k issues; assert < 200ms p95 latency using `v_board_cards`.
  - [ ] Record benchmark results and execution plans (`EXPLAIN (ANALYZE, BUFFERS)`) in `docs/testing/load-test-results.md`.

#### Definition of Done
- k6 load tests validate < 30ms webhook ingestion and < 200ms board listing on a 10,000+ issue dataset.
- Grafana dashboard visualizes live metrics during load testing.

---

## 📋 Quality & Definition of Done Checklist

Every individual pull request or milestone must satisfy this checklist prior to marking a phase complete:
- [ ] **Domain Integrity:** Request validation (JSR-380) and business invariants are enforced on the server.
- [ ] **Security:** Endpoint authorization verifies workspace and project membership; sensitive tokens are never logged.
- [ ] **Database:** Database migrations (Flyway) are backwards-compatible and include all foreign keys, unique constraints, and justified indexes.
- [ ] **Concurreny:** Mutable entities enforce optimistic locking (`@Version`); concurrent updates map to 409 Conflict.
- [ ] **Testing:** Unit tests cover domain rules; Testcontainers tests verify real PostgreSQL interactions.
- [ ] **Error Handling:** Errors strictly conform to RFC 7807 Problem Details; no stack traces leak in production.
- [ ] **Observability:** Custom metrics and structured logs capture operational telemetry without leaking credentials.
- [ ] **UI Polish:** Loading skeletons, empty states, error banners, and conflict rollbacks are implemented.
