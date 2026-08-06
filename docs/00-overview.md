# Overview

[← Back to README](../README.md)

**Guiding question: why do organizations need this class of software at all?**

Every organization above a certain size makes financial commitments that outlive the moment they were made — a purchase agreed to today is still a promise weeks or months later, when the invoices against it finally arrive. Something has to remember that promise accurately for as long as it takes to fulfill, and stop anyone, including the system itself, from quietly losing track of how much of it remains. That's the entire reason this class of software exists. This document establishes the vocabulary, roles, and lifecycle every later chapter assumes — read it first, even though it says the least about *how* any of it works.

---

## Table of Contents

1. [The Business Problem](#1-the-business-problem)
2. [Who Uses the System](#2-who-uses-the-system)
3. [The End-to-End Lifecycle](#3-the-end-to-end-lifecycle)
4. [Core Capabilities](#4-core-capabilities)
5. [What This System Deliberately Does Not Do](#5-what-this-system-deliberately-does-not-do)
6. [What This Document Leaves Out](#6-what-this-document-leaves-out)
7. [Where to Go Next](#7-where-to-go-next)

---

## 1. The Business Problem

A manual process — a spreadsheet, a shared tracker, an email thread someone is supposed to update — can track a purchase commitment for a while. It fails in the same few ways every time, once volume grows past what one careful person can hold in their head: nothing stops the same commitment from being invoiced twice; nothing forces the running total to be checked before the next invoice is accepted; and nothing preserves an honest record of what changed, when, once the person who made the change has moved on to something else.

None of those are technology problems in the sense of needing a faster machine. They're consistency problems — the same fact needs to stay true no matter how many people touch it, in whatever order they happen to touch it. That's what this software is actually for: not spending money, not routing approvals, but making sure the arithmetic behind every commitment stays honest automatically, without depending on any one person remembering to check it by hand.

---

## 2. Who Uses the System

Four categories of user, referenced consistently by every later document:

| Role | What they do |
|---|---|
| **Finance User** | Creates and manages purchases and invoices within their own organizational unit — the primary, day-to-day user of the system. |
| **Finance Administrator** | Everything a Finance User can do, plus administering shared reference data — vendors, expense categories, expense types — that every Finance User's purchases depend on. |
| **System Administrator** | Unrestricted visibility and access across the entire system; the role responsible for the system itself, not for any single organizational unit's purchases. |
| **Cross-organizational reviewer** | A small number of individuals granted visibility across organizational boundaries the standard role hierarchy doesn't cross — [Security Model](04-security-model.md) covers the mechanism and its cost in full. |

A single person can hold more than one of these at once; the system's authorization model, covered fully in [Security Model](04-security-model.md), is built to resolve visibility correctly even when roles overlap.

---

## 3. The End-to-End Lifecycle

A vendor is registered and categorized. A Finance User records a purchase against that vendor — a committed amount, an expense category, an organizational unit, supporting documentation. From there, one or more invoices are submitted against that purchase over time, each one checked the moment it arrives against how much of the commitment remains. The purchase's own status — open, partially invoiced, fully settled — is never a separate fact anyone has to update; it's always derived from its invoices, on demand.

There's no approval step anywhere in this sequence. [Business Workflows](03-business-workflows.md) covers why that's a deliberate scope decision, not an omission — this system's engineering weight sits in guaranteeing the numbers stay consistent, not in orchestrating who signs off on them.

---

## 4. Core Capabilities

- **Structured purchase and invoice recording**, with supporting documentation attached to each.
- **An enforced financial invariant** — the combined value of invoices against a purchase can never exceed what that purchase committed to, checked at the moment each invoice is submitted.
- **Computed, always-current status** for every purchase and invoice, without a separate value to keep in sync.
- **Independently administered reference data** — vendors, expense categories, expense types — each with its own change history.
- **Role-scoped visibility**, including the narrow, deliberate exception for cross-organizational review.
- **A dashboard** aggregating the same transactional data into role-appropriate views, live.

---

## 5. What This System Deliberately Does Not Do

- It does not route a purchase or invoice through any human approval chain. [Business Workflows, Section 5](03-business-workflows.md#5-why-theres-no-approval-gate) explains why that's a different problem than the one this system solves.
- It does not treat its reference data as a place where workflow *behavior* lives — categories and vendors describe things, they don't drive logic, a distinction [Design Decisions](05-design-decisions.md) returns to directly.
- It is not a general-purpose financial or workflow platform. It solves one process — committing to spend, then reconciling what's claimed against it — precisely, rather than offering configurable workflows for arbitrary processes.
- It does not attempt real-time collaborative editing of a single record; a purchase or invoice is authored and acted on by one user at a time.

---

Every useful system exists to keep a small number of important guarantees true, regardless of how many ways there are to break them.

---

## 6. What This Document Leaves Out

- How the domain's entities relate to one another structurally — [`01-business-domain.md`](01-business-domain.md).
- Why the architecture is shaped the way it is — [`02-system-architecture.md`](02-system-architecture.md).
- Every decision this overview only gestures at — [`05-design-decisions.md`](05-design-decisions.md) is where each one is actually argued.

---

## 7. Where to Go Next

This document introduced the vocabulary. The next two build directly on it.

- Continue to [`01-business-domain.md`](01-business-domain.md) for how vendors, categories, purchases, and invoices relate to one another structurally.
- Continue to [`02-system-architecture.md`](02-system-architecture.md) for how the system that supports this lifecycle is built.
- Jump directly to [`03-business-workflows.md`](03-business-workflows.md) for the full mechanics of the lifecycle summarized in Section 3.
- See the [Glossary](glossary.md) for a quick-reference definition of every term introduced here.
