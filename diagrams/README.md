# Diagrams

[← Back to README](../README.md)

Every diagram referenced from `docs/` lives here as its own self-contained file — an embedded diagram plus a short explanation, so each one can be understood without the surrounding chapter. None of these introduce a new idea; every diagram visualizes an argument already made in `docs/`, cited at the top of each file.

| Diagram | Shows | Owning document |
|---|---|---|
| [`architecture.md`](architecture.md) | The layered architecture and where its boundaries sit | [`02-system-architecture.md`](../docs/02-system-architecture.md) |
| [`database-er.md`](database-er.md) | Every entity and how they relate | [`01-business-domain.md`](../docs/01-business-domain.md) |
| [`workflow.md`](workflow.md) | The purchase-to-settlement reconciliation flow | [`03-business-workflows.md`](../docs/03-business-workflows.md) |
| [`state-machine.md`](state-machine.md) | The computed status states for a purchase and for an invoice | [`03-business-workflows.md`](../docs/03-business-workflows.md) |
| [`security-flow.md`](security-flow.md) | The two authorization layers and where each applies | [`04-security-model.md`](../docs/04-security-model.md) |
| [`dashboard-flow.md`](dashboard-flow.md) | How role-scoped queries feed a live-aggregated dashboard | [`07-scalability.md`](../docs/07-scalability.md) |
| [`reconciliation-sequence.md`](reconciliation-sequence.md) | A purchase and its invoices, from creation through the invariant check to settlement | [`05-design-decisions.md`](../docs/05-design-decisions.md) |

If you want the full argument behind any diagram, follow its owning-document link. If you just want the shape of an idea before committing to reading a chapter, start here.
