# Interview Guide

[← Back to README](../README.md)

This document is different from the rest of `docs/`. Every other chapter builds an argument; this one is a practical companion for using that argument out loud, under time pressure, in a room where the interviewer is judging how you think, not just what you know. It assumes you've read at least [`05-design-decisions.md`](05-design-decisions.md), [`06-architecture-trade-offs.md`](06-architecture-trade-offs.md), and [`08-lessons-learned.md`](08-lessons-learned.md) — this guide indexes into them, it doesn't re-argue them.

---

## Table of Contents

1. [How to Use This Guide](#1-how-to-use-this-guide)
2. [Explaining the System in 60 Seconds](#2-explaining-the-system-in-60-seconds)
3. [Explaining It in 5 Minutes](#3-explaining-it-in-5-minutes)
4. [Likely Architecture Questions](#4-likely-architecture-questions)
5. [Likely Design-Decision Questions](#5-likely-design-decision-questions)
6. [Trade-off Questions](#6-trade-off-questions)
7. [Scalability Questions](#7-scalability-questions)
8. [Behavioral Questions Using This Project](#8-behavioral-questions-using-this-project)
9. [Common Weak Answers vs. Stronger Answers](#9-common-weak-answers-vs-stronger-answers)
10. [Where to Review Before an Interview](#10-where-to-review-before-an-interview)

---

## 1. How to Use This Guide

Spend ten minutes before an interview reviewing Sections 2 and 3 out loud, not just reading them — the 60-second and 5-minute versions are meant to be *spoken*, and they read differently in your own voice than on the page. Then skim Sections 4 through 7 for the specific question categories most relevant to the role, and read Section 9 once, carefully — it's the highest-leverage section in this document, because the gap between a weak and a strong answer is rarely knowledge, it's structure.

---

## 2. Explaining the System in 60 Seconds

"It's an enterprise system for tracking purchase commitments against vendors and reconciling them against the invoices raised over time. The interesting engineering problem isn't the CRUD — it's that no combination of invoices can be allowed to exceed what a purchase committed to, checked automatically, without a human having to verify the arithmetic by hand. Everything about how it's built — a layered architecture, computed rather than stored status, two-tier authorization — exists in service of that one guarantee staying true no matter how the system is used."

That's the whole pitch: financial integrity as the organizing idea, not invoices as the subject matter.

---

## 3. Explaining It in 5 Minutes

Extend the 60-second version with, roughly in this order:

1. **The business problem** ([`00-overview.md`](00-overview.md)) — why manual tracking fails predictably at scale, and what a system like this actually guarantees that a spreadsheet can't.
2. **The core invariant** ([`03-business-workflows.md`](03-business-workflows.md)) — the running-total check, walked through with a concrete (illustrative) example.
3. **Why it's enforced where it is** ([ADR-002](05-design-decisions.md#adr-002--application-layer-financial-invariant-enforcement)) — application layer over a database constraint, and the one honest trade-off that decision carries: a concurrency gap, named rather than hidden.
4. **One structural idea** — pick either Computed State ([ADR-003](05-design-decisions.md#adr-003--computed-status-instead-of-a-persisted-status-field)) or Two-Layer Authorization ([`04-security-model.md`](04-security-model.md)), whichever the conversation is drifting toward.
5. **One honest lesson** ([`08-lessons-learned.md`](08-lessons-learned.md)) — the identity-override decision revisited: not "I'd do it differently," but "I'd give it an explicit expiration condition, because it didn't have one."

Five minutes is enough to show the shape of the whole repository without reciting any of it.

---

## 4. Likely Architecture Questions

- *"Why a layered, server-rendered architecture instead of an API plus a frontend?"* → [ADR-001](05-design-decisions.md#adr-001--layered-server-rendered-architecture): the problem's difficulty is correctness under concurrent writes, not interactivity — don't build for complexity the problem doesn't have.
- *"Where do transaction boundaries live, and why there?"* → [System Architecture, Section 5](02-system-architecture.md#5-transaction-boundaries): at the service layer, because it's the only layer with enough business context to know where an operation actually starts and ends.
- *"What would you say is this system's single most important architectural property?"* → One-directional dependency: nothing outside the module needs to know it exists to function. [System Architecture, Section 6](02-system-architecture.md#6-dependency-direction).

---

## 5. Likely Design-Decision Questions

- *"Walk me through one ADR in full — context, alternatives, trade-off."* → Use [ADR-002](05-design-decisions.md#adr-002--application-layer-financial-invariant-enforcement) if you only prepare one; it's the richest, and its trade-off (the concurrency gap) is the most defensible kind of honesty to show an interviewer.
- *"Why does some data get a change history and some doesn't?"* → [ADR-004](05-design-decisions.md#adr-004--reference-data-gets-history-transactional-data-doesnt): reference data is shared by many dependents and edited rarely; transactional data is owned by its creator and edited often. Different risk profiles, deliberately different treatment.
- *"If you had to remove one ADR's guarantee to hit a deadline, which one, and why?"* → There's no rehearsed answer here — this is a prompt to reason live using [Architecture Trade-offs](06-architecture-trade-offs.md)'s cost framing: name what each guarantee costs, and argue from there rather than guessing at what the interviewer wants to hear.

---

## 6. Trade-off Questions

- *"Every guarantee has a cost — give me one from this system and both sides of it."* → Any of the six from [Architecture Trade-offs](06-architecture-trade-offs.md) works; know one well enough to state the cost in one sentence, not a paragraph.
- *"What's the difference between a trade-off and a mistake?"* → A trade-off is a cost paid deliberately and visibly for a benefit that's worth it; [Lessons Learned](08-lessons-learned.md) is built entirely around checking, later, whether that's still true — a mistake is a cost nobody decided to pay.
- *"Name an engineering tension that generalizes beyond this system."* → Computed vs. persisted state ([Architecture Trade-offs, Section 3](06-architecture-trade-offs.md#3-computed-state-correctness-vs-computation)) is the most portable one to talk about — it applies to almost any system with a derived value.

---

## 7. Scalability Questions

- *"What breaks first as this system grows?"* → Live dashboard aggregation — its cost scales with data volume, not with how often it's viewed. [Scalability, Section 2](07-scalability.md#2-computed-state-at-scale).
- *"How would you scale the authorization check without weakening it?"* → Index the Entitlement lookup, cache it narrowly by record with a short expiry — never widen the cache to role-level, which stops being the same check. [Scalability, Section 3](07-scalability.md#3-keeping-entitlement-cheap).
- *"Is every scaling problem here solvable with more engineering?"* → No — say so directly. The concurrency gap only shrinks with less contention, not with cleverness. [Scalability, Section 6](07-scalability.md#6-the-costs-scaling-cant-remove) makes exactly this distinction, and naming it unprompted is a strong signal.

---

## 8. Behavioral Questions Using This Project

- *"Tell me about a decision you'd make differently."* → Use [Lessons Learned, Section 4](08-lessons-learned.md#4-the-identity-override) directly — and use the corrected framing, not the discarded one: the decision was right, what was missing was a scheduled trigger to revisit it. This is a stronger story than "I was wrong," because it's a lesson about process, not just an admission.
- *"Tell me about a decision you'd defend even under pushback."* → [Lessons Learned, Section 5](08-lessons-learned.md#5-the-missing-approval-gate-reconsidered) — no approval gate. Explain *why* it's a different problem than the one an approval chain solves, not just that you'd keep it.
- *"How do you decide when a shortcut is acceptable?"* → Every shortcut needs a stated scope and an expiration condition — a date, a threshold, an owner. [Lessons Learned, Section 6](08-lessons-learned.md#6-what-revisiting-looks-like-when-its-done-honestly) is the concrete example to cite.

---

## 9. Common Weak Answers vs. Stronger Answers

| Weak | Stronger |
|---|---|
| "We used a layered architecture because it's a common best practice." | "We used it because the problem's difficulty is correctness under concurrent writes, not interactivity — a decoupled frontend would have added a second contract to maintain for a kind of complexity this problem doesn't have." |
| "The invariant check makes sure invoices don't exceed the purchase." | "The invariant is checked synchronously at write time, in the service layer, specifically so it can never be bypassed — and it has a known concurrency gap under simultaneous writes, which is an accepted, documented trade-off, not an oversight." |
| "We'd fix the identity-override thing by building a proper role." | "The decision was right at the scale it was made for — what it needed was an explicit trigger to revisit it, not a different implementation from day one." |
| "The system doesn't have an approval workflow." | "It doesn't, because its risk is arithmetic consistency, not authorization to spend — those are different problems, and building an approval chain wouldn't have addressed the one this system actually has." |
| "We'd add caching to make the dashboard faster." | "We'd cache the *result* of the live computation for a bounded window, invalidated on the writes that affect it — never replace it with a separately-maintained summary, which is the exact failure mode the live-computation decision was made to avoid." |

The pattern across every "stronger" answer: name the guarantee, name its cost, and show the cost was chosen, not discovered after the fact.

---

## 10. Where to Review Before an Interview

- Rehearsing the 5-minute version: [`00-overview.md`](00-overview.md), [`03-business-workflows.md`](03-business-workflows.md), [`05-design-decisions.md`](05-design-decisions.md).
- Architecture and trade-off depth: [`02-system-architecture.md`](02-system-architecture.md), [`06-architecture-trade-offs.md`](06-architecture-trade-offs.md).
- Scalability specifics: [`07-scalability.md`](07-scalability.md).
- Behavioral material: [`08-lessons-learned.md`](08-lessons-learned.md).
- Vocabulary check before any of the above: the [Glossary](glossary.md).
