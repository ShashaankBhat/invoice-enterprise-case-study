# Reference Implementation: Financial Invariant Validator

[← Back to reference implementations index](../README.md) · [Owning document: ADR-002](../../docs/05-design-decisions.md#adr-002--application-layer-financial-invariant-enforcement)

## Purpose

Demonstrates the Aggregate Invariant pattern: enforcing that a set of related child records can never, together, exceed a value fixed on their parent — checked synchronously, at the moment a new child is added, not after the fact.

## The Problem This Solves

No single invoice, considered on its own, can violate this invariant — the same $1 invoice is either accepted or rejected depending entirely on how much of the parent's commitment is already claimed by other invoices. Validating a record in isolation can never catch this class of rule; the check has to look sideways, at siblings, not just inward.

## What This Sample Shows

`tryAddInvoice` sums every invoice already recorded, adds the candidate, and rejects the addition if the total would exceed the commitment — before the invoice is added to the list, not after. Run it, and watch the third scenario: a $1 invoice is rejected against a purchase with zero headroom remaining, immediately after the code comments explain that the same $1 invoice would be accepted against a purchase with room left. The rejection is never about the number. It's about the relationship between that number and everything already recorded against the same parent.

The code comments at the top of the file are as important as the code itself here — they name, explicitly, the concurrency gap this single-threaded sample doesn't demonstrate: two simultaneous calls to `tryAddInvoice` against the same commitment can each read a total that doesn't yet include the other, and both can pass. A production implementation of this pattern needs a lock or an equivalent concurrency guard around the read-then-write; this sample deliberately isolates the invariant check itself from that separate, real concern, rather than conflating the two.

## How to Run

```bash
javac FinancialInvariantValidator.java
java FinancialInvariantValidator
```

## What This Is Not

This is not an excerpt from the real system described in this repository, and it is not thread-safe — see the concurrency note above. It's a small, original, purpose-written sample built to demonstrate the invariant check itself in isolation.

## Related Documents

- [`docs/05-design-decisions.md`](../../docs/05-design-decisions.md), [ADR-002](../../docs/05-design-decisions.md#adr-002--application-layer-financial-invariant-enforcement) — the full decision, including the concurrency trade-off named in this sample's comments.
- [`docs/08-lessons-learned.md`](../../docs/08-lessons-learned.md), [Section 2](../../docs/08-lessons-learned.md#2-the-concurrency-gap) — the concurrency gap this sample deliberately doesn't solve.
- [`diagrams/reconciliation-sequence.md`](../../diagrams/reconciliation-sequence.md) — this same check, shown as a sequence over time.
- [`articles/06-why-single-ownership-beats-duplicate-business-rules.md`](../../articles/06-why-single-ownership-beats-duplicate-business-rules.md) — the general engineering idea behind enforcing this rule from exactly one place.
