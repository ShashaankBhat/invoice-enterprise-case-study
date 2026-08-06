# Scalability

[← Back to README](../README.md) · [← Previous: Architecture Trade-offs](06-architecture-trade-offs.md)

**Guiding question: when does this architecture stop working, and what gives first?**

Every optimization spends correctness, complexity, or money. None of them are free, and none of them spend nothing — an optimization that seems to cost nothing has simply hidden its cost somewhere the person applying it hasn't looked yet. [Architecture Trade-offs](06-architecture-trade-offs.md) established which currency each guarantee in this system already pays in. This document asks what happens to that bill as load grows, and — for each guarantee — what it costs to keep paying it versus what it costs to stop.

---

## Table of Contents

1. [Scaling Is Renegotiating Who Pays](#1-scaling-is-renegotiating-who-pays)
2. [Computed State at Scale](#2-computed-state-at-scale)
3. [Keeping Entitlement Cheap](#3-keeping-entitlement-cheap)
4. [Paying Less for the Financial Invariant](#4-paying-less-for-the-financial-invariant)
5. [Single Ownership Under Load](#5-single-ownership-under-load)
6. [The Costs Scaling Can't Remove](#6-the-costs-scaling-cant-remove)
7. [What This Document Leaves Out](#7-what-this-document-leaves-out)
8. [Where to Go Next](#8-where-to-go-next)

---

## 1. Scaling Is Renegotiating Who Pays

A system under light load can afford to pay every guarantee's cost in the most straightforward currency available — recompute it live, check it precisely every time, route it all through one owner. Load doesn't change whether a guarantee is worth keeping. It changes whether the *cheapest possible way* of keeping it is still affordable. Scaling this system correctly means renegotiating where each guarantee's cost is paid — never quietly agreeing to stop paying it.

---

## 2. Computed State at Scale

**The guarantee.** A purchase's status, an invoice's status, and every dashboard aggregate are computed from current data on every read, never stored — [ADR-003](05-design-decisions.md#adr-003--computed-status-instead-of-a-persisted-status-field) and [ADR-008](05-design-decisions.md#adr-008--on-demand-dashboard-aggregation).

**Why it becomes expensive.** The cost of computing scales with the volume of underlying data being read, not with how often a person happens to look at it. A dashboard scoped to a large organizational unit re-scans a proportionally large amount of transactional data on every single load, whether the last load was ten seconds ago or ten hours ago.

**What preserves the guarantee.** Caching the *result* of a computation for a short, bounded window, invalidated the moment a write occurs that could change it — the value shown is still always derived from the same authoritative computation, just not re-derived on every millisecond. This spends complexity (a cache with correct invalidation is a real thing to get right) to buy back performance without touching what "correct" means.

**What quietly weakens it.** Replacing live computation with a separately-maintained summary table, updated incrementally by whichever write paths someone remembered to update. This is the exact alternative ADR-003 and ADR-008 already rejected, reintroduced under the name "optimization" — it spends correctness to buy performance, and it's the single most common way a computed-state guarantee gets undone by someone who wasn't told why it existed in the first place.

**When the trade-off is justified.** When measured, not assumed: once a specific read path's live-computation cost is shown to dominate its total request time at realistic volume. Not before — caching a computation that was never expensive spends complexity for nothing.

---

## 3. Keeping Entitlement Cheap

**The guarantee.** Every record-bearing request is checked against the specific record's ownership or scope, not just the requester's role — [ADR-005](05-design-decisions.md#adr-005--two-layer-authorization).

**Why it becomes expensive.** Entitlement, by definition, requires loading something about the specific record before it can be evaluated. At high request volume, that per-request lookup is pure overhead layered on top of Eligibility's already-cheap check.

**What preserves the guarantee.** Indexing the specific columns an Entitlement check actually reads, and — where the same record's Entitlement is checked repeatedly in a short window — caching the *narrow* result ("this user may act on this record") for a short duration, scoped tightly enough that a change in ownership is reflected almost immediately. This spends memory and a small amount of staleness tolerance to keep the check itself fast, without changing what it checks.

**What quietly weakens it.** Skipping the Entitlement check on paths that "are just reads," or caching it broadly enough — by role, rather than by record — that it stops being a record-level check at all. Both spend exactly the coverage this repository named as a real risk in [Security Model, Section 6](04-security-model.md#6-where-the-two-checks-can-quietly-drift-apart): the check still runs, technically, but it's stopped answering the question it exists to answer.

**When the trade-off is justified.** Narrow, record-scoped caching with a short expiry is close to always justified once volume makes it worth the complexity. Widening the cache's scope past the individual record is never justified by a performance argument — that's not an optimization of Entitlement, it's a quiet return to Eligibility wearing Entitlement's name.

---

## 4. Paying Less for the Financial Invariant

**The guarantee.** No combination of invoices against a purchase can exceed that purchase's committed value, checked synchronously at write time — [ADR-002](05-design-decisions.md#adr-002--application-layer-financial-invariant-enforcement).

**Why it becomes expensive.** As concurrent submissions against the same purchase grow more frequent, correctly closing the concurrency gap already named in ADR-002 and [Architecture Trade-offs, Section 5](06-architecture-trade-offs.md#5-application-layer-invariants-portability-vs-concurrency) requires the write to hold a lock, or retry under optimistic concurrency, for the duration of the check. Both add latency and, under contention, queuing — a cost that scales with how often multiple invoices race against the same purchase, not with overall system volume.

**What preserves the guarantee.** Paying that latency cost directly — a short-lived lock scoped to the single purchase being written to, or an optimistic-concurrency retry loop bounded to a small number of attempts. This spends money, in the literal sense of compute time spent waiting or retrying, to keep the invariant genuinely unbreakable rather than merely usually-true.

**What quietly weakens it.** Relaxing the check to run asynchronously, "eventually" reconciling instead of blocking the write — which is precisely the first alternative ADR-002 already considered and rejected, for the same reason it would be a mistake here: a financial invariant that's temporarily false is, for a financial system, simply false, for however long "temporarily" lasts.

**When the trade-off is justified.** Only if the business itself decides a brief window of possible inconsistency is an acceptable cost — a decision this repository has been explicit does not currently hold. Scaling this guarantee means paying more for the lock or the retry, not paying less for a weaker version of the promise.

---

## 5. Single Ownership Under Load

**The guarantee.** Every business rule this repository discusses is enforced in exactly one place — [Architecture Trade-offs, Section 2](06-architecture-trade-offs.md#2-single-ownership-duplication-vs-coupling).

**Why it becomes expensive.** A single logical owner is also a coordination point: as load grows, every request needing that rule enforced passes through the same narrow point, which can become a throughput ceiling even when every individual check is fast.

**What preserves the guarantee.** Scaling the owner horizontally — more instances of the same enforcement logic, running behind the same interface, still one *logical* owner even though it's no longer one physical process. This spends money (more compute) to keep the "exactly one place" guarantee intact under more load than a single instance could carry.

**What quietly weakens it.** Splitting the rule itself across multiple independently-maintained owners to relieve pressure on any one of them — which spends back the exact consistency Single Ownership was bought to protect in the first place. Two owners of the same rule can disagree; that was the whole risk Single Ownership existed to close.

**When the trade-off is justified.** Horizontal scaling of a single logical owner is close to always the right first move. Splitting ownership is justified only alongside a new, explicit coordination mechanism that replaces what a single owner used to guarantee for free — never as a plain response to load.

---

## 6. The Costs Scaling Can't Remove

Not every cost named in this document shrinks with a better implementation. The concurrency gap in Section 4 doesn't disappear at any scale — it only becomes more or less frequent depending on how many concurrent submissions actually collide, and the fix is always the same lock or retry, paid more or less often. Some costs yield to engineering. Others only yield to reduced contention. Good scalability distinguishes between them.

---

Every optimization spends correctness, complexity, or money. The only real choice is which one, and how much.

---

## 7. What This Document Leaves Out

- Deployment-specific measurements — throughput, latency, hardware sizing, and load-test results — this document establishes which cost grows with which kind of load, not the exact point at which any deployment would feel it.
- Alternatives to this architecture entirely, such as an event-driven or eventually-consistent redesign — that comparison belongs to a future extension of this repository, not to a document about scaling the architecture as it stands.
- Any claim that these costs have already been measured against a production system — they are described as the costs this architecture's *design* implies, not as reported incidents.

---

## 8. Where to Go Next

This document explained what scaling this system actually costs. The next document asks what, in hindsight, deserved a different answer.

- Continue to [`08-lessons-learned.md`](08-lessons-learned.md) for which of these costs this repository would pay differently on a second attempt.
- Revisit [Architecture Trade-offs](06-architecture-trade-offs.md) for the trade-offs this document showed under load.
- Revisit [Design Decisions](05-design-decisions.md) for the original reasoning behind each guarantee named here.
