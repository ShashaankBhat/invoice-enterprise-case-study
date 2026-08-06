# Computed State vs. Stored State

[← Back to README](../README.md) · [← Articles index](README.md)

Somewhere in almost every backend system there's a column called `status`. It seems like the most natural thing in the world to add — a value like `pending`, `active`, `completed`, sitting right there on the record, updated whenever something relevant happens. And in a surprising number of systems, that one column becomes a slow, quiet source of bugs that outlives the engineers who added it.

The problem isn't the concept of status. It's the decision to store it as its own fact, separate from the data it's supposed to summarize.

## Two Ways to Answer the Same Question

Suppose an order's status depends on whether it's been paid, shipped, and delivered. There are two fundamentally different ways to make that status available to the rest of the system.

**Store it.** Add a `status` column. Every code path that could affect it — recording a payment, marking a shipment, confirming delivery — updates the column as part of that operation. Reading the status is now a single, cheap column read.

**Compute it.** Don't add the column. Instead, derive the status on demand, every time it's needed, from the underlying facts: is there a payment record, is there a shipment record, is there a delivery confirmation. Reading the status now costs a small computation instead of a column read.

The stored version is faster to read and has a structural weakness the computed version doesn't: it requires every future write path, including ones that don't exist yet, to remember to keep it honest. A payment-refund feature added eighteen months later, by an engineer who's never heard of the status column, can silently leave a refunded order sitting in `paid` forever. Nothing crashes. Nothing errors. The value is just quietly wrong, and it stays wrong until someone notices by accident.

The computed version has no such weakness, for a simple reason: there's no separate fact to forget to update, because there is no separate fact. The status is a question, answered fresh, every time it's asked, from data that has to be correct anyway for other reasons.

## The Transition Nobody Writes a Handler For

There's a category of status change that exposes the stored-value problem especially clearly: transitions driven purely by time. An invoice becomes overdue not because anyone did anything, but because a due date quietly passed while nobody was looking. A stored `status` column has no natural trigger for this — nothing *writes* on the day an invoice becomes overdue, so a system relying on stored status needs a separate scheduled job whose only purpose is walking through records and updating a field to reflect that time moved forward. That job is one more thing to build, one more thing to monitor, and one more way for the stored value to drift if the job fails silently for a week.

A computed status handles this transition automatically, for free, as a side effect of being computed rather than stored: the moment "today" changes, every future computation of that status reflects it, without anyone writing code to make that specific thing happen.

## Where Computing It Costs You

None of this means stored values are a mistake. They're the correct choice whenever the underlying computation is expensive and the value is read far more often than its inputs change — a search index, a materialized report, a cached recommendation score. The trade being made in those cases is explicit: pay for staleness, in exchange for speed.

The mistake is choosing the stored version by default, out of habit, for values whose correctness matters more than their read latency — and discovering the staleness cost only after it's already caused a problem a customer noticed before the engineering team did.

A concrete version of this trade-off, including what it costs at scale and how to cache a computed value safely without quietly turning it back into a stored one, is worked through in full using an enterprise invoice-processing system as the running example in [Design Decisions, ADR-003](../docs/05-design-decisions.md#adr-003--computed-status-instead-of-a-persisted-status-field) and [Scalability, Section 2](../docs/07-scalability.md#2-computed-state-at-scale).

The next time you're about to add a `status` column, ask a smaller question first: is this a fact, or a question the system can always afford to answer live? If it's a question, don't build a fact that has to remember to keep up with the answer.

---

*This essay generalizes a decision made in full, with a concrete numeric example, in [Design Decisions, ADR-003](../docs/05-design-decisions.md#adr-003--computed-status-instead-of-a-persisted-status-field).*
