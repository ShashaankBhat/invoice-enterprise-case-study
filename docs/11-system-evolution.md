# System Evolution

[← Back to README](../README.md) · [← Previous: Engineering Patterns](10-engineering-patterns.md)

**Guiding question: how should an engineer read structural signals in any unfamiliar codebase?**

A codebase tells you what it values by where it spends complexity. Every guarantee this repository has described cost something — a second authorization check, a synchronous invariant, a history table some entities have and others don't — and none of those costs were spent by accident. They were spent because something mattered enough to pay for it. This document generalizes that observation into a method: how to read a system you didn't design, tell its deliberate complexity from its incidental complexity, and change it without quietly breaking the guarantees the deliberate complexity was there to protect.

---

## Table of Contents

1. [The Thesis, Stated Plainly](#1-the-thesis-stated-plainly)
2. [Finding the Guarantees](#2-finding-the-guarantees)
3. [Recognizing the Patterns Under Different Names](#3-recognizing-the-patterns-under-different-names)
4. [Telling Intentional Complexity From Accidental Complexity](#4-telling-intentional-complexity-from-accidental-complexity)
5. [Evolving a System Without Breaking Its Guarantees](#5-evolving-a-system-without-breaking-its-guarantees)
6. [Knowing When an Evolution Is Actually Complete](#6-knowing-when-an-evolution-is-actually-complete)
7. [What This Document Leaves Out](#7-what-this-document-leaves-out)
8. [Where to Go Next](#8-where-to-go-next)

---

## 1. The Thesis, Stated Plainly

Nobody writes a second authorization check, a synchronous cross-record validation, or a snapshot-before-update history table by accident — each one is more code than the version without it, and more code is never free, per [Architecture Trade-offs](06-architecture-trade-offs.md). So when a codebase contains it anyway, that expense is a signal, not noise: something was worth protecting badly enough that a previous engineer paid to protect it. Reading a system well starts with taking that signal seriously before deciding whether to touch it.

---

## 2. Finding the Guarantees

A guarantee rarely announces itself in a comment. It shows up as a shape: a code path that every write of a certain kind is forced through, with no shortcut around it. Look for code every execution path is forced through, or values that are repeatedly checked, re-derived, or validated — a mandatory chokepoint and a repeated validation are the two major structural signals, and neither is usually an oversight. Both are the guarantee made visible. In this system, that shape is the financial invariant check that every invoice submission passes through regardless of which entry point reached it; in an unfamiliar system, it's whatever the equivalent chokepoint turns out to be. The question worth asking of any surprising piece of code isn't "why is this here" — that invites a shrug. It's "what would go wrong if this code didn't run," which usually has a real answer, even when nobody left a comment explaining it.

---

## 3. Recognizing the Patterns Under Different Names

Once a guarantee is found, [Engineering Patterns](10-engineering-patterns.md) is the vocabulary for naming *how* it's being protected — but the names won't match. A system that calls its coarse-then-fine authorization check "permission gate, then ownership check" is doing exactly what this repository calls Eligibility and Entitlement; a system with a "read model" and a "write model" kept deliberately separate is doing a close relative of Derive, Don't Duplicate; a system where one service is the only thing allowed to touch a table is practicing Single Ownership under a name nobody bothered to give it. The patterns are the same handful of shapes recurring across the industry, under as many names as there are teams who independently discovered them. Recognizing the shape matters more than matching the vocabulary — a system doesn't need to use this repository's words to be doing this repository's ideas.

---

## 4. Telling Intentional Complexity From Accidental Complexity

Not every strange thing in a codebase is protecting a guarantee. Some of it is just leftover — an abandoned attempt, a rule that used to matter and no longer does, two slightly different implementations of the same idea that never got reconciled. The distinction that matters before touching anything: complexity that's *consistent* — the same check, applied the same way, everywhere it's relevant — is usually intentional, even if nobody can immediately explain why. Complexity that's *inconsistent* — the same kind of check applied in some places and not others, two different-looking implementations of what should be one rule — is a much stronger signal of something accidental, or something that used to be intentional and has since decayed. [Lessons Learned](08-lessons-learned.md) is what disciplined engineers produce instead of letting that decay happen silently: an explicit record of which complexity is still earning its cost and which isn't. In its absence, consistency is the best available evidence for which is which.

---

## 5. Evolving a System Without Breaking Its Guarantees

Once a guarantee is identified, changing the system around it has an ordering: change the implementation before changing the guarantee, and never let the second happen silently as a side effect of the first. Swapping how a value is computed, which layer performs a check, or how a rule is stored can all happen freely as long as the guarantee itself — the property that was true before — is still true afterward. The moment a change would make a previously-guaranteed property no longer reliably true, that's not an implementation change anymore, and treating it like one is how systems lose guarantees nobody decided to give up. If a guarantee genuinely needs to change — the business's tolerance for risk shifted, the assumption behind it expired per [Lessons Learned](08-lessons-learned.md) — the correct move is to state the new guarantee explicitly, the same way [Design Decisions](05-design-decisions.md) states this system's guarantees, not to let the old one erode one convenient shortcut at a time. Implementation is free to evolve. Guarantees are not.

---

## 6. Knowing When an Evolution Is Actually Complete

An evolution is finished when three things are true, not when the code compiles and the tests pass. First, every guarantee that existed before the change either still holds or was deliberately, explicitly replaced with a stated new one — never silently narrowed. Second, the cost of every guarantee still in force remains visible somewhere a future engineer will actually find it, the same discipline [Architecture Trade-offs](06-architecture-trade-offs.md) practiced throughout this repository. Third, anything temporary introduced along the way — a shortcut, an exception list, a stopgap — has an expiration condition attached to it per [Lessons Learned](08-lessons-learned.md), not just an intention to revisit it eventually. A change that passes its tests but leaves any of these three undone isn't finished. It's just not failing yet.

---

Read a codebase by where it spends complexity, not by what it says about itself. Complexity spent protecting something is a value the system holds. Complexity spent nowhere in particular is an accident waiting for someone to either name it or remove it.

---

## 7. What This Document Leaves Out

- A checklist or step-by-step migration procedure — this document is a way of reading and reasoning, not a runbook, and turning it into one would falsely suggest every system's guarantees can be found the same mechanical way.
- New concepts — every idea used here was already introduced and cited from an earlier chapter; this document's only job is to show them working together, on a system other than this one.
- Any claim that this method finds every guarantee in every system — some are genuinely invisible until they're violated once. This document improves the odds of finding them first. It doesn't guarantee it, which would be a strange thing for this particular document to promise.

---

## 8. Where to Go Next

This document closes the conceptual arc that began with [Design Decisions](05-design-decisions.md): why guarantees are made, what they cost, how those costs change, when to revisit them, what to call them, and how to recognize them anywhere. What remains is grounding all of it back in this specific system and making it quick to reference.

- Continue to [`00-overview.md`](00-overview.md) and [`01-business-domain.md`](01-business-domain.md) for the concrete business problem and domain model everything in this repository has been protecting.
- Continue to [`09-interview-guide.md`](09-interview-guide.md) to practice discussing all of it out loud.
- Revisit [Engineering Patterns](10-engineering-patterns.md) for the named vocabulary this document assumed throughout.
