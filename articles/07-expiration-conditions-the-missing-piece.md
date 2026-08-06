# Expiration Conditions: The Missing Piece in Most Architecture Decisions

[← Back to README](../README.md) · [← Articles index](README.md)

Search almost any mature codebase and you'll find a comment that reads something like `// TODO: revisit this once we scale past X` — and if you check how old that comment is, there's a good chance it predates several people who currently work on the team. Nobody removed it because nobody was wrong to write it. The condition it named may well have arrived already. Nobody was watching for it.

This is one of the most common and least discussed failure modes in software architecture, and it has nothing to do with making a bad decision. It's about making a perfectly good, perfectly reasonable decision — and then never assigning anyone the job of noticing when that decision's reasoning stopped applying.

## Every Decision Has a Shelf Life, Whether or Not It's Labeled

Almost no architectural decision is meant to be permanent, even when it's written as though it were. "We'll use a simple in-memory list for this, since only a handful of users need it" is a decision that's correct today and was never claimed to be correct forever — it's correct *conditional on* the list staying small. That condition is real whether or not anyone writes it down. The only choice engineers actually have is whether the condition is explicit, checkable, and owned, or implicit, unchecked, and drifting.

Call the explicit version an **expiration condition**: a specific, checkable circumstance — a date, a threshold, a triggering event — under which a decision's original justification stops holding, paired with a person or process responsible for actually checking it when the time comes. Most architecture decisions that later look like mistakes in a postmortem weren't mistakes when they were made. They were decisions that had an implicit expiration condition nobody made explicit, so nobody was watching for it, so it arrived, and quietly kept being ignored, until something forced the issue at a worse time than a scheduled review would have chosen.

## Why "We'll Revisit This Later" Almost Never Happens

"We'll revisit this later" is one of the most common sentences in software engineering, and one of the least reliably honored. It fails for a boring, structural reason: it names an intention without naming a trigger. Nothing in a calendar, a ticketing system, or a team's working rhythm causes "later" to actually arrive on its own. Without a specific date, a specific metric crossing a specific threshold, or a specific event that's already scheduled to happen, "later" competes against every concrete, deadline-bound piece of work currently in front of the team — and concrete, deadline-bound work always wins that competition, every single sprint, forever.

The fix costs almost nothing at the moment the original decision is made, which is exactly what makes skipping it so easy to justify under time pressure: instead of "we'll revisit this once it becomes a problem," write "we'll revisit this once the list exceeds ten entries" or "we'll revisit this at the Q3 planning review" or "this needs to be reconsidered the next time the team reorganizes." Any of those is checkable by someone other than the original author, at a specific point they'll actually recognize, rather than requiring anyone to remember an open-ended intention indefinitely.

## The Distinction This Actually Produces

Attaching expiration conditions to temporary decisions doesn't mean second-guessing every shortcut a team takes — plenty of shortcuts are exactly the right call, and staying suspicious of every one of them is its own kind of waste. The distinction this habit produces is much narrower and more useful: it separates a shortcut that's still correct, because its condition hasn't arrived yet, from a shortcut that's silently expired, because nobody assigned anyone the job of checking. Both look identical in the code. Only one of them is still a good decision.

A concrete instance of exactly this correction — a real decision, revisited honestly, where the underlying choice held up completely and the actual gap was the missing expiration condition, not the decision itself — is worked through in [Lessons Learned, Section 4](../docs/08-lessons-learned.md#4-the-identity-override).

The next time you make a decision you know is a shortcut — a hardcoded list, a "temporary" workaround, a simplification that's correct only while something stays small — spend the extra thirty seconds writing down the specific condition under which it stops being correct, and who's responsible for noticing. That thirty seconds is the entire difference between a trade-off someone will actually revisit and a TODO comment someone will find, confused, three years from now.

---

*This essay generalizes a lesson developed in full, across four revisited decisions, in [Lessons Learned](../docs/08-lessons-learned.md).*
