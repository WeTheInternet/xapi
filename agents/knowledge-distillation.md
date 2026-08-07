# Knowledge Distillation and Session Wrap-Up

Run this process at the end of substantial implementation, investigation, migration, or handoff work.

## 1. Verify the outcome

- Re-read the user objective and confirm what is complete versus deferred.
- Record exact changed files, commands, tests, publication, and downstream verification.
- Distinguish scoped changes from unrelated worktree state.

## 2. Review the session for durable knowledge

Look for:

- corrected assumptions;
- non-obvious build/test/publication behavior;
- source-of-truth and generated-file rules;
- consumer compatibility constraints;
- abandoned ideas, alternatives, concerns, and follow-up plans;
- repeated confusion that better routing or guidance could prevent.

## 3. Distill into the right place

- Obvious verified current reality -> update `agents/knowledge/` or a nested `AGENTS.md`.
- Owner-confirmed policy or architectural choice -> `agents/decisions/`.
- Bounded remaining work or investigation -> `agents/tasks/`.
- Repeatable multi-step workflow -> propose a skill; do not disguise it as general knowledge.
- Ephemeral session coordination -> handoff outbox, not repository docs.

Avoid copying raw chat history. Preserve evidence, conclusions, uncertainty, and actionable next steps.

## 4. Resolve contentious gaps

If the update would introduce new policy, change architecture, classify uncertain code, or materially redirect a plan, conduct a short owner interview before making it normative. If the update is an obvious consequence of verified work, apply it and tell the user what documentation changed.

## 5. Close the task

- Update the originating task note with final status and verification.
- Ensure current-reality docs no longer contradict the code.
- Report remaining tasks and suggested review/staging paths.
- Suggest improvements to agent guidance when useful, even if documentation was not requested.
