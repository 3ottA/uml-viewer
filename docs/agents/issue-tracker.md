# Issue tracker: GitHub

Issues and specs for this repo live in GitHub Issues at `3ottA/uml-viewer`. Use `gh` from this clone.

## Conventions

- Create: `gh issue create --title "..." --body-file <file>`
- Read: `gh issue view <number> --comments`
- List: `gh issue list --state open` with the needed label or state filters
- Comment: `gh issue comment <number> --body-file <file>`
- Label: `gh issue edit <number> --add-label "..."` or `--remove-label "..."`
- Close: `gh issue close <number> --comment "..."`

## Pull requests as a triage surface

**PRs as a request surface: no.**

## Skill operations

- "Publish to the issue tracker": create a GitHub issue.
- "Fetch the relevant ticket": read it with `gh issue view <number> --comments`.

## Wayfinding operations

- Map: one issue labeled `wayfinder:map`.
- Child ticket: a GitHub sub-issue when available; otherwise link it from a task list in the map and put `Part of #<map>` in its body. Label it `wayfinder:<type>` (`research`, `prototype`, `grilling`, or `task`).
- Blocking: use native issue dependencies when available; otherwise put `Blocked by: #<n>, #<n>` at the top of the child body.
- Frontier: take the first open, unassigned child in map order whose blockers are closed.
- Claim: `gh issue edit <n> --add-assignee @me` before starting work.
- Resolve: comment with the answer, close the child, and add a context pointer to the map's decisions.
