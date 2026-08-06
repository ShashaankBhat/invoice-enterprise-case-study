# Architecture Trade-offs

[← Back to README](../README.md) · [← Previous: Security Model](04-security-model.md)

**Guiding question: what engineering tensions recur here, independent of how any one of them was resolved?**

Throughout this repository, a **guarantee** means a property the system promises will always remain true, regardless of which execution path reaches it. A guarantee is never free. Every one this repository has described so far — a single owner for the financial invariant, status computed instead of stored, Eligibility checked separately from Entitlement — bought something real and charged something real for it. The engineering question was never *whether* to pay that cost. It was where. This document is a single argument, told six times: name the tension, show where this system chose to pay, and show what that choice would look like if the cost moved somewhere else.

---

## Table of Contents

1. [The One Law Behind Six Examples](#1-the-one-law-behind-six-examples)
2. [Single Ownership: Duplication vs. Coupling](#2-single-ownership-duplication-vs-coupling)
3. [Computed State: Correctness vs. Computation](#3-computed-state-correctness-vs-computation)
4. [Two-Layer Authorization: Precision vs. Cost](#4-two-layer-authorization-precision-vs-cost)
5. [Application-Layer Invariants: Portability vs. Concurrency](#5-application-layer-invariants-portability-vs-concurrency)
6. [Identity Overrides: Simplicity Now vs. Scale Later](#6-identity-overrides-simplicity-now-vs-scale-later)
7. [Live Aggregation: Freshness vs. Performance](#7-live-aggregation-freshness-vs-performance)
8. [Why These Are the Same Tension](#8-why-these-are-the-same-tension)
9. [What This Document Leaves Out](#9-what-this-document-leaves-out)
10. [Where to Go Next](#10-where-to-go-next)

---

## 1. The One Law Behind Six Examples

Every decision in [Design Decisions](05-design-decisions.md) reads, on its surface, like a different kind of choice — one about storage, one about layering, one about a status field. Underneath, they're the same choice made six times: something has to be guaranteed, guaranteeing it costs something, and the only real decision is which line item pays. A system that pretends otherwise — that a guarantee can be added without a corresponding cost appearing somewhere — hasn't avoided the cost. It's just made it invisible, which is worse, because a visible cost can be budgeted for and an invisible one can't.

The six sections that follow are not six unrelated trade-offs. They're six places this one law shows up.

---

## 2. Single Ownership: Duplication vs. Coupling

[ADR-002](05-design-decisions.md#adr-002--application-layer-financial-invariant-enforcement) put the financial invariant in exactly one place. The alternative — checking it wherever it might matter, close to each caller — would avoid a dependency: every call site could decide independently, without needing to route through a shared owner. That independence is exactly what's traded away. A rule enforced in one place can only be inconsistent with itself if that one place has a bug. A rule enforced in several places can be inconsistent between them, and usually will be, eventually, the moment one of them is changed and the others aren't. Single ownership pays in coupling — every caller now depends on the one place that decides — to avoid paying in duplication, which is a debt that compounds silently until two copies of the same rule disagree. Every guarantee with one owner creates a dependency on that owner. That dependency is the price of consistency.

---

## 3. Computed State: Correctness vs. Computation

[ADR-003](05-design-decisions.md#adr-003--computed-status-instead-of-a-persisted-status-field) and [ADR-008](05-design-decisions.md#adr-008--on-demand-dashboard-aggregation) both derive a value instead of storing it. The alternative — persist it, update it on every relevant write — is cheaper to read and more expensive to keep honest: every write path that could affect the value has to remember to update it, including the write paths nobody thought to check for, which is exactly how a persisted value quietly goes stale. Computing it live instead means it can never be stale, at the cost of paying the computation on every single read instead of once per write. This system chose to pay in computation to avoid paying in the much harder currency of correctness maintained by hand. Stored facts must be synchronized. Derived facts must be recomputed. Every system chooses which bill it prefers to pay.

---

## 4. Two-Layer Authorization: Precision vs. Cost

[ADR-005](05-design-decisions.md#adr-005--two-layer-authorization) — [Security Model](04-security-model.md) covers the full reasoning — checks Eligibility cheaply, then Entitlement precisely, only once a specific record justifies the expense. A single, always-precise check would be simpler to reason about, with one code path instead of two — and would pay Entitlement's cost on every request, including the large fraction that were never going to be authorized regardless of which record they named. Two layers pay a small, constant coordination cost — two checks to keep conceptually distinct, forever — to avoid paying a variable, and much larger, per-request cost on traffic that didn't need the expensive check at all. Entitlement is more precise than Eligibility because it knows more. Knowing more always costs more.

---

## 5. Application-Layer Invariants: Portability vs. Concurrency

ADR-002 also made a second trade explicit in its own text, worth generalizing here rather than repeating: enforcing the invariant in application code, instead of as a database constraint, keeps the rule portable, testable, and readable without needing database-specific procedural knowledge — at the cost of a concurrency gap a database-level constraint would have closed for free, at the database's own expense in flexibility. This is the sharpest example in the whole repository of the law in Section 1 refusing to be avoided: this system chose where to pay — portability now, a narrow concurrency risk later — and named that choice rather than pretending the concurrency gap didn't exist. An acknowledged limitation is part of an architecture. An unacknowledged limitation eventually becomes a defect.

---

## 6. Identity Overrides: Simplicity Now vs. Scale Later

[ADR-006](05-design-decisions.md#adr-006--identity-based-visibility-overrides) grants a handful of people expanded visibility through an explicit list instead of a formally modeled role. The list is trivial to build and to audit today, for exactly as many people as it currently names — the entire cost is deferred, not avoided, to the day that list needs to grow or change with any regularity, at which point maintaining it correctly costs more than building the formal role would have cost from the start. This is the only trade-off in this repository whose cost is mostly future maintenance rather than present execution — which makes it worth watching, not worth forgetting.

---

## 7. Live Aggregation: Freshness vs. Performance

[ADR-008](05-design-decisions.md#adr-008--on-demand-dashboard-aggregation) computes dashboard metrics live rather than maintaining a precomputed summary, for the same reason as Section 3: a precomputed summary can drift from the transactional data it's supposed to summarize the moment a write path forgets to update it. Live computation can't drift, because there's nothing separate to fall out of sync — at the cost of paying the full aggregation cost on every dashboard load, growing with the volume of data being aggregated rather than with how often the dashboard is viewed. [`07-scalability.md`](07-scalability.md) covers exactly where that cost becomes the wrong one to keep paying. Fresh data is always computed somewhere. The only question is whether it's computed during writes or during reads.

---

## 8. Why These Are the Same Tension

Line them up: coupling instead of duplication, computation instead of drift, a second check instead of a blind spot, a concurrency risk instead of database lock-in, a deferred cost instead of premature abstraction, aggregation cost instead of staleness. Six different currencies, one recurring choice — pay for a guarantee in a cost that's visible, bounded, and named, or don't pay for it at all and discover the cost later, at a worse time, in a form that's much harder to trace back to the decision that created it.

Good architecture is therefore less about eliminating trade-offs than about choosing trade-offs whose costs stay visible.

---

Every guarantee has a cost. Good architecture chooses where to pay it.

---

## 9. What This Document Leaves Out

- The full reasoning behind any individual decision named here — that's [Design Decisions](05-design-decisions.md)' job; this document only generalizes the tension each one already named.
- Quantified thresholds for when a trade-off's cost becomes unacceptable — [`07-scalability.md`](07-scalability.md) owns the concrete numbers.
- A recommendation for which side of any tension is "correct" in the abstract — there isn't one; the correct side depends entirely on which cost a specific system can afford, which is precisely why this is a tension and not a rule.

---

## 10. Where to Go Next

This document generalized six specific decisions into one recurring law. The next document asks what happens as the costs already named here grow past what this system was originally built to absorb.

- Continue to [`07-scalability.md`](07-scalability.md) for where each of these costs stops being affordable, and what changes first.
- Continue to [`08-lessons-learned.md`](08-lessons-learned.md) for which of these trade-offs, in hindsight, would be worth revisiting.
- Revisit [Design Decisions](05-design-decisions.md) for the full context behind any single trade-off summarized here.
