# Articles

[← Back to README](../README.md)

Seven standalone essays, each built around one engineering idea rather than one chapter of this repository. None of them require having read `docs/` first — each is written to stand on its own, using the enterprise invoice-processing system described in this repository as its concrete illustration, not its subject. If an idea interests you, the linked chapter at the end of each article is where the full argument, with every trade-off, lives.

| Article | The idea | Built from |
|---|---|---|
| [`01-why-every-guarantee-has-a-cost.md`](01-why-every-guarantee-has-a-cost.md) | Every guarantee a system keeps is paid for in coupling, computation, or latency — never for free. | [`06-architecture-trade-offs.md`](../docs/06-architecture-trade-offs.md) |
| [`02-computed-state-vs-stored-state.md`](02-computed-state-vs-stored-state.md) | A stored status field needs every future writer to remember to update it; a computed one can't go stale, because there's nothing to fall out of sync. | [ADR-003](../docs/05-design-decisions.md#adr-003--computed-status-instead-of-a-persisted-status-field), [`07-scalability.md`](../docs/07-scalability.md) |
| [`03-why-authorization-needs-two-different-questions.md`](03-why-authorization-needs-two-different-questions.md) | "Can this role use this feature" and "can this user act on this record" are different questions, and a missing second check produces no symptom at all. | [`04-security-model.md`](../docs/04-security-model.md) |
| [`04-the-difference-between-a-trade-off-and-a-mistake.md`](04-the-difference-between-a-trade-off-and-a-mistake.md) | A trade-off is a decision with its assumption written down; a mistake is the same decision made silently. | [`08-lessons-learned.md`](../docs/08-lessons-learned.md) |
| [`05-reading-an-unfamiliar-codebase-by-its-guarantees.md`](05-reading-an-unfamiliar-codebase-by-its-guarantees.md) | Read a codebase by where it spent extra complexity, not file by file — that's where its guarantees are. | [`11-system-evolution.md`](../docs/11-system-evolution.md) |
| [`06-why-single-ownership-beats-duplicate-business-rules.md`](06-why-single-ownership-beats-duplicate-business-rules.md) | Two copies of the same rule will drift, not because of carelessness, but because nothing forces independent copies to change together. | [ADR-002](../docs/05-design-decisions.md#adr-002--application-layer-financial-invariant-enforcement), [`10-engineering-patterns.md`](../docs/10-engineering-patterns.md) |
| [`07-expiration-conditions-the-missing-piece.md`](07-expiration-conditions-the-missing-piece.md) | "We'll revisit this later" almost never happens without a specific, checkable trigger and a named owner. | [`08-lessons-learned.md`](../docs/08-lessons-learned.md) |

Each article closes with a link to the chapter it was drawn from, for readers who want the full argument, the counterexamples, and the concrete numbers behind the idea.
