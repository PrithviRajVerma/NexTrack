# ⚡ NexTrack

> **High-Performance Issue Tracking & Software Development Management Platform**  
> *A production-grade, modular monolith built with Java 21, Spring Boot 3, PostgreSQL, React, and TypeScript.*

---

## 🌟 Overview

**NexTrack** is a modern project-management and software development tracking platform inspired by Linear's keyboard-centric, high-performance user experience and Jira's deep developer ecosystem integration.

Unlike conventional lightweight Kanban tools, NexTrack bridges the gap between project management and actual software delivery:
- **Jira-Inspired Development Module:** First-class traceability linking branches, commits, pull requests, CI/CD builds, and deployments directly to project issues via GitHub App integration.
- **Asynchronous Webhook Inbox Pattern:** High-throughput webhook ingestion decoupling untrusted external telemetry dispatches from relational database processing using a 2-phase claim/lease lifecycle with row-level locking (`FOR UPDATE SKIP LOCKED`).
- **Atomic Concurrency Controls:** Atomic row counter increment guaranteeing monotonically increasing issue numbering (`NT-101`) without table locks, paired with `@Version` optimistic locking for safe concurrent issue collaboration.
- **Modular Monolith Architecture:** Explicit domain module boundaries (`identity`, `workspace`, `project`, `issue`, `workflow`, `development`, `notification`, `audit`) communicating via typed public interfaces and domain events, ready for smooth future evolution without microservice network penalties.

---

## 📐 Architecture Specification

The complete, frozen engineering design specification and architectural decision records (ADRs) are documented in:
📄 **[`architecture.md`](./architecture.md)**

For step-by-step development tracking across all 12 phases, see:
📋 **[`implementation.md`](./implementation.md)**

### Key Technical Patterns
1. **GitHub App Primary Integration:** Installation-based authentication with RS256 JWTs and ephemeral in-memory cached tokens (50-min TTL), eliminating personal access token risks.
2. **Pre-Parsing HMAC-SHA256 Verification:** `WebhookSignatureFilter` enforces bounded streaming (max 2 MB limit) and validates raw request bytes prior to JSON parsing, preventing DoS and deserialization vulnerabilities.
3. **2-Phase Decoupled Worker Transactions:** Background workers claim inbox records via `SKIP LOCKED` and an ownership token (`lease_owner_token`), safely processing events in isolated transactions and rolling back on lease expiration to prevent stale overwrites.
4. **Validated Issue Workflow State Machine:** Fixed v1 state transition rules (`BACKLOG` → `TO_DO` → `IN_PROGRESS` → `IN_REVIEW` → `DONE`) with comprehensive audit event logging.
5. **Linear Dial-Theme Design System:** Accessible, dark-mode-first aesthetic with high-density task lists, interactive drag-and-drop boards, and instant keyboard navigation.

---

## 🎨 UI & Design Previews

The repository includes design mockups and theme prototypes:
- **Design Screen Previews:** View [`preview-designs.html`](./preview-designs.html) or browse the [`screens/`](./screens) directory.
- **Linear Dial Dark Theme:** Located in [`ui-linear-dark/`](./ui-linear-dark).

---

## 🗺️ Implementation Roadmap

NexTrack follows a phased engineering roadmap:
- **Phase 0:** Repository initialization, design freeze & documentation baseline. *(Completed)*
- **Phase 1:** Spring Boot 3 & React skeleton with PostgreSQL Flyway migrations.
- **Phase 2:** Multi-tenant workspace management, RBAC, and project key allocation.
- **Phase 3:** Issue aggregate, atomic monotonic issue numbering, and optimistic locking.
- **Phase 4:** Validated issue workflow state machine & audit logging.
- **Phase 5:** End-to-end Issue Board UI with optimistic React updates.
- **Phase 6:** In-app notifications & Transactional Outbox pattern.
- **Phase 7:** Search, filtering, and rich attachments.
- **Phase 8:** Development Module & GitHub App Webhook Inbox integration.
- **Phase 9:** Automatic issue-key extraction, branch/commit/PR tracking & manual linking.
- **Phase 10:** CI/CD build & deployment tracking.
- **Phase 11:** Development analytics, cycle time metrics & performance benchmarks.

---

## 📄 License

This project is licensed under the MIT License.
