# Lessons Learned

[← Back to README](../README.md) · [← Previous: Scalability](07-scalability.md)

**Guiding question: what would a second attempt do differently?**

Every engineering decision has an expiration condition, whether or not anyone writes it down. It was made against a specific set of assumptions — about scale, about who'd be using the system, about how often something unlikely would actually happen — and the decision remains correct for exactly as long as those assumptions hold, not a moment longer. Good engineers don't try to make decisions that never need revisiting. They make the expiration condition explicit, then actually revisit the decision when it arrives, instead of waiting for the condition to force the issue. This document does that revisiting, for the decisions [Design Decisions](05-design-decisions.md) made and [Scalability](07-scalability.md) already put under pressure.

---

## Table of Contents

1. [The Discipline This Document Practices](#1-the-discipline-this-document-practices)
2. [The Concurrency Gap](#2-the-concurrency-gap)
3. [The History Asymmetry](#3-the-history-asymmetry)
4. [The Identity Override](#4-the-identity-override)
5. [The Missing Approval Gate, Reconsidered](#5-the-missing-approval-gate-reconsidered)
6. [What Revisiting Looks Like When It's Done Honestly](#6-what-revisiting-looks-like-when-its-done-honestly)
7. [What This Document Leaves Out](#7-what-this-document-leaves-out)
8. [Where to Go Next](#8-where-to-go-next)

---

## 1. The Discipline This Document Practices

For each decision below, the same five questions, in the same order: what was decided, what assumption justified it, whether that assumption still holds, what's changed if it doesn't, and — honestly — whether the same decision would be made again knowing that. The last question is the one most retrospectives skip, because the honest answer is often still yes. A decision under real pressure isn't a mistake just because its cost eventually became visible; naming that cost early, in [Design Decisions](05-design-decisions.md) and [Architecture Trade-offs](06-architecture-trade-offs.md), was the entire point of writing it down as a trade-off rather than presenting it as free.

---

## 2. The Concurrency Gap

**Decision.** [ADR-002](05-design-decisions.md#adr-002--application-layer-financial-invariant-enforcement) checks the financial invariant synchronously in application code, without row-level locking, accepting a narrow concurrency gap under simultaneous submissions against the same purchase.

**Original assumption.** Two invoices arriving against the same purchase within the same short window would stay rare enough that the exposure was acceptable without the added complexity of locking from day one.

**What still holds.** For the volume this system was designed around, that assumption is still reasonable — most purchases don't attract concurrent invoice submissions often enough for the gap to matter in practice.

**What changed.** Nothing has changed in the system. What's changed is how it reads in hindsight: a gap that's acceptable at low contention is easy to forget about precisely because it rarely manifests, which is a worse property than a gap that fails loudly and often. A rare failure is a failure nobody builds intuition for.

**Would I make it again?** Yes, on the core decision — application-layer enforcement is still right, for the reasons ADR-002 gives. But knowing what's known now, the locking strategy from [Scalability, Section 4](07-scalability.md#4-paying-less-for-the-financial-invariant) belongs in the same release as the invariant check itself, not as a follow-up deferred until contention is observed. A rare bug is still a bug; deferring its fix because it's rare is a different decision than deferring it because it's unimportant, and this repository would rather have made that distinction explicit the first time.

---

## 3. The History Asymmetry

**Decision.** [ADR-004](05-design-decisions.md#adr-004--reference-data-gets-history-transactional-data-doesnt) gives reference data a full change history and gives transactional records none.

**Original assumption.** Transactional records are edited by the person who owns them, disputes about "what did this used to say" would be rare, and the storage and code cost of history everywhere wasn't justified by that rarity.

**What still holds.** Most of it. Reference data is still the more dangerous thing to change silently, because it's depended on by many records at once, and that reasoning hasn't weakened.

**What changed.** The assumption that transactional-record disputes would stay rare was never actually tested against real usage patterns — it was a reasonable guess at design time, not a measured fact, which is a different kind of claim than the rest of this system usually makes.

**Would I make it again?** Yes, but with a narrower initial scope than "no history at all" — extending the same snapshot-before-update pattern already proven on reference data to just the highest-value fields on a purchase (the committed amount, specifically) would have cost little and closed the most consequential version of this gap immediately, rather than waiting for [Design Decisions](05-design-decisions.md#adr-004--reference-data-gets-history-transactional-data-doesnt)'s "when this decision stops scaling" condition to actually trigger before responding to it.

---

## 4. The Identity Override

**Decision.** [ADR-006](05-design-decisions.md#adr-006--identity-based-visibility-overrides) grants cross-organizational visibility to specific named individuals through an explicit list, rather than a formally modeled role.

**Original assumption.** The number of people needing this visibility would stay small and stable enough that a formal role's maintenance overhead wasn't worth building.

**What still holds.** For exactly as many people as the list currently names, the shortcut is still simple to audit and reason about, precisely as ADR-006 predicted.

**What changed.** Nothing about the list has grown yet — but [Architecture Trade-offs, Section 6](06-architecture-trade-offs.md#6-identity-overrides-simplicity-now-vs-scale-later) already named this as the one trade-off whose bill hasn't come due, and "hasn't come due yet" is doing a lot of quiet work in that sentence: it's a condition, but not one anyone is scheduled to check.

**Would I make it again?** This is the one decision whose implementation I'd change, even though I'd keep the underlying decision itself. Nothing in [Design Decisions](05-design-decisions.md), [Architecture Trade-offs](06-architecture-trade-offs.md), or [Scalability](07-scalability.md) shows the list has actually grown past where the shortcut stops being appropriate — the assumption that justified ADR-006 hasn't expired, it just hasn't been checked. That's the actual gap: not that the shortcut was the wrong call, but that "acceptable while the list stays small" was never given a date, a threshold, or an owner responsible for asking the question. An open-ended condition is, in practice, a condition nobody revisits until something forces the issue — usually at a worse time than a scheduled check would have chosen. The fix isn't replacing the shortcut with a formal role today. It's converting an open-ended shortcut into a scheduled one: a specific trigger — a headcount, a date, a review tied to the next organizational change — after which someone is obligated to ask whether ADR-006 still holds, rather than assuming it does because nothing has broken yet.

---

## 5. The Missing Approval Gate, Reconsidered

**Decision.** [ADR-009](05-design-decisions.md#adr-009--no-multi-level-approval-gate) gives this system no human sign-off step at all.

**Original assumption.** This system's risk is arithmetic consistency, not authorization to spend — a different problem than the one an approval chain solves, and not one this system needed to also solve.

**What still holds.** All of it. Nothing encountered while writing [Business Workflows](03-business-workflows.md) or [Security Model](04-security-model.md) suggested this system's actual risk had shifted toward "who is entitled to authorize this" — the arithmetic risk ADR-009 was built to address remains the dominant one.

**What changed.** Nothing.

**Would I make it again?** Yes, without qualification. This is included deliberately alongside the three decisions above, which each earned a real revision, to make a point this document would otherwise only make implicitly: not every honestly-examined decision needs to change on a second look. The discipline in Section 1 isn't a search for regrets. It's a check, and sometimes a check confirms the original answer.

---

## 6. What Revisiting Looks Like When It's Done Honestly

Look closely at the shape of Sections 2 through 4, and they're not three different corrections — they're the same correction, applied three times. A locking strategy shipped alongside the check it protects, instead of deferred until contention is observed. A narrower history extended immediately, instead of waiting for a documented condition to trigger on its own. A shortcut given a scheduled trigger for re-examination, instead of an open-ended "while N stays small." None of the three central decisions changed. What changed, in every case, is that an implicit, unmonitored expiration condition became an explicit, scheduled one. Section 5 needed no correction at all, for the same underlying reason the other three needed the same one: its assumption was checked, honestly, and found to still hold — which is what a genuine review looks like when a decision passes it, not just when a decision fails it.

---

Every engineering decision has an expiration condition. Good engineers make that condition explicit, assign someone to watch it, then revisit the decision when it arrives.

---

## 7. What This Document Leaves Out

- New decisions not already named in [Design Decisions](05-design-decisions.md) — this document revisits, it doesn't introduce.
- A generic list of best practices or process improvements unrelated to a specific assumption made somewhere in this repository.
- Any claim that these four decisions are the only ones worth revisiting — they're the four where [Scalability](07-scalability.md) or [Architecture Trade-offs](06-architecture-trade-offs.md) already surfaced enough pressure to make the check worth doing in public.

---

## 8. Where to Go Next

This document closes the argument that began in [Architecture Trade-offs](06-architecture-trade-offs.md): what a guarantee costs, what it costs under load, and which of those costs were worth paying in hindsight. What's left is reference material built on top of everything established so far.

- Continue to [`10-engineering-patterns.md`](10-engineering-patterns.md) for the recurring patterns underneath these decisions, named independently of this specific system.
- Continue to [`09-interview-guide.md`](09-interview-guide.md) to practice discussing these decisions, including the one this document changed its mind about.
- Continue to [`11-system-evolution.md`](11-system-evolution.md) for how to read these same signals — an assumption, a cost, a revisit — in a codebase you didn't write.
