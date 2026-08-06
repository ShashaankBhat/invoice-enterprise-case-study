# Reference Implementations

[← Back to README](../README.md)

Three small, self-contained, runnable code samples — not excerpts from any production system. Each demonstrates exactly one pattern from [`10-engineering-patterns.md`](../docs/10-engineering-patterns.md), stripped of every unrelated concern (framework wiring, persistence, error handling for edge cases outside the pattern being taught) that would otherwise obscure the idea. Plain Java, no build tool or framework required — each sample compiles and runs with `javac` and `java` directly.

This distinction matters: a reference implementation optimizes for teaching one idea in isolation. Production code optimizes for correctness inside a much larger system with many competing concerns. Presenting the two as interchangeable would misrepresent both.

| Reference implementation | Pattern demonstrated | Owning document |
|---|---|---|
| [`derived-status-calculator/`](derived-status-calculator/README.md) | Computing a record's status from its underlying data instead of persisting a separate status field | [ADR-003](../docs/05-design-decisions.md#adr-003--computed-status-instead-of-a-persisted-status-field) |
| [`financial-invariant-validator/`](financial-invariant-validator/README.md) | Enforcing that a set of related records can never together exceed a value fixed on their parent | [ADR-002](../docs/05-design-decisions.md#adr-002--application-layer-financial-invariant-enforcement) |
| [`layered-authorization-example/`](layered-authorization-example/README.md) | Combining a coarse-grained Eligibility check with a fine-grained, record-level Entitlement check | [ADR-005](../docs/05-design-decisions.md#adr-005--two-layer-authorization) |

## How to Run Any of These

Each folder contains exactly one `.java` file with a `main` method. From inside a folder:

```bash
javac *.java
java <ClassName>
```

Each sample prints a small sequence of example scenarios — including at least one that's expected to fail — with the reasoning behind each outcome, so the output itself is part of the explanation.
