# Glossary

[← Back to README](../README.md)

Every term below is used consistently across this repository — once introduced, a later chapter never redefines it, only builds on it. If a word here is capitalized in a document, it's being used in this specific, defined sense, not as ordinary English.

---

## Core Domain Entities

**Vendor** — An external party the organization purchases from. Referenced by identity from every Purchase Record made against it, never copied. See [`01-business-domain.md`](01-business-domain.md).

**Expense Category** / **Expense Type** — A two-level classification a Purchase Record is tagged with; Expense Type is the more specific classification within an Expense Category. See [`01-business-domain.md`](01-business-domain.md).

**Purchase Record** — A committed financial amount, tied to a Vendor, an Expense Category and Type, and an Operational Unit, with Supporting Documents attached. See [`01-business-domain.md`](01-business-domain.md) and [`03-business-workflows.md`](03-business-workflows.md).

**Invoice Record** — A claim against a specific Purchase Record's committed amount, tracked independently toward payment. See [`03-business-workflows.md`](03-business-workflows.md).

**Supporting Document** — Evidence attached to a Purchase Record or Invoice Record — a purchase order copy, a receipt, or similar. See [`01-business-domain.md`](01-business-domain.md).

**Organization** / **Operational Unit** — A two-level hierarchy; every Operational Unit belongs to exactly one Organization, and every Purchase Record belongs to exactly one Operational Unit. Scopes visibility — see [`01-business-domain.md`](01-business-domain.md) and [`04-security-model.md`](04-security-model.md).

---

## Roles

**Finance User** — Creates and manages Purchase Records and Invoice Records within their own Operational Unit.

**Finance Administrator** — Everything a Finance User can do, plus administering Vendor, Expense Category, and Expense Type data.

**System Administrator** — Unrestricted visibility and access across the entire system.

**Cross-organizational reviewer** — A small number of individuals granted visibility across Organization boundaries via an identity-based visibility override (see [ADR-006](05-design-decisions.md#adr-006--identity-based-visibility-overrides) and [The Explicit Exception List](10-engineering-patterns.md#7-the-explicit-exception-list)), not a role in the usual sense. See [`04-security-model.md`](04-security-model.md).

All four are introduced in [`00-overview.md`](00-overview.md).

---

## Core Engineering Concepts

**Guarantee** — A property the system promises will always remain true, regardless of which execution path reaches it. See [`06-architecture-trade-offs.md`](06-architecture-trade-offs.md).

**Invariant** — A specific guarantee that spans more than one record — in this system, that the combined value of a Purchase Record's Invoice Records can never exceed its committed amount. See [`03-business-workflows.md`](03-business-workflows.md) and [`05-design-decisions.md`](05-design-decisions.md).

**Relationship** (as in "correctness is relational") — The idea that correctness is often a property of multiple records taken together, rather than any single record in isolation. Central to [`03-business-workflows.md`](03-business-workflows.md).

**Single Ownership** — Enforcing a rule from exactly one place in the system, rather than duplicating it across several. Sometimes called Single Source of Truth, Sole Authority, or Single Writer elsewhere in the industry. See [`06-architecture-trade-offs.md`](06-architecture-trade-offs.md) and [`10-engineering-patterns.md`](10-engineering-patterns.md).

**Computed State** — A value derived from underlying data on every read, rather than stored and separately maintained. See [`05-design-decisions.md`](05-design-decisions.md#adr-003--computed-status-instead-of-a-persisted-status-field).

**Eligibility** — The coarse, role-based authorization question: can a user with this role reach this feature at all? See [`04-security-model.md`](04-security-model.md).

**Entitlement** — The narrow, record-level authorization question: does this specific user have a legitimate relationship to this specific record? See [`04-security-model.md`](04-security-model.md).

**Trade-off** — A guarantee's cost, made explicit: what's gained, what's spent, and where. See [`06-architecture-trade-offs.md`](06-architecture-trade-offs.md).

**Visible Cost** — A cost that's been named and documented, as opposed to one that's been hidden by an optimization that only appears to be free. Central to [`06-architecture-trade-offs.md`](06-architecture-trade-offs.md) and [`11-system-evolution.md`](11-system-evolution.md).

**Expiration Condition** — The specific, checkable circumstance under which a decision's original assumption stops holding, and the decision needs to be revisited. See [`08-lessons-learned.md`](08-lessons-learned.md).

---

## Named Engineering Patterns

Each of the following is introduced and fully argued in [`10-engineering-patterns.md`](10-engineering-patterns.md):

- **Single Ownership** — see above.
- **Derive, Don't Duplicate** — computing a value instead of storing and syncing it.
- **Coarse-Then-Fine Authorization** — Eligibility checked cheaply before Entitlement is checked precisely.
- **The Aggregate Invariant** — an Invariant enforced across a cluster of related records, closely related to domain-driven design's aggregate boundary.
- **Reference Data With History** — master data that carries a change history; transactional data that doesn't.
- **The Explicit Exception List** — a narrow, identity-based grant of access outside the normal role hierarchy, safe only with an Expiration Condition attached.

---

## Where to Go Next

This glossary is a lookup reference, not a reading-order document. Start with [`00-overview.md`](00-overview.md) if you haven't yet, or jump to any term's linked chapter for the full argument behind it.
