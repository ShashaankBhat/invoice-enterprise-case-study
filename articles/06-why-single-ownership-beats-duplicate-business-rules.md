# Why Single Ownership Beats Duplicate Business Rules

[← Back to README](../README.md) · [← Articles index](README.md)

There's a specific moment that happens in almost every growing codebase, and it usually feels harmless when it happens: a validation rule that lives in one service gets copy-pasted into a second service, because the second service needs "basically the same check" and pulling in a shared dependency felt like overkill for something this small. Six months later, someone changes the rule in one place, doesn't know the second copy exists, and the system now enforces two different versions of what's supposed to be one business rule — silently, until a customer notices the inconsistency before anyone on the engineering team does.

This is not a story about carelessness. It's a story about a very reasonable-sounding shortcut that has a structural flaw built into it from the moment it's taken.

## Two Copies of a Rule Will Always Drift

The core problem isn't that duplication is more code to maintain, though it is. The core problem is that duplicated logic has no mechanism forcing it to stay synchronized. Two independent implementations of the same rule are, by construction, two independent things — nothing about how software works causes them to change together just because they started out identical. They drift not because anyone makes a mistake, but because the ordinary, correct process of fixing a bug or adjusting a rule in one place has no way of knowing a second, textually different copy of the same logic exists somewhere else in the codebase.

Contrast that with a rule enforced from exactly one place, referenced by every caller that needs it. It can only be wrong in one way: that one place has a bug. It cannot be *inconsistent with itself*, because there's no second version for it to disagree with. This is the entire argument for what's worth naming explicitly as its own pattern — **Single Ownership** — enforcing a rule, a computation, or a piece of business logic from exactly one place in a system, and routing every caller through it rather than letting convenience produce a second copy.

## The Cost Nobody Likes Naming

Single Ownership isn't free, and pretending it is undersells the pattern rather than strengthening it. The cost is coupling: every caller that needs the rule now has a real dependency on the one place that owns it — a shared library, a shared service, a shared module. That dependency is inconvenient in exactly the way the original copy-paste shortcut was trying to avoid: it means a schema change, a language boundary, or a deployment boundary between two parts of a system all become friction the moment they need to share a rule, rather than each side just writing its own quick version and moving on.

That inconvenience is worth naming honestly, because the alternative — accepting duplication to avoid the coupling — doesn't actually avoid a cost. It just defers the cost, moves it later in time, and changes its shape from "friction now, during development" to "an inconsistency discovered in production, after two rules have already disagreed about something a customer noticed." Coupling is a cost paid upfront and visibly. Duplication is a cost paid later, invisibly, usually by someone who didn't make the original decision and has to reverse-engineer why two things that were supposed to be the same thing no longer are.

## Recognizing It Under a Different Name

This pattern shows up in the industry under several names — Single Source of Truth, Single Writer, sometimes Sole Authority — and the specific name matters far less than recognizing the shape wherever it appears, because the shape recurs constantly, well outside anything resembling the original example: a financial calculation performed identically by two different reporting pipelines that eventually disagree; a permission check duplicated between a web client and a backend that eventually diverges after only one of them gets patched; a piece of pricing logic copied into a second service "just for this one feature" that quietly becomes the system's second, competing definition of price.

A concrete instance of this pattern — a financial rule enforced from exactly one place in a live system, with its coupling cost stated plainly rather than glossed over — is worked through in [Architecture Trade-offs, Section 2](../docs/06-architecture-trade-offs.md#2-single-ownership-duplication-vs-coupling) and named as a transferable pattern in [Engineering Patterns](../docs/10-engineering-patterns.md#2-single-ownership).

The next time "I'll just copy this check, it's basically the same rule" sounds like the fast option, remember what it's actually trading away: a small amount of coupling now, in exchange for a guarantee that the rule can only ever be wrong in one place — never wrong in two places, disagreeing with each other, with nobody currently on the team who remembers that the second copy exists.

---

*This essay generalizes a decision made in full, with its coupling cost stated explicitly, in [Design Decisions, ADR-002](../docs/05-design-decisions.md#adr-002--application-layer-financial-invariant-enforcement) and [Architecture Trade-offs, Section 2](../docs/06-architecture-trade-offs.md#2-single-ownership-duplication-vs-coupling).*
