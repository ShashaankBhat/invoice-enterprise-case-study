# Why Authorization Needs Two Different Questions

[← Back to README](../README.md) · [← Articles index](README.md)

Most authorization bugs aren't caused by a missing role check. They're caused by a role check that was working exactly as designed — and was never the check that actually needed to run.

Here's the pattern, and it's common enough that it probably exists somewhere in a system you've worked on: a user with the "Manager" role can view expense reports. The role check confirms they're a Manager. The report loads. Nobody asks the second question — *is this specific manager entitled to see this specific report* — because the first question already returned yes, and it felt like enough.

It wasn't enough. It was never going to be enough, and understanding why requires separating two ideas that get casually merged into one word, "authorization," far too often.

## Eligibility Is Not Entitlement

A role answers a question about a *category*: can a user of this kind perform this kind of action, in general, on this kind of resource? That's a useful, cheap, coarse-grained check, and it's the right first line of defense — filtering out users who have no business anywhere near a feature, before spending any more effort on them.

But a role, by construction, knows nothing about any *specific instance* of the resource it's guarding. It was assigned before the specific record existed and will still apply after that record is gone. Asking a role "can this user see this specific report" is asking it a question it was never built to answer — the honest answer from a role alone is always "I don't know, I only track categories."

The second question — does *this* user have a legitimate relationship to *this* record — needs a different kind of check entirely, one that loads the specific record and evaluates the requester's actual relationship to it: did they create it, does it belong to their team, were they explicitly granted access to it. Call the first question **Eligibility** and the second **Entitlement**. They are not two strengths of the same check. They're answers to two different questions, and a system that only asks the first one has a structural gap, not a bug waiting to be found — a gap that exists by construction, the moment the second check was never written.

## Why the Gap Is So Easy to Miss

The reason this mistake is so common isn't carelessness. It's that a missing Entitlement check produces no symptom. The request succeeds. The page renders. Nothing crashes, nothing logs an error, nothing looks different from a legitimate request — except that it returned data to someone who shouldn't have received it. A missing Eligibility check tends to get caught quickly, because it usually breaks something visible: a user reaching a feature that visibly doesn't apply to them, in a UI that wasn't built to show it to them. A missing Entitlement check hides in plain sight, inside a feature that's rendering correctly, for the wrong person.

This is also why the two checks need to be genuinely, architecturally distinct — not just conceptually distinct in a design document, but implemented in a way that makes it structurally awkward to skip the second one. The moment a codebase starts treating Entitlement as an optional strengthening of Eligibility, rather than a separate, mandatory step, some code path — usually a read, usually one that "felt like it was just displaying data" — will end up skipping it, and nothing about how the system runs will reveal that it happened.

## What This Costs, and Why It's Worth Paying

Running two checks instead of one is more code, more to test, and a small amount of latency on every authorized request — the price of Eligibility being cheap enough to run first and eliminate most illegitimate requests before anything expensive happens, and Entitlement being precise enough to close the gap the first check structurally can't. That's a fair trade for almost any system handling records with an owner, a team, or a scope narrower than "every user with this role."

A worked example of this two-layer model, including a concrete authorization flow and the specific ways the two checks can drift apart in practice, is developed in full in [Security Model](../docs/04-security-model.md), using an enterprise invoice-processing system as the running example.

The next time you review an authorization check, ask it directly: is this answering "can a user like this reach a feature like this," or "can *this* user act on *this* record"? If the code only ever asks the first question, it has a gap — not a maybe, a certainty, waiting for someone to notice the record it exposed.

---

*This essay generalizes an idea developed in full, with a concrete authorization model, in [Security Model](../docs/04-security-model.md).*
