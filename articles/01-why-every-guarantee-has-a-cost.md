# Why Every Guarantee Has a Cost

[← Back to README](../README.md) · [← Articles index](README.md)

Every system makes promises. Some are explicit — "this endpoint returns within 200 milliseconds." Most are implicit, and far more consequential — "two people editing the same record won't silently overwrite each other," "a deleted item can be recovered," "the total on this invoice always matches the sum of its line items." These implicit promises are the ones that determine whether a system is trustworthy, and they're also the ones engineers are most likely to forget they're paying for.

Call a promise like this a **guarantee**: a property the system commits to keeping true, no matter which code path a request happens to take to get there. And here is the uncomfortable fact underneath every guarantee ever kept: it cost something. Not metaphorically — literally, in one of a small number of currencies. Complexity. Coupling. Latency. Storage. The question worth asking about any guarantee isn't whether it's free. It never is. The question is which currency it's paid in, and whether that currency is one you can afford.

## The Three Currencies

Almost every guarantee-preserving decision spends in one of three ways.

**It spends coupling to avoid duplication.** If a rule is enforced in exactly one place, every caller of that rule now depends on it — a real cost, since a single point of failure is also a single point of dependency. The alternative, letting every caller enforce the rule independently, avoids that dependency but introduces a much worse problem: two copies of the same logic will eventually disagree, because someone will fix one and forget the other. Most systems that claim to have "no single point of failure" in their business logic have actually just distributed their inconsistency instead of eliminating it.

**It spends computation to avoid drift.** A value that's recomputed from source data on every read can never become stale, because there's no cached copy sitting between the read and the truth. That freshness isn't free — recomputing costs CPU time, every single time, instead of once at write time. Caching the value instead is cheaper per read and has a much sneakier cost: the moment some write path forgets to invalidate the cache, the value quietly stops being true, and nothing in the system will tell you.

**It spends latency to avoid silent inconsistency.** A rule that must hold across multiple related records — "these line items may never sum to more than this total," "only one person can hold this seat" — usually needs some form of locking or serialization to check correctly under concurrent access. That locking costs time: requests wait for each other instead of running fully in parallel. Skipping the lock buys speed and creates a race condition that will, eventually, let two things both be true that can't both be true.

None of these trades has a universally correct answer. A system with low write concurrency can get away with a much cheaper version of the third currency than a system processing thousands of concurrent transactions per second. The mistake isn't picking one side of the trade. The mistake is not knowing you made a choice.

## The Sign of a System That Knows What It's Paying For

An enterprise invoice-processing system offers a clean illustration, precisely because its central guarantee is easy to state: the invoices raised against a purchase order can never, in total, exceed what that purchase order committed to. Enforcing that guarantee synchronously — checking the running total before accepting a new invoice, inside the same transaction as the write — spends latency and a certain amount of implementation complexity to buy an invariant that's never observably false, not even for a moment. A cheaper alternative exists: accept the invoice, and reconcile any overage later with a background job. That alternative is faster to write and faster to run. It also means the system can be wrong, visibly, for however long "later" takes — a cost that looks small in a design document and looks very different the first time a vendor is paid twice against a commitment.

Neither choice is inherently right. What separates a mature system from an immature one is whether the choice was made on purpose, and whether the cost it incurred is written down somewhere a future engineer will actually find it, rather than discovered the hard way during an incident review.

## The Practical Version of This Idea

The next time you're reviewing a design — your own, or someone else's — try replacing "is this a good approach?" with "what is this approach spending, and where?" The second question is harder to answer with a shrug. It forces a concrete claim: this spends coupling, this spends computation, this spends latency — and it forces the follow-up question that actually matters: is that a currency this system can afford, at its current and near-future scale?

Good architecture isn't the absence of cost. It's cost paid on purpose, in a currency the system can afford, by someone who wrote down which currency they chose.

---

*This essay generalizes an idea developed in full, with concrete examples from an enterprise invoice-processing system, in [Architecture Trade-offs](../docs/06-architecture-trade-offs.md).*
