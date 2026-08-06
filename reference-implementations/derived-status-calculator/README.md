# Reference Implementation: Derived Status Calculator

[← Back to reference implementations index](../README.md) · [Owning document: ADR-003](../../docs/05-design-decisions.md#adr-003--computed-status-instead-of-a-persisted-status-field)

## Purpose

Demonstrates the Computed State pattern: deriving a record's status from its underlying data on every read, rather than storing a separate status field that every write path would need to remember to keep in sync.

## The Problem This Solves

A stored `status` field requires every code path that could affect it to remember to update it — including a purely time-driven transition like "becomes overdue," which has no natural write path to hang an update off of. A computed status can't drift, because there's no separate fact to drift from.

## What This Sample Shows

Two independent status computations, deliberately different in shape:

- **`purchaseStatus`** is **relational** — it depends on every invoice tied to the purchase, not on the purchase record's own fields.
- **`invoiceStatus`** is **local** — it depends only on the invoice's own fields and the current date.

Scenario 3 is the important one to read closely: the invoice's status changes from `PENDING` to `OVERDUE` between two calls with no write operation happening in between — only the `today` parameter changes. That's the entire point of computing instead of storing: a purely time-driven transition requires no scheduled job, no update, nothing. It's just true the next time someone asks.

## How to Run

```bash
javac DerivedStatusCalculator.java
java DerivedStatusCalculator
```

## What This Is Not

This is not an excerpt from the real system described in this repository. It's a small, original, purpose-written sample built to demonstrate one pattern clearly — no persistence, no framework, no error handling beyond what the pattern itself requires.

## Related Documents

- [`docs/05-design-decisions.md`](../../docs/05-design-decisions.md), [ADR-003](../../docs/05-design-decisions.md#adr-003--computed-status-instead-of-a-persisted-status-field) — the full decision this sample illustrates.
- [`diagrams/state-machine.md`](../../diagrams/state-machine.md) — the same two status computations, shown as state diagrams.
- [`articles/02-computed-state-vs-stored-state.md`](../../articles/02-computed-state-vs-stored-state.md) — the general engineering idea, independent of this specific system.
