# Handoff: structure, behavior, and context in UML Viewer

Status: first built-in workflow version implemented; agent-generated explanations remain future work.

## Decisions settled in the design interview

- The first usable release lets a new reader trace one named workflow from entry point to source through Structure and Behavior. Context shows deterministic facts and Curated explanations for the main components and behavior steps. Explanation generation and buttons are deferred.
- The first Behavior view claims a **possible code path** justified by static evidence. It marks ambiguous calls, branches, and missing links rather than claiming a tested path or observed execution.
- The first workflow is this repository's policy and source -> generated EDN -> displayed diagram flow. The EDN file is a handoff between separate generator and viewer processes.
- The first Behavior level shows broad workflow stages. Double-clicking a stage reveals named functions involved in that stage where the source supports them.
- Prewritten explanations cover every top-level Structure component, every broad workflow stage, and the named functions shown beneath those stages. Other selections may show deterministic facts without an explanation.
- Context presents that prose in a separate **Curated explanation** section with links to supporting source locations, distinct from extracted facts.
- When Structure shows a proposal, Behavior continues showing the current-code workflow without an extra label. Links and cross-highlighting between the two panels are disabled in that state.
- **Context** remains the panel name. Use **selection context** for the selected item and its details, and **architecture context** for the real diagram or proposal currently under discussion.
- The built-in workflow and curated explanations appear only when UML Viewer examines its own repository. Other repositories show an empty Behavior state until they have workflow data.
- For the first version, the hand-authored workflow and curated explanations live as data in Clojure source, linked by stable structure IDs and source locations. A reusable external workflow file format is deferred.
- The top-level Behavior view is a flowchart of broad stages with the generated EDN shown as an explicit file handoff between generator and viewer processes.
- Its stages are **Read policy and scan source** -> **Build and write IR** -> generated EDN file -> **Load and compile diagram** -> **Draw current view**.
- In a drilled stage, arrows between named functions mean source-verified direct calls or data handoffs. Uncertain relationships remain unconnected or explicitly unresolved.
- Selecting a Structure element does not change Behavior automatically. In the real architecture context, Context offers **Show related behavior** for elements that participate in the workflow.
- **Show related behavior** opens the top-level workflow and highlights every stage related to the selected Structure element; the reader chooses where to drill next.
- The built-in sample remains visible for this repository without checking its source references against the viewed checkout at load time. Its authored links may drift as code changes.
- At normal window width, Structure and Behavior appear side by side with Context on the right. At narrow widths, switch between Structure and Behavior while keeping Context available.
- Selecting a behavior step highlights visible Structure participants. **Show in Structure** is an explicit navigation action; selection alone does not change the Structure drill level.

## Goal

Extend UML Viewer from a navigable map of code structure into a place where an engineer can learn **what the system is made of** and **how a core workflow proceeds**. The first version provides curated explanations; on-demand code-agent explanations may follow later. This is primarily a learning and design tool.

The intended window has three cooperating areas:

1. **Context**: facts about the selected component, function, or workflow step, plus a separate Curated explanation when one is available.
2. **Structure**: the existing hierarchical map of namespaces/modules, classes, interfaces, and dependencies.
3. **Behavior**: a flowchart of the named workflow's broad stages, an explicit EDN file handoff, and named functions within each stage.

At normal width, Structure and Behavior sit side by side with Context on the right. At narrow width, readers switch between Structure and Behavior while Context stays available. Exact dimensions and resizing remain implementation choices. The current implementation has a fixed-width right-hand inspector/sidebar.

## Interaction contract

- A **single click** selects an element in Structure or Behavior. Context immediately switches to that selection and displays available facts and the Curated explanation, if one exists.
- A **double-click** drills down within the panel where it occurred. In Structure, double-clicking `engine` shows its child modules and their relationships. In Behavior, double-clicking a broad stage shows its named function steps.
- Provide a visible way back to the parent level, consistent with the existing Structure navigation.
- With the real structure selected, clicking a Behavior step highlights its visible Structure participants without changing Structure's drill level. **Show in Structure** explicitly navigates there. Clicking Structure does not change Behavior; **Show related behavior** opens the top-level workflow and highlights all related stages. With a proposal selected, the current-code workflow remains visible but all cross-panel links and highlighting are off.
- Context always states what is selected and at which level. If an item has no deeper data, double-click should not imply that detail exists.

Example: select `engine` in Structure -> Context shows its name, namespace/package, parent, children, dependencies, source locations, and Curated explanation. Select a function step in Behavior -> Context shows its participants, source evidence, and Curated explanation. Every top-level Structure component, broad stage, and drilled function step has one; other selections may show facts only.

## Sources of information and trust boundary

