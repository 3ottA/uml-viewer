# Handoff: structure, behavior, and context in UML Viewer

Status: product/technical proposal for the next session. No feature code has been changed.

## Goal

Extend UML Viewer from a navigable map of code structure into a place where an engineer can learn **what the system is made of** and **how a core workflow proceeds**, then request a code-grounded explanation of whatever they are examining. Keep the viewer useful without requiring an agent to generate every diagram. This is primarily a learning and design tool; agent-driven code changes are not the initial use case.

The intended window has three cooperating areas:

1. **Context**: facts about the selected component, function, or workflow step; buttons for optional agent explanations; saved results of those explanations.
2. **Structure**: the existing hierarchical map of namespaces/modules, classes, interfaces, and dependencies.
3. **Behavior**: a new view of a named workflow, initially as ordered interactions/sequence steps, with room for activity or state views later.

The exact sizes and placement of these areas are undecided. The current implementation has a right-hand inspector/sidebar, not this finished three-area layout.

## Interaction contract

- A **single click** selects an element in Structure or Behavior. Context immediately switches to that selection and displays available deterministic facts and any saved explanation. Selection does not automatically spend tokens.
- A **double-click** drills down within the panel where it occurred. In Structure, double-clicking `engine` shows its child modules and their relationships. In Behavior, double-clicking a workflow step shows its smaller actions/interactions, if that step has a defined lower level.
- Provide a visible way back to the parent level, consistent with the existing Structure navigation.
- Structure and Behavior remain linked: a behavior step identifies the participating structure elements; selecting the step can highlight them without forcing either panel to drill down. Selecting a structure element can reveal workflows involving it, without inventing a workflow automatically.
- Context always states what is selected and at which level. If an item has no deeper data, double-click should not imply that detail exists.

Example: select `engine` in Structure -> Context shows name, namespace/package, parent, children, dependencies, source locations, and actions such as **Explain purpose** or **Explain role in the system**. Triggering one action runs a code agent and stores its answer. Select a function/step in Behavior -> Context shows its interface, callers/callees or participants, source evidence, and its saved explanation when available.

## Sources of information and trust boundary

| Information | Proposed source | Rule |
| --- | --- | --- |
| Structure topology | Language-specific parser/scanner plus existing policy/IR | Deterministic; keep links to source locations. |
| Behavior steps and edges | Deterministic extraction from a **named entry point, test, or trace** | Say whether the diagram is a possible code path, a tested path, or an observed execution. |
| Context facts | Structure/behavior models | Show immediately, without an agent call. |
| Narrative explanations | Codex CLI inspecting the repository on demand | Label as agent-generated, preserve evidence and the code revision used. |

Important limit: static dependencies are not an execution sequence. A parser alone cannot reliably identify every "core workflow," resolve all runtime dispatch, or tell which branch actually ran. The first behavior view should start from one deliberately chosen entry point or test and show only steps the extractor can justify. A real observed path needs test instrumentation or runtime traces. Do not use AI to silently fill gaps in a diagram presented as deterministic.

Likewise, domain terminology, business purpose, and design intent may not be present in code. An explanation can connect available evidence and state uncertainty, but it should not be treated as a verified fact merely because the agent produced it.

## On-demand explanation flow

1. User selects an element and presses a specific action, e.g. **Explain this component**.
2. The app supplies Codex with the repository root, selected stable element ID, current revision, the action, relevant known facts, and instructions to cite inspectable files/lines and state uncertainty. Run with read-only access for explanation tasks.
3. Codex returns a structured final result: explanation, supporting source locations, and any uncertainty/limitations. The UI shows progress/error without replacing the previous valid answer until a new run succeeds.
4. Store the complete **final answer** for that selection/action. Keep execution events/logs separately for diagnostics; they are not the Context article.

`codex exec` is a plausible first integration: it supports noninteractive runs, explicit `$skill-name` invocation in the prompt, and structured final output via `--output-schema`. Confirm the installed CLI's exact flags when implementing. Start with one focused explanation skill and a few action templates; split into multiple skills only if the buttons require genuinely different research procedures. Do not make a skill just because a new button exists.

