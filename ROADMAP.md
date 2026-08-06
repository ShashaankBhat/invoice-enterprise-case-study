# Roadmap

[← Back to README](README.md)

This roadmap is written the same way the rest of this repository is: in terms of guarantees, trade-offs, and expiration conditions, not a plain feature checklist. Each planned addition below states what it would teach, which existing guarantee or trade-off it extends, and — where relevant — its own expiration condition, so this document doesn't quietly become exactly the kind of open-ended intention [`08-lessons-learned.md`](docs/08-lessons-learned.md) argues against.

---

## Planned Additions

### An Event-Driven Variant

**What it would teach.** How the guarantees this repository already documents — the financial invariant from ADR-002, computed state from ADR-003 — would need to be re-argued, not just re-implemented, under an architecture where writes are asynchronous and consistency is eventual rather than synchronous. The interesting question isn't "how would this be built with events instead," it's "which of this repository's guarantees would have to become weaker, and which could stay exactly as strong."

**What it extends.** [`06-architecture-trade-offs.md`](docs/06-architecture-trade-offs.md)'s consistency-vs-performance tension, taken to its logical extreme.

### A CQRS Discussion

**What it would teach.** This system currently mixes its write model (invariant enforcement) and its read model (dashboard aggregation) inside the same layered architecture. A dedicated discussion of separating them — a distinct read-optimized model fed by the same authoritative writes — would extend [Scalability, Section 2](docs/07-scalability.md#2-computed-state-at-scale) with a concrete alternative to "cache the computed result," rather than just naming caching as the only lever.

**What it extends.** [`07-scalability.md`](docs/07-scalability.md), specifically the cost of live aggregation under load.

### Interactive Versions of the State and Sequence Diagrams

**What it would teach.** Nothing new conceptually — this is a presentation improvement, not a new guarantee, trade-off, or lesson. Included here for completeness, not because it belongs alongside the items above in intellectual weight.

### A Minimal, Combined Reference Implementation

**What it would teach.** The three existing reference implementations each demonstrate one pattern in isolation. A single, minimal example combining the invariant validator and the status calculator — showing how they compose in one small system rather than three separate files — would demonstrate that these patterns aren't just individually correct, they're designed to work together.

**What it extends.** [`reference-implementations/`](reference-implementations/README.md), directly.

---

## Expiration Condition for This Roadmap Itself

This document should be reviewed whenever a new document is added to `docs/` — at that point, check whether any item above has been superseded by what the new chapter already covers, and remove it rather than letting a stale planned addition sit alongside content that already exists. An open-ended roadmap that never gets pruned is exactly the kind of unmonitored expiration condition this repository argues against everywhere else.

---

## How to Propose an Addition

See [`CONTRIBUTING.md`](CONTRIBUTING.md). In short: a proposed addition should be able to answer the same two questions every item above answers — what would it teach, and which existing guarantee or trade-off does it extend — before it's added to this list.
