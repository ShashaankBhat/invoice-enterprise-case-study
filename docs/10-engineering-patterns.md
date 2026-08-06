# Engineering Patterns

[← Back to README](../README.md) · [← Previous: Lessons Learned](08-lessons-learned.md)

**Guiding question: which patterns here transfer to other systems, independent of this domain?**

A pattern is just a guarantee's cost given a name other engineers already recognize. Every concept this repository has built up — Single Ownership, Computed State, Eligibility and Entitlement, an invariant enforced at write time, reference data that outlives the transactions that cite it — has a name in the wider engineering vocabulary, usually several. This document is where those names get attached, so a reader can carry the pattern into a system that has nothing to do with purchases or invoices and still recognize it on arrival.

---

## Table of Contents

1. [What Makes Something a Pattern Here](#1-what-makes-something-a-pattern-here)
2. [Single Ownership](#2-single-ownership)
3. [Derive, Don't Duplicate](#3-derive-dont-duplicate)
4. [Coarse-Then-Fine Authorization](#4-coarse-then-fine-authorization)
5. [The Aggregate Invariant](#5-the-aggregate-invariant)
6. [Reference Data With History](#6-reference-data-with-history)
7. [The Explicit Exception List](#7-the-explicit-exception-list)
8. [The Supporting Cast](#8-the-supporting-cast)
9. [What This Document Leaves Out](#9-what-this-document-leaves-out)
10. [Where to Go Next](#10-where-to-go-next)

---

## 1. What Makes Something a Pattern Here

Not every technical choice in this system is a pattern worth naming — a pattern, in the sense this document means it, is a recurring answer to a recurring problem, portable enough that naming it helps a reader solve a *different* problem later. Six qualify. Each section below follows the same shape: the problem the pattern answers, how this system uses it, where else the same shape shows up, and the cost [Architecture Trade-offs](06-architecture-trade-offs.md) already named for it — because a pattern without its cost attached is marketing, not engineering.

---

## 2. Single Ownership

**The problem.** A rule that can be enforced from more than one place will eventually be enforced inconsistently between them — not because anyone made a mistake, but because two copies of the same logic drift the moment one is changed and the other isn't. Elsewhere this same idea goes by Single Source of Truth, Sole Authority, or Single Writer, depending on which corner of the industry is naming it — this repository has been calling it Single Ownership since [Architecture Trade-offs](06-architecture-trade-offs.md).

**How this system uses it.** The financial invariant lives in exactly one method, in exactly one layer, per [ADR-002](05-design-decisions.md#adr-002--application-layer-financial-invariant-enforcement). Every caller routes through it; none of them re-implement it.

**Where else it applies.** Any business rule that could plausibly be checked from multiple entry points — client-side and server-side validation, multiple API endpoints performing the same write, multiple services that could each independently decide whether an operation is allowed. The pattern is the same regardless of domain: pick one place, make every path go through it, and resist the convenience of a second, "just this once" implementation.

**The cost.** Coupling — everything that needs the rule now depends on the one place that owns it. [Architecture Trade-offs, Section 2](06-architecture-trade-offs.md#2-single-ownership-duplication-vs-coupling) names this trade explicitly: coupling, accepted deliberately, to avoid duplication, which is worse.

---

## 3. Derive, Don't Duplicate

**The problem.** A value that's stored separately from the data it summarizes needs every writer of that underlying data to remember to update it — and "remember" is not a mechanism, it's a hope.

**How this system uses it.** Purchase and invoice status, and every dashboard aggregate, are computed from source data on every read, never persisted as their own field — [ADR-003](05-design-decisions.md#adr-003--computed-status-instead-of-a-persisted-status-field) and [ADR-008](05-design-decisions.md#adr-008--on-demand-dashboard-aggregation).

**Where else it applies.** Any derived value with more than one contributing source — an order's total computed from its line items rather than stored and adjusted, a user's reputation score computed from their activity rather than incremented in a dozen different places, a document's "last modified" summary computed from its actual edit history rather than tracked by hand. The tell is the same everywhere: if a value's correctness depends on every future change remembering to touch it, it's a candidate to be derived instead.

**The cost.** Computation, paid on every read instead of once per write — [Scalability, Section 2](07-scalability.md#2-computed-state-at-scale) covers exactly where that becomes the wrong exchange rate, and what to do about it without abandoning the pattern.

---

## 4. Coarse-Then-Fine Authorization

**The problem.** A single authorization check tends to be either cheap and blind to record-specific context, or precise and expensive to run on every request — rarely both at once.

**How this system uses it.** Every action passes a cheap, role-based Eligibility check before a narrower, record-level Entitlement check — [ADR-005](05-design-decisions.md#adr-005--two-layer-authorization), covered in full in [Security Model](04-security-model.md).

**Where else it applies.** This is one of the most portable patterns in the whole document — coarse-then-fine checks show up anywhere a system needs to reject the bulk of illegitimate requests cheaply before spending anything precise: rate limiting before request validation, a cache check before a database query, a schema check before a business-rule check. The shape is always the same: filter cheap and broad first, verify expensive and specific second, and never let the second check be the only one running.

**The cost.** Two checks to keep conceptually distinct forever, rather than one — [Architecture Trade-offs, Section 4](06-architecture-trade-offs.md#4-two-layer-authorization-precision-vs-cost) names the risk directly: the two checks can be applied inconsistently if a system stops treating them as genuinely separate concerns.

---

## 5. The Aggregate Invariant

**The problem.** A rule that spans more than one record — "these child records may never together exceed a value fixed on their parent" — can't be verified by validating any single record in isolation.

**How this system uses it.** The invariant from ADR-002 is checked against the running total of every invoice tied to the same purchase, synchronously, before a new one is accepted.

**Where else it applies.** This is one concrete example of what domain-driven design calls an aggregate boundary — a cluster of records that must be kept consistent as a unit, with exactly one path allowed to write to any of them. It's worth noting explicitly that this system achieves that guarantee without a rich domain model wrapping the records themselves — [ADR-003](05-design-decisions.md#adr-003--computed-status-instead-of-a-persisted-status-field) already established that this system's entities are largely data holders, not behavior-bearing objects. The invariant lives in the service layer instead of on the parent record, which shows the underlying idea — one enforced boundary around a cluster of related records — doesn't require any particular implementation style to work. Any system with a "total of children may not exceed parent" shape has this same problem: line items against a budget, seats booked against a venue's capacity, votes cast against an allocated quota.

**The cost.** Everything named in [ADR-002](05-design-decisions.md#adr-002--application-layer-financial-invariant-enforcement)'s trade-offs, chief among them the concurrency gap discussed at length in [Lessons Learned, Section 2](08-lessons-learned.md#2-the-concurrency-gap) — an aggregate invariant checked outside a lock is only as strong as the assumption that nothing else is writing to the same aggregate at the same instant.

---

## 6. Reference Data With History

**The problem.** Some data is shared by many other records and edited rarely; other data is created constantly and owned by whoever created it. Treating both the same way — either no history anywhere, or history everywhere — ignores that they have different risk profiles.

**How this system uses it.** Vendors, expense categories, and expense types carry a full snapshot-before-update history. Purchases and invoices don't — [ADR-004](05-design-decisions.md#adr-004--reference-data-gets-history-transactional-data-doesnt).

**Where else it applies.** Any system with a clear split between shared, slowly-changing reference data and high-volume transactional data that references it — product catalogs versus orders, currency exchange rates versus the transactions priced against them, org-chart data versus the records that cite an employee by role. The pattern isn't "audit everything" or "audit nothing." It's "audit the data whose silent drift would be most dangerous to whoever depends on it," which is usually the shared, rarely-changing data, not the high-volume data changing constantly under its own owner's control.

**The cost.** An honest asymmetry — the data that doesn't get history can't answer "what did this used to say" if that question ever becomes important. [Lessons Learned, Section 3](08-lessons-learned.md#3-the-history-asymmetry) covers exactly when that gap became worth narrowing.

---

## 7. The Explicit Exception List

**The problem.** A small number of specific people sometimes need access that the standard role hierarchy doesn't grant, and building a fully general role for a need that applies to a handful of named individuals can be more machinery than the problem justifies — at first.

**How this system uses it.** [ADR-006](05-design-decisions.md#adr-006--identity-based-visibility-overrides) grants expanded visibility through a short, explicit, identity-based list rather than a new role.

**Where else it applies.** This pattern shows up constantly, under different names, in systems of every size — an allow-list of specific accounts with elevated access, a hardcoded set of internal users exempt from a rate limit, a short list of email addresses that bypass a normal approval step "for now." It's a legitimate, useful pattern for exactly the scale it's built for. It is also, by a wide margin, the most commonly *misused* pattern on this list, because nothing about it signals when it's stopped being appropriate.

**The cost.** This is the pattern [Lessons Learned, Section 4](08-lessons-learned.md#4-the-identity-override) revisited most directly: the pattern itself isn't the risk, an *open-ended* instance of it is. Anywhere this pattern is used, it should be used with an explicit expiration condition attached from the start — a threshold, a date, an owner — not adopted as a permanent architectural feature that happens to look small today.

---

## 8. The Supporting Cast

A handful of other patterns are present throughout this system without needing their own section, because [System Architecture](02-system-architecture.md) already covers their structural role in full. Unlike the six above, these are **structural implementation patterns** rather than guarantee-preserving ones — they shape how the system is organized, not what it promises to keep true: a Repository abstraction separating business logic from query mechanics, a Service Layer owning transaction boundaries and business rules, and Data Transfer Objects shaping what crosses a layer boundary without exposing internal record structure directly. These are foundational enough, and common enough, that naming them here is a courtesy to readers building a pattern vocabulary — not a claim that this system does anything novel with them.

---

Every pattern in this document is the same idea wearing different clothes: decide where a guarantee's cost is paid, name that decision, and never let the pattern's convenience obscure the trade-off underneath it.

---

## 9. What This Document Leaves Out

- The mechanics of how any of these patterns are implemented in this specific system — that belongs to the documents already cited throughout, especially [System Architecture](02-system-architecture.md) and [Design Decisions](05-design-decisions.md).
- Patterns from the broader software-engineering literature that this system doesn't actually use — this is a catalog of what's present, not a survey of what exists.
- A claim that any of these six patterns originated here — none of them did; the contribution of this document is connecting each one to a concrete, examined instance rather than presenting it in the abstract.

---

## 10. Where to Go Next

This document named the patterns. The next one asks how to keep applying them correctly as the system keeps changing.

- Continue to [`11-system-evolution.md`](11-system-evolution.md) for how to read these same patterns — deliberate or accidental — in a codebase you didn't write.
- Revisit [Architecture Trade-offs](06-architecture-trade-offs.md) for the cost each pattern above was shown to carry.
- Revisit [Lessons Learned](08-lessons-learned.md) for what happens when a pattern's expiration condition is left implicit.
