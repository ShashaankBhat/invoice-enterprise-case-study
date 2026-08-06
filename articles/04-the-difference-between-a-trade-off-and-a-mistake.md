# The Difference Between a Trade-off and a Mistake

[← Back to README](../README.md) · [← Articles index](README.md)

Every engineer has said some version of this sentence in a retrospective: "we knew about this, we just didn't think it would matter." It's usually said with a slightly apologetic tone, as if it's a confession. It shouldn't be. Whether that sentence describes good engineering or bad engineering depends entirely on one detail that the sentence itself doesn't reveal: was the assumption ever written down, and did anyone own the job of checking whether it still held?

That detail is the entire difference between a trade-off and a mistake. Both start the same way — a decision made under a specific assumption about scale, usage, or risk. They diverge based on what happens after the assumption stops being true.

## A Trade-off Is a Decision With Its Assumption Attached

A trade-off is a choice made deliberately, with a stated cost, under an assumption that's explicit enough to check later. "We're not adding a lock here because concurrent writes to the same record are rare at our current volume" is a trade-off — not because it's risk-free, but because it names exactly the condition under which it would need to be revisited: concurrent writes becoming less rare. Anyone reading that decision later, including its original author eighteen months on, can evaluate it against current reality instead of having to reconstruct what was assumed from memory or from the shape of the bug that eventually surfaced.

A mistake, by contrast, is usually the same decision made silently — a shortcut taken under time pressure, with the reasoning living only in the head of whoever made it, evaporating the moment they move to a different team. The system ends up in an identical state either way. The difference only shows up later, when the assumption breaks: a trade-off gets caught during a scheduled check, because someone was watching for exactly this. A mistake gets caught during an incident, because nobody was watching for anything.

## Why "We'd Do It Differently Now" Is the Wrong Question to Ask First

When revisiting an old decision, the instinct is to ask "would I make this choice again?" That question invites a binary answer and produces a shallow retrospective — a list of things that were "wrong," most of which weren't wrong at all when they were decided, given what was known and what was actually likely at the time.

A better first question: "what was the assumption, and does it still hold?" This reframes the exercise from judgment to verification, and it produces a much more useful, and much more common, answer than "this was a mistake": *the assumption still holds, and the decision is still correct.* That answer matters — a disciplined review that confirms a decision is still sound is not a wasted exercise, and treating every revisit as a hunt for regret trains engineers to either avoid revisiting decisions at all, or to manufacture criticism of choices that were perfectly reasonable.

When the assumption genuinely no longer holds, the next question isn't automatically "replace the whole decision." Often the actual gap is narrower and more interesting: the original decision was right, and what was missing was never a better implementation — it was an explicit trigger for checking whether the assumption still held. A shortcut adopted "because the affected group is small" isn't wrong for being a shortcut. It's incomplete for never having been given a date, a threshold, or a named owner responsible for asking, later, whether "small" is still true.

## What This Looks Like in Practice

An enterprise invoice-processing system offers a concrete instance of exactly this distinction: a decision to grant a handful of specific people cross-organizational visibility through an explicit list, rather than building a formal role for it. Revisited honestly, the finding isn't "this should have been a formal role from day one" — nothing in the system's actual usage showed the list had grown past where the shortcut was reasonable. The finding is that the decision was never given a scheduled trigger for re-examination, which is a different, more specific, and more fixable problem than "we chose wrong."

The full accounting — four decisions revisited, three confirmed with a scheduling correction and one confirmed outright, each checked against its original assumption rather than judged by outcome — is worked through in [Lessons Learned](../docs/08-lessons-learned.md).

The next time you're tempted to say "we knew about this, we just didn't think it would matter," ask yourself one more question before you say it: was that assumption written down somewhere, with someone's name next to the job of checking it? If yes, you made a trade-off, and you're allowed to say so without apologizing. If no, say that instead — it's a more useful thing to learn from than a false confession.

---

*This essay generalizes a discipline demonstrated in full, across four real decisions, in [Lessons Learned](../docs/08-lessons-learned.md).*
