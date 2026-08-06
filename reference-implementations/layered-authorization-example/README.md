# Reference Implementation: Layered Authorization Example

[← Back to reference implementations index](../README.md) · [Owning document: ADR-005](../../docs/05-design-decisions.md#adr-005--two-layer-authorization)

## Purpose

Demonstrates the Coarse-Then-Fine Authorization pattern: a cheap, role-based Eligibility check, followed by a precise, record-level Entitlement check — and, specifically, why the second check can't be replaced by a more detailed version of the first.

## The Problem This Solves

Two Finance Users hold the exact same role. A role-based check alone cannot distinguish between them with respect to a specific record — both are equally "eligible" to use the feature. Only a check that loads the specific record and asks about *this user's* relationship to *it* can tell them apart.

## What This Sample Shows

`checkEligibility` never loads a `PurchaseRecord` — it can't, because it doesn't need one, which is exactly what makes it cheap. `checkEntitlement` only runs after Eligibility has already passed, and only against a record already in hand. Scenario 2 is the one to read carefully: two users with the identical role, one denied — not because of anything about their role, but because of their relationship (or lack of one) to the specific record. Scenario 4 makes the risk concrete: it shows, directly, what `checkEligibility` alone would have returned for the denied user — `ALLOWED` — to make vivid exactly what a missing second check would have silently let through.

## How to Run

```bash
javac LayeredAuthorizationExample.java
java LayeredAuthorizationExample
```

## What This Is Not

This is not an excerpt from the real system described in this repository, and it is not a complete authorization framework — there's no session handling, no route wiring, no persistence. It's a small, original, purpose-written sample built to demonstrate exactly one distinction: Eligibility is not Entitlement.

## Related Documents

- [`docs/04-security-model.md`](../../docs/04-security-model.md) — the full argument this sample illustrates.
- [`docs/05-design-decisions.md`](../../docs/05-design-decisions.md), [ADR-005](../../docs/05-design-decisions.md#adr-005--two-layer-authorization) — the decision behind the two-check model.
- [`diagrams/security-flow.md`](../../diagrams/security-flow.md) — this same flow, shown as a diagram.
- [`articles/03-why-authorization-needs-two-different-questions.md`](../../articles/03-why-authorization-needs-two-different-questions.md) — the general engineering idea, independent of this specific system.
