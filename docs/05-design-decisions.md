# Design Decisions

[← Back to README](../README.md)

**Guiding question: why were these decisions made?**

This document answers that question, and only that question — plus the two it implies: what each decision cost, and when it would need to be revisited.

It assumes the reader already has the vocabulary this repository uses — a purchase as a committed amount, an invoice as a claim against that commitment, a vendor, an organizational hierarchy — and introduces no new concepts of its own. Its job isn't to define what an invariant is or what a layered architecture looks like; those exist elsewhere. Its job is to make explicit the reasoning that produced them: what problem each one solved, what alternatives were rejected, and what was traded away to get there.

If financial integrity is the thread running through this entire repository, this is the document where that thread gets tied to specific, defensible choices. Everything else in the repository elaborates on a decision made here.

---

## Table of Contents

1. [How to Read This Document](#1-how-to-read-this-document)
2. [ADR-001 — Layered, Server-Rendered Architecture](#adr-001--layered-server-rendered-architecture)
3. [ADR-002 — Application-Layer Financial Invariant Enforcement](#adr-002--application-layer-financial-invariant-enforcement)
4. [ADR-003 — Computed Status Instead of a Persisted Status Field](#adr-003--computed-status-instead-of-a-persisted-status-field)
5. [ADR-004 — Reference Data Gets History; Transactional Data Doesn't](#adr-004--reference-data-gets-history-transactional-data-doesnt)
6. [ADR-005 — Two-Layer Authorization](#adr-005--two-layer-authorization)
7. [ADR-006 — Identity-Based Visibility Overrides](#adr-006--identity-based-visibility-overrides)
8. [ADR-007 — Co-located Document Storage](#adr-007--co-located-document-storage)
9. [ADR-008 — On-Demand Dashboard Aggregation](#adr-008--on-demand-dashboard-aggregation)
10. [ADR-009 — No Multi-Level Approval Gate](#adr-009--no-multi-level-approval-gate)
11. [What This Document Leaves Out](#11-what-this-document-leaves-out)
12. [Where to Go Next](#12-where-to-go-next)

---

## 1. How to Read This Document

Each entry below follows the same nine-part structure, in the same order, regardless of the decision it covers:

- **Decision** — a one-line name for what was decided.
- **Context** — the situation that made a decision necessary at all.
- **Problem** — the specific tension or requirement being resolved.
- **Alternatives Considered** — the other shapes this decision could plausibly have taken.
- **Decision Taken** — what was actually built.
- **Benefits** — what that choice buys the system.
- **Trade-offs** — what it costs, stated as plainly as the benefits.
- **When This Decision Stops Scaling** — the concrete condition under which this decision would need to be revisited, not a vague gesture at "someday."
- **Related Documents** — where the resulting concept is used operationally elsewhere in this repository.

The nine decisions are ordered to move the way understanding this system actually moves: first the architectural shape everything else lives inside (ADR-001), then the business rules that shape exists to protect (ADRs 2–4), then the operational and implementation choices built on top of both (ADRs 5–9).

---

## ADR-001 — Layered, Server-Rendered Architecture

**Decision.** Build the system as a conventional, server-rendered, layered application — presentation, service, data access — rather than a decoupled frontend communicating with a backend API.

**Context.** The system's job is structured data entry (purchases, invoices, vendor and category administration) and role-scoped reporting, none of which inherently demands a rich, app-like client experience. Its actual difficulty is correctness under concurrent, financially-consequential writes — not interactivity.

**Problem.** Choose an architectural topology that matches where the problem's difficulty actually lives, without over-building for a kind of complexity this problem doesn't have.

**Alternatives Considered.**
- *A decoupled single-page application over an API.* A richer client experience, at the cost of a second contract — the API — to design, version, and keep in sync with the backend, and a second runtime to reason about, for a problem that doesn't need either.
- *A layered, server-rendered application.* One request lifecycle, one place business rules execute, no API contract to maintain separately.

**Decision Taken.** The second option, detailed in [`02-system-architecture.md`](02-system-architecture.md).

**Benefits.** A new engineer can understand "what happens when an invoice is submitted" by reading one service method, without first understanding a separate API contract and a separate frontend build. The invariant enforced in ADR-002 lives in exactly one place, not duplicated or approximated on a client that a malicious or buggy caller could simply not call.

**Trade-offs.** The user experience is bounded by what a server-rendered page can reasonably offer — this is not the right architecture for a product whose value depends on a highly dynamic, real-time interface. It also couples the view layer to the backend more tightly than a decoupled frontend would.

**When This Decision Stops Scaling.** If the system's use cases ever demand genuinely rich, real-time interactivity — live collaborative editing of the same record, for instance — this architecture's server-rendered foundation becomes a real constraint, not a stylistic choice, and would need deliberate reconsideration rather than incremental patching.

**Related Documents.** Full treatment in [`02-system-architecture.md`](02-system-architecture.md); scaling characteristics in [`07-scalability.md`](07-scalability.md).

---

## ADR-002 — Application-Layer Financial Invariant Enforcement

**Decision.** Enforce the rule "invoices against a purchase may never together exceed that purchase's committed value" inside the service layer, at the moment an invoice is submitted — not as a database constraint, and not as a periodic reconciliation job.

**Context.** A purchase commits to a fixed amount. Invoices against it can arrive one at a time, over an unpredictable span of time, from different users, possibly concurrently. Nothing about the shape of a single invoice reveals whether it violates the commitment — only the running total across all of them does.

**Problem.** Decide where, and when, to check a rule that depends on more than one record: at write time, before the invoice is persisted at all, or after the fact, by periodically scanning for violations.

**Alternatives Considered.**
- *Check nothing at write time; reconcile periodically.* Cheapest to implement, but a violation exists — visibly, in the data — for however long it takes the reconciliation job to notice it. For a financial system, "temporarily wrong" is not meaningfully different from "wrong."
- *Enforce the rule as a database constraint* (a check constraint or trigger summing sibling rows). Guarantees the invariant regardless of which application code path writes the row, but most relational databases either can't express a constraint that aggregates across sibling rows declaratively, or only support it through a trigger — moving the business rule out of application code and into database-specific procedural logic that's harder to test, harder to version alongside the rest of the codebase, and invisible to anyone reading the service layer.
- *Enforce the rule in the service layer, synchronously, before the write.* The service method that saves a new invoice first sums the existing invoices against the same purchase, checks the new total against the purchase's committed value, and only proceeds if the check passes.

**Decision Taken.** The third option. Full mechanics in [`03-business-workflows.md`](03-business-workflows.md).

**Benefits.** The invariant is never observably violated, even for a moment — an invoice that would break it is rejected before it's written, not caught afterward. The rule lives in one place, in ordinary application code, next to the other validation the same method already performs, readable by anyone who opens that method without needing to know anything about database-specific trigger syntax. It's trivially unit-testable in isolation from a running database.

**Trade-offs.** The check is only as strong as the transaction boundary around it. If two invoices against the same purchase are submitted concurrently, both requests can read the same "current total" before either writes, both can pass the check independently, and both can be persisted — together violating the invariant that neither one violated alone. Enforcing this correctly under concurrency requires the same transaction that performs the check to also hold a lock (or use an equivalent optimistic-concurrency guard) on the parent purchase for the duration of the write, which this system's isolation level does not guarantee by default. This is one of the most important honest gaps discussed in this repository — see [`06-architecture-trade-offs.md`](06-architecture-trade-offs.md) and [`08-lessons-learned.md`](08-lessons-learned.md).

**When This Decision Stops Scaling.** If invoice submission volume against the same purchase ever becomes genuinely concurrent at meaningful scale — multiple users or automated integrations submitting against the same commitment within the same short window — the application-layer check alone stops being sufficient, and the system needs either row-level locking on the parent purchase for the duration of the check-and-write, or a database-level constraint as a backstop underneath the application check (not a replacement for it — see the trade-off above). Until measured contention demonstrates otherwise, the simpler synchronous check is the right engineering investment.

**Related Documents.** Full lifecycle in [`03-business-workflows.md`](03-business-workflows.md); the concurrency gap is a recurring example in [`06-architecture-trade-offs.md`](06-architecture-trade-offs.md); generic implementation in [`reference-implementations/financial-invariant-validator/`](../reference-implementations/financial-invariant-validator/README.md).

---

## ADR-003 — Computed Status Instead of a Persisted Status Field

**Decision.** Derive a purchase's status (open, partially invoiced, fully settled) and an invoice's status (pending, overdue, settled) from underlying data every time either is read, rather than storing status as its own column and updating it whenever something changes.

**Context.** A purchase's status depends on the invoices against it and how much of each has been paid. An invoice's status depends on whether it's been paid and how its due date compares to today — a fact that changes on its own, with no write ever happening, purely because time passed.

**Problem.** Decide whether "status" is a fact the system stores and keeps in sync, or a question the system answers fresh every time it's asked.

**Alternatives Considered.**
- *A persisted status column, updated by whichever write path changes something relevant.* Cheap to read. But every code path that can affect status — recording a payment, editing an invoice amount, and simply the passage of time for overdue detection — has to remember to also update the field, and "the passage of time" has no write path at all, so a purely time-based transition (pending becoming overdue) would need a scheduled job just to keep the stored value honest.
- *A cached, periodically-refreshed status,* recomputed on a schedule rather than on every write. Reduces the "forgot to update it" risk somewhat, but introduces a window where the stored value is stale by up to one refresh interval — acceptable for some purposes, not for a value a user might act on immediately after a payment is recorded.
- *Compute status on every read, from source data, and never store it at all.* There is no field to forget to update, because there is no field.

**Decision Taken.** The third option. A single function is the one and only authority for what "overdue" means, and every part of the system — detail views, list screens, the dashboard — calls it, rather than reading a stored value that could have drifted.

**Benefits.** Status can never be stale, because it isn't stored anywhere to go stale. There's exactly one place responsible for the computation, so the definition of "overdue" can only be wrong in one place, never inconsistently wrong in two. It correctly handles the class of transition — pure time passing — that has no natural write path to hang an update off of.

**Trade-offs.** Every read pays the computation cost, including reads that happen very often, like a dashboard rendering summary counts across many records. If the computation itself requires touching related records (an invoice's derived status is cheap; a purchase's, which depends on summing its invoices, is not), a naive implementation risks becoming the expensive part of a hot request path — precisely the concern examined in [`07-scalability.md`](07-scalability.md).

**When This Decision Stops Scaling.** When the read volume against a specific computed status genuinely dominates a page's cost — not hypothetically, but measured — the fix is to cache the *result* of the computation for a bounded time or invalidate it explicitly on the specific writes that can affect it, while keeping the computation itself as the single source of truth being cached. That's a caching layer added on top of this decision, not a reversal of it: the computation stays canonical, only its result gets memoized.

**Related Documents.** Full state definitions in [`03-business-workflows.md`](03-business-workflows.md); the "computed vs. persisted" tension generalized in [`06-architecture-trade-offs.md`](06-architecture-trade-offs.md); worked implementation in [`reference-implementations/derived-status-calculator/`](../reference-implementations/derived-status-calculator/README.md).

---

## ADR-004 — Reference Data Gets History; Transactional Data Doesn't

**Decision.** Give reference data — vendors, expense categories, expense types — a full change history, preserving the prior version of a record every time it's edited. Give transactional records — purchases, invoices — no equivalent history; they carry only who last touched them and when, not what the record looked like before.

**Context.** Reference data changes rarely, is edited by a small number of administrative users, and is depended on by many transactional records at once — a single vendor record might be referenced by hundreds of purchases. Transactional records are created far more often, edited by the people who own them, and don't get depended on by other records the way reference data does.

**Problem.** History is not free. It increases storage, write complexity, and long-term maintenance — a second code path (the snapshot-before-update) that every edit has to remember to invoke correctly. Decide which data actually needs to justify that cost, rather than applying the same policy uniformly out of consistency for its own sake.

**Alternatives Considered.**
- *No history anywhere.* Simplest, but loses the ability to answer "what did this vendor's details look like when this purchase was created against it" — a real question, since a purchase references a vendor by identity, not by a frozen copy, so a later edit to the vendor silently changes what old purchases appear to reference.
- *History everywhere, uniformly.* Answers every "what did this look like before" question for every entity, at the cost of a snapshot table and a snapshot-write for every edit to every entity in the system, including high-volume transactional writes where the business value of that history is much less clear.
- *History for reference data only, driven by how each kind of data is actually used* — reference data is edited rarely, by administrators, and is shared across many dependents, which is exactly the profile where "what did this used to say" is a question someone will actually ask. Transactional records are owned by the person who created them and edited far more frequently; a full history for them would grow faster and get consulted less.

**Decision Taken.** The third option, as detailed in [`01-business-domain.md`](01-business-domain.md).

**Benefits.** The data most likely to be silently relied upon by many other records — and therefore most dangerous to change without a trace — is exactly the data that keeps a trace. The cost of history is paid only where the value is clearest.

**Trade-offs.** This is an honest asymmetry, not a hidden one: an edit to a transactional record leaves no record of what it looked like before, only who last touched it and when. If a dispute ever hinges on "what did this specific purchase record say last month, before it was edited," the system cannot answer that question the way it can for a vendor's history. That gap is discussed candidly, not minimized, in [`08-lessons-learned.md`](08-lessons-learned.md).

**When This Decision Stops Scaling.** If transactional-record disputes — "what did this look like before it was changed" — become a recurring, business-relevant need rather than a hypothetical one, the fix is to extend the same snapshot-before-update pattern already proven on reference data to the transactional entities that need it, rather than inventing a different history mechanism for them.

**Related Documents.** Structural definition in [`01-business-domain.md`](01-business-domain.md); the master-vs-transactional tension as a transferable lesson in [`06-architecture-trade-offs.md`](06-architecture-trade-offs.md); the honest gap in [`08-lessons-learned.md`](08-lessons-learned.md).

---

## ADR-005 — Two-Layer Authorization

**Decision.** Authorize every action through two independent checks: a coarse, role-based check determining whether a user can reach a feature at all, followed by a narrower, record-level check determining whether they can act on the specific record in question.

**Context.** Two different classes of unauthorized access need preventing: a user reaching an action their role has no business performing at all, and a user reaching an action their role permits in general but not on the specific record they're pointing at.

**Problem.** A single authorization check tends to be either cheap and coarse, or precise and comparatively expensive — rarely both. Relying on only the cheap one leaves record-specific access structurally unverified.

**Alternatives Considered.**
- *A single, role-based check only.* Fast, simple, and structurally blind to record-specific entitlement — a user with the right role for a *feature* can reach any record that feature exposes, whether or not it's theirs. This is a common, easy mistake, and one worth naming plainly: a route-level check answers "can this role use this feature," never "does this user have any relationship to this specific record."
- *A single, always-record-level check.* Precise, but requires loading and evaluating a specific record on every request, including requests that were never going to be authorized regardless of which record they named.
- *Two layers, in sequence.* The cheap check eliminates the bulk of illegitimate requests early; the precise check is only paid for once a specific, plausible record is already in hand.

**Decision Taken.** The two-layer model, full mechanics in [`04-security-model.md`](04-security-model.md).

**Benefits.** Each layer is reasoned about independently — the route-level check is a simple, auditable table; the record-level check is a single, centralized comparison against the record's owning context. Together they close a gap that neither closes alone.

**Trade-offs.** Two checks are more surface area to keep in sync than one, and they have to stay genuinely distinct in responsibility. The moment a system starts applying the two layers inconsistently — some record-reading endpoints getting the record-level recheck, others quietly skipped because "it's just a read" — the coverage gap that the whole model exists to close reopens silently, and reopens exactly where it's least visible: on reads, which don't announce their own absence the way a broken write path would. This repository treats that inconsistency as a real, worth-naming risk rather than a hypothetical one — see [`06-architecture-trade-offs.md`](06-architecture-trade-offs.md).

**When This Decision Stops Scaling.** If record-level checks become a measurable performance bottleneck at high request volume, the fix is to make the check itself cheaper — better indexing, targeted caching of the ownership lookup — never to weaken it or fold it back into the coarse layer. [`07-scalability.md`](07-scalability.md) covers this concretely.

**Related Documents.** Full mechanics in [`04-security-model.md`](04-security-model.md); the coverage-gap risk generalized in [`06-architecture-trade-offs.md`](06-architecture-trade-offs.md); generic implementation in [`reference-implementations/layered-authorization-example/`](../reference-implementations/layered-authorization-example/README.md).

---

## ADR-006 — Identity-Based Visibility Overrides

**Decision.** Grant a small number of specifically designated individuals visibility across organizational boundaries that the standard role hierarchy does not otherwise cross, via an explicit identity-based allow-list rather than a new, formally modeled role.

**Context.** The system scopes most users' visibility to their own organizational unit. A small number of people — group-level finance reviewers, in this domain — legitimately need visibility across multiple units, but that need applies to specific, known individuals at a point in time, not to a role that any future hire in a given position would automatically inherit.

**Problem.** Decide how to grant an exception to the standard visibility rule when the exception applies to specific people rather than a cleanly definable role.

**Alternatives Considered.**
- *Model a new, formal role* ("Group Reviewer") with its own defined scope, assignable through the same role-administration mechanism as every other role. The generalizable, "correct" answer — but for a handful of people, it means building and maintaining a whole role-scope abstraction for a need that, at this system's current scale, doesn't yet recur often enough to justify it.
- *An identity-based override* — a short, explicit list of specific individuals granted expanded scope, checked directly rather than through the role system.

**Decision Taken.** The second option — simpler to build and to reason about for a small, stable number of people, at a real and openly acknowledged cost described below.

**Benefits.** Extremely simple to implement and to audit for exactly as long as the list stays short: anyone can read the check and know precisely who has expanded visibility and why, without tracing through a role hierarchy.

**Trade-offs.** This is the clearest example in the whole system of a shortcut that works precisely because of how small its scope is, and stops working the moment that scope grows. It doesn't scale past a handful of people before it becomes its own maintenance burden. It requires a code change — not a configuration or data change — to add or remove someone, which means the people best positioned to know that access should change (an HR or organizational change) are the least likely to be the ones who can make that change happen. It's a deliberate trade of correctness-at-scale for simplicity-at-small-scale, and it's worth naming as exactly that rather than dressing it up as a permanent architectural pattern.

**When This Decision Stops Scaling.** The moment the list of individuals needing cross-organizational visibility grows past a handful, or changes with any regularity, this stops being a reasonable shortcut and becomes a liability — at that point it should be replaced with this ADR's first rejected alternative, a properly modeled role, not patched by making the list longer.

**Related Documents.** Operational use in [`04-security-model.md`](04-security-model.md); discussed as a transferable "simple-now, doesn't-scale-later" pattern in [`06-architecture-trade-offs.md`](06-architecture-trade-offs.md) and revisited honestly in [`08-lessons-learned.md`](08-lessons-learned.md).

---

## ADR-007 — Co-located Document Storage

**Decision.** Store uploaded documents (purchase order copies, supporting evidence, invoice copies) directly alongside the transactional record they belong to, rather than in a separate file-storage system referenced by a pointer.

**Context.** Every purchase and invoice can carry one or more attached documents, and those documents are functionally inseparable from the record they support — a purchase without its purchase order copy is missing part of what makes it verifiable.

**Problem.** Decide whether file content lives in the same store as the record that owns it, or in a separate, purpose-built file store referenced by a pointer.

**Alternatives Considered.**
- *A separate file store* (object storage or a filesystem), with the transactional record holding a reference to it. The conventional answer for file-heavy systems — keeps large binary content out of the primary data store, and scales file storage independently of transactional storage.
- *Store the document directly alongside the record.* Guarantees the record and its supporting document can never point at each other incorrectly and can never be independently lost — deleting or backing up the record inherently includes what it depended on, with no second system to keep in sync.

**Decision Taken.** The second option, for the volume and size of documents this system actually handles.

**Benefits.** There's no separate file-storage system to provision, back up, or keep consistent with the primary data store — one backup captures both. A record and its supporting evidence can never drift apart the way a pointer and its target can if one is deleted and the other isn't.

**Trade-offs.** As document volume and size grow, storing binary content alongside transactional data makes the primary data store larger and more expensive to back up and query efficiently than it would be if large content lived elsewhere — a cost this decision accepts deliberately at today's scale, not one it's blind to.

**When This Decision Stops Scaling.** If document volume or individual file sizes grow enough that they meaningfully affect the primary data store's backup time, replication cost, or query performance, the answer is to migrate to a separate file store with the record holding a reference — the first rejected alternative above — not to keep growing the current approach past the point it was designed for.

**Related Documents.** Operational detail in [`01-business-domain.md`](01-business-domain.md); revisited as a "right choice at the time, worth watching" example in [`08-lessons-learned.md`](08-lessons-learned.md).

---

## ADR-008 — On-Demand Dashboard Aggregation

**Decision.** Compute dashboard metrics — open/settled counts, aging buckets, totals — live, from current transactional data, on every dashboard load, rather than maintaining a precomputed or cached summary table.

**Context.** The dashboard's numbers need to reflect the current state of every purchase and invoice a user is scoped to see, and that underlying data changes continuously as records are created and updated.

**Problem.** Decide whether "the current numbers" are computed fresh every time someone asks, or maintained as a running, incrementally-updated summary that every relevant write has to remember to keep current.

**Alternatives Considered.**
- *A precomputed summary,* updated incrementally by every write that could affect it. Fast to read, but every write path that touches a purchase or invoice has to remember to update the right summary rows correctly — the same "forgot to keep it in sync" risk ADR-003 rejected for individual record status, now at the aggregate level, where a mistake is harder to spot because it doesn't obviously break any one record.
- *On-demand, live aggregation,* computed at read time directly from the transactional tables.

**Decision Taken.** The second option — the same "compute it, don't store it" philosophy as ADR-003, applied at the aggregate level. This follows directly from that same principle: derive facts from authoritative data instead of maintaining secondary representations that can drift.

**Benefits.** The dashboard is always exactly current — there's no summary table that can be forgotten, updated late, or updated incorrectly by a write path that didn't anticipate its effect on the aggregate. One computation is the single source of truth for both an individual record's status and its contribution to every aggregate that includes it.

**Trade-offs.** This is the most expensive way to answer the dashboard's questions, computationally, and its cost grows with the volume of transactional data being scanned, not with how often the dashboard is viewed. It is the most direct example in this system of a decision that's correct in principle and needs an explicit performance answer in practice — covered fully, including where it currently costs more than it should, in [`07-scalability.md`](07-scalability.md).

**When This Decision Stops Scaling.** Once the volume of transactional data being aggregated makes live computation noticeably slow at normal dashboard-viewing frequency, the answer is to cache the *result* for a bounded time window — seconds to a few minutes, depending on how current the dashboard needs to feel — while keeping live computation as the definition of correctness that the cache is approximating, not replacing it with a separately-maintained summary that could drift.

**Related Documents.** Full aggregation mechanics in a future dashboard-focused treatment; cost analysis in [`07-scalability.md`](07-scalability.md); the same computed-vs-persisted reasoning generalized in [`06-architecture-trade-offs.md`](06-architecture-trade-offs.md).

---

## ADR-009 — No Multi-Level Approval Gate

**Decision.** Give purchases and invoices no human approval or sign-off step. Once a record is entered correctly and passes the financial invariant check, it stands — its status is authoritative from the moment it's saved, not provisional pending someone else's review.

**Context.** Many enterprise systems in adjacent domains route a request through a chain of human approvers before it takes effect. This system's engineering problem is different: it isn't about who is entitled to authorize an action, it's about whether the numbers stay internally consistent once entered.

**Problem.** Decide whether the system's correctness guarantee comes from a human review step, an automatic invariant check, or both — and if only one, which one actually addresses the risk that matters here.

**Alternatives Considered.**
- *A multi-level approval chain*, requiring sign-off before a purchase or invoice takes effect — the shape a workflow-oriented system in an adjacent domain might reasonably take. **An approval is not an invariant.** Would add a human checkpoint, but wouldn't itself prevent an approved invoice from violating the invariant in ADR-002 — a human approving a number doesn't re-derive whether it's mathematically consistent with everything else already recorded against the same purchase, a computer does that reliably and a person does it inconsistently.
- *No human gate; an automatic, synchronous invariant check instead.* The check that actually addresses this system's specific risk — internal financial consistency — rather than a different risk (authorization to spend) that a different class of system is built to solve.

**Decision Taken.** The second option. This system is deliberately not a workflow-orchestration system; see [`00-overview.md`](00-overview.md) for the fuller distinction between "who is allowed to authorize this" and "is this internally consistent," and why this system solves only the second.

**Benefits.** The system's correctness doesn't depend on a person remembering to check something a computer is better positioned to check exactly, every time, instantly. There is no queue, no waiting, no process to route around when someone is unavailable — the record is authoritative the moment it's entered and passes the invariant.

**Trade-offs.** This system provides no mechanism for a second person to catch a mistake that is *valid* under the invariant but still wrong in some other sense — a legitimate-looking invoice entered against the wrong purchase, for instance, or a correctly-formed record that simply shouldn't have been created. An invariant check verifies internal consistency; it cannot verify intent. A system that needs a human check against intent, not just arithmetic, needs an approval layer this one deliberately doesn't have.

**When This Decision Stops Scaling.** If the organization's real risk shifts from "are the numbers consistent" to "should this specific commitment have been authorized in the first place," that's a different problem needing a different kind of system — an approval workflow layered on top of, not instead of, the invariant enforcement this system already does. The two are not substitutes for each other, and recognizing that distinction early avoids building the wrong one.

**Related Documents.** The domain-shape distinction in [`00-overview.md`](00-overview.md); the lifecycle this replaces a gate with in [`03-business-workflows.md`](03-business-workflows.md).

---

Every implementation choice exists to preserve one or more guarantees.

---

## 11. What This Document Leaves Out

- The mechanics of any individual concept named here — those belong to the document that owns it, linked in each ADR's "Related Documents."
- Quantified performance thresholds — this document explains *why* a decision would need revisiting, not the specific load figures at which that happens. Those belong in [`07-scalability.md`](07-scalability.md).
- New concepts. By design, this document introduces none — see [Section 1](#1-how-to-read-this-document).

---

## 12. Where to Go Next

This document explained why the system's foundational decisions were made. The next question is how the resulting architecture holds up structurally.

- Continue to [`02-system-architecture.md`](02-system-architecture.md) to see the shape these decisions produced.
- Continue to [`06-architecture-trade-offs.md`](06-architecture-trade-offs.md) for the tensions named throughout this document, generalized beyond this specific system.
- Continue to [`07-scalability.md`](07-scalability.md) for the concrete conditions under which each "When This Decision Stops Scaling" note above would actually trigger.
