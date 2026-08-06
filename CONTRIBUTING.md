# Contributing

[← Back to README](README.md)

This isn't a collaboratively-developed project accepting new features — it's a case study with a deliberately fixed scope. What's welcome is corrections, clarity improvements, and proposed additions that follow the standards below closely enough that they don't erode the consistency the rest of the repository has built up. Corrections and clarity issues are welcome; if you're proposing a new document, read this file in full first.

---

## Documentation Standards

These matter more than the code standards below, because this repository's value is almost entirely in its writing discipline.

### The Frozen Vocabulary

The following terms are used consistently across every document, in this specific, defined sense: **Guarantee**, **Invariant**, **Relationship**, **Single Ownership**, **Computed State**, **Eligibility**, **Entitlement**, **Trade-off**, **Visible Cost**, **Expiration Condition**. Full definitions are in [`docs/glossary.md`](docs/glossary.md).

Do not introduce a new capitalized term unless the idea genuinely isn't covered by one of the above — and if it isn't, add it to the glossary in the same contribution, don't leave it defined only by example. Do not use a synonym for an existing term because it reads slightly better in one sentence; consistency across the repository is worth more than local elegance in any single sentence.

### The Guarantee / Pattern / Implementation-Detail Distinction

Every document in `docs/` sits at one of three altitudes, and a contribution should stay at the altitude of the document it's editing:

- **Guarantees and decisions** ([`05-design-decisions.md`](docs/05-design-decisions.md)) — what the system promises, and why that specific promise was chosen over its alternatives.
- **Patterns** ([`10-engineering-patterns.md`](docs/10-engineering-patterns.md)) — the reusable shape a guarantee is protected by, named so it transfers to other systems.
- **Implementation details** — specific to this system's structure. These belong in [`02-system-architecture.md`](docs/02-system-architecture.md) at most, and should almost never appear in `articles/`, which are written to stand on their own without implementation specifics.

A contribution that describes *how* something is built, in a document whose job is to explain *why*, is the single most common way this repository's quality would erode over time. When in doubt, ask which of the three altitudes above the sentence belongs to before deciding where it goes.

### The House Style for `docs/`

Every conceptual chapter (`00` through `08`, `10`, `11`) follows the same shape:

1. A guiding question, stated in bold, immediately after the title.
2. A one-paragraph thesis that answers it, before any evidence is introduced.
3. Several sections, each serving as evidence for the thesis — not a list of unrelated subtopics.
4. A single memorable closing law, set off on its own line before the final two sections.
5. "What This Document Leaves Out" — an explicit list of adjacent topics this document deliberately doesn't cover, with a link to whichever document does.
6. "Where to Go Next" — links forward, not just backward.

`09-interview-guide.md` and `glossary.md` are the two deliberate exceptions — practical reference material, not part of the conceptual arc, and they say so explicitly at their own openings.

### Confidentiality — the Rebuild Test

Every sentence in this repository, in every document, must pass one test: *could someone reconstruct a real, private system from this?* If yes, it doesn't belong here, regardless of how illustrative it would be. No real company, product, or individual is named anywhere. No source code, schema, or configuration from any real system is reproduced — every reference implementation is original, purpose-written code, built to demonstrate one pattern in isolation, never adapted from anything resembling a production codebase.

---

## Code Standards (Reference Implementations)

- Plain Java, no build tool or framework dependency — every sample must compile and run with `javac` and `java` directly, nothing more.
- One pattern per file. If a sample starts needing to demonstrate a second idea to make sense, that's a sign it should be two samples, not one.
- Every sample's `main` method must print output that includes at least one rejected/denied/failing scenario, not only success cases — a sample that only shows the happy path doesn't teach the boundary of the pattern it's demonstrating.
- Every sample's README must state, explicitly, what the sample is *not* — not an excerpt of a real system, and not a complete solution to problems adjacent to the one pattern it demonstrates (transaction handling, persistence, session management, unless that's the specific pattern being shown).

---

## Proposing a New Document

Before writing anything, answer three questions, the same way [`ROADMAP.md`](ROADMAP.md) requires of every planned addition:

1. What single guiding question would this document answer?
2. Which existing document, if any, does this extend rather than duplicate?
3. Does this pass the rebuild test?

If you can't answer all three cleanly, the idea likely needs more shape before it's ready to become a document.

---

## What This File Leaves Out

- Git workflow, branch naming, or PR process — this repository doesn't have enough contributor volume to need one yet.
- A code of conduct — see the repository's general terms instead.
- Licensing details for the reference implementations — covered in the root [`README.md`](README.md) disclaimer.