| Information | Proposed source | Rule |
| --- | --- | --- |
| Structure topology | Language-specific parser/scanner plus existing policy/IR | Deterministic; keep links to source locations. |
| First Behavior steps and edges | Hand-authored data in Clojure source, checked against named code paths while authoring | Present as a **possible code path**. Function arrows mean source-supported calls or data handoffs; show uncertainty rather than inventing a link. |
| Context facts | Structure/behavior models | Show immediately, without an agent call. |
| Curated explanations | Hand-authored data in Clojure source | Display separately from facts with supporting source links. They are not generated at selection time. |

Important limit: static dependencies are not an execution sequence. The first view is a code-grounded authored example, not a trace or automatic extraction. Its stages are **Read policy and scan source** -> **Build and write IR** -> generated EDN file -> **Load and compile diagram** -> **Draw current view**. The file separates generator and viewer processes. A real observed path needs runtime evidence. Do not invent missing links. The sample appears only when viewing this repository; it remains visible without checking references against the checkout at load time, so links can drift after code changes.

Likewise, domain terminology, business purpose, and design intent may not be present in code. Curated prose can connect available evidence and state uncertainty, but it remains an interpretation distinct from extracted facts.

## Later: on-demand explanation flow

1. User selects an element and presses a specific action, e.g. **Explain this component**.
2. The app supplies Codex with the repository root, selected stable element ID, current revision, the action, relevant known facts, and instructions to cite inspectable files/lines and state uncertainty. Run with read-only access for explanation tasks.
3. Codex returns a structured final result: explanation, supporting source locations, and any uncertainty/limitations. The UI shows progress/error without replacing the previous valid answer until a new run succeeds.
4. Store the complete **final answer** for that selection/action. Keep execution events/logs separately for diagnostics; they are not the Context article.

`codex exec` is a plausible first integration: it supports noninteractive runs, explicit `$skill-name` invocation in the prompt, and structured final output via `--output-schema`. Confirm the installed CLI's exact flags when implementing. Start with one focused explanation skill and a few action templates; split into multiple skills only if the buttons require genuinely different research procedures. Do not make a skill just because a new button exists.

**Display lifetime vs. cache lifetime for a later agent integration:** changing selection immediately changes the Context display, but should not delete an earlier answer or launch Codex again. Reuse a saved answer when the same element/action is revisited at the same code and prompt/skill version. A possible cache key is repository revision + element ID + action ID + skill/prompt version. Define how dirty working trees are identified before enabling that cache. A changed revision should mark the old answer stale and offer regeneration.

## What exists in this checkout

- `README.md` documents hierarchical navigation, selection, the right-hand inspector, proposals, agent mailbox, and the EDN IR.
- `src/uml_viewer/graph.clj` defines `LanguageGraph`; `src/uml_viewer/clojure_language/graph_clojure.clj` is the current language implementation. The current scanner generates structure, not workflow behavior.
- `src/uml_viewer/application/document.clj` loads/compiles the IR and tracks focus, selection, and session state.
- `src/uml_viewer/application/events.clj` handles clicks, selection, drill-down, and navigation.
- `src/uml_viewer/adapters/draw.clj`, `src/uml_viewer/adapters/sketch.clj`, and `src/uml_viewer/engine/layout.clj` render and handle the current diagram and inspector. The sidebar is currently fixed-width (`sidebar-w`), so three-area layout needs deliberate work.
- `src/uml_viewer/domain/mailbox.clj` and `adapters/sketch.clj` contain existing viewer/agent communication. Reuse or adapt that boundary before adding another messaging mechanism. The present companion starts Grok; substituting/adding Codex for explanation actions is a separate decision from rendering behavior diagrams.
- The existing IR and policies distinguish generated topology from authored policy/proposals. Preserve that distinction for the behavior model and any annotations.

## Smallest useful build sequence

1. **First usable version:** put this repository's hand-authored workflow and Curated explanations in Clojure source, linked to structural IDs and source locations. Render the four-stage flowchart and EDN handoff beside Structure; implement select, drill, back, cross-highlighting, and explicit navigation. Context shows facts and Curated explanations for every top-level component, broad stage, and drilled function step. No agent integration or explanation buttons are needed yet.
2. **Later:** add deterministic extraction from a named entry point or test, with stable IDs and source evidence. Mark unsupported or ambiguous calls as unresolved.
3. **Later:** add one read-only Codex explanation action, structured output, and revision-aware stale/reuse rules.
4. Expand only after the above is useful: additional workflow types, state machines, languages, repository links, tests/traces, and more explanation actions.

The first deliverable should let a new reader trace the diagram generation and display flow, inspect a step's source, and explain the flow back using the graph and Context.

## Implementation details still open

- Exact panel dimensions, resizing, and narrow-window breakpoint.
- How the app recognizes that the viewed project is this repository.
- The precise function steps and source references under each stage; author only edges justified by the inspected source.
- Agent prompt, output, storage, and revision rules when on-demand explanations are added later.

## Suggested opening prompt for the next session

"Read `FEATURE_HANDOFF.md` and the relevant UML Viewer source files. Implement the first built-in workflow and Curated explanations as specified here. Preserve the authored workflow's code evidence and its possible-path claim. Leave Codex explanation generation for later."