**Display lifetime vs. cache lifetime:** changing selection immediately changes the Context display, but should not delete an earlier answer or launch Codex again. Reuse a saved answer when the same element/action is revisited at the same code and prompt/skill version. For a first implementation, cache by repository revision + element ID + action ID + skill/prompt version. This may invalidate more than necessary after a commit, but avoids falsely showing stale text as current. Define how dirty working trees are identified before enabling caching for them. A changed revision should mark the old answer stale and offer regeneration; never quietly relabel it current.

## What exists in this checkout

- `README.md` documents hierarchical navigation, selection, the right-hand inspector, proposals, agent mailbox, and the EDN IR.
- `src/uml_viewer/graph.clj` defines `LanguageGraph`; `src/uml_viewer/clojure_language/graph_clojure.clj` is the current language implementation. The current scanner generates structure, not workflow behavior.
- `src/uml_viewer/application/document.clj` loads/compiles the IR and tracks focus, selection, and session state.
- `src/uml_viewer/application/events.clj` handles clicks, selection, drill-down, and navigation.
- `src/uml_viewer/adapters/draw.clj`, `src/uml_viewer/adapters/sketch.clj`, and `src/uml_viewer/engine/layout.clj` render and handle the current diagram and inspector. The sidebar is currently fixed-width (`sidebar-w`), so three-area layout needs deliberate work.
- `src/uml_viewer/domain/mailbox.clj` and `adapters/sketch.clj` contain existing viewer/agent communication. Reuse or adapt that boundary before adding another messaging mechanism. The present companion starts Grok; substituting/adding Codex for explanation actions is a separate decision from rendering behavior diagrams.
- The existing IR and policies distinguish generated topology from authored policy/proposals. Preserve that distinction for the behavior model and any annotations.

## Smallest useful build sequence

1. **Interaction prototype:** use one hand-authored workflow fixture linked to the current structural IR. Render a Behavior area beside Structure; implement select, double-click drill, back, and cross-highlighting. Context shows deterministic facts only. This tests the navigation before solving program analysis.
2. **One deterministic extractor:** choose one language and one named workflow entry point or test. Emit the same behavior model as the fixture, including stable IDs and source evidence. Mark unsupported/ambiguous calls as unresolved rather than inventing steps.
3. **One explanation action:** invoke Codex read-only for the selected component or step, store the structured final response, display sources, and implement the revision-aware stale/reuse rule.
4. Expand only after the above is useful: additional workflow types, state machines, languages, repository links, tests/traces, and more explanation actions.

First demonstration target: a small, real workflow in this repository (for example, source/policy -> generated IR -> displayed diagram). The first deliverable should let a new reader trace it, inspect a step's source, and explain the flow back without relying on a generated essay alone.

## Decisions to polish before or during implementation

1. **Layout:** where Context and Behavior sit around the existing canvas; resizing and narrow-window behavior.
2. **Workflow seed:** the first real workflow and whether its ground truth comes from a named function, a test, or a trace.
3. **Granularity:** what a behavior step represents at each drill level (component interaction, function call, internal action).
4. **Cross-panel sync:** highlight only on single click, or also navigate the other panel under an explicit command.
5. **Model format:** extend EDN IR or store behavior in a linked sidecar. Keep stable IDs and evidence either way; avoid a generic UML framework before one flow works.
6. **Agent action contract:** the first button's exact prompt/skill, output schema, source verification, and storage location.
7. **Revision handling:** how to identify uncommitted changes and when to call an explanation stale.

## Suggested opening prompt for the next session

"Read `FEATURE_HANDOFF.md` and the relevant UML Viewer source files. Help me settle the first workflow seed and three-area interaction, then implement the smallest end-to-end behavior view linked to the existing structure view. Preserve deterministic diagram provenance; keep Codex explanations on demand."
