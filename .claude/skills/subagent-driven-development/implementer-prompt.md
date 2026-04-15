# Implementer Subagent Prompt Template

Use this template when dispatching an implementer subagent.

```
Task tool (general-purpose):
  description: "Implement Task N: [task name]"
  prompt: |
    You are implementing Task N: [task name]

    ## Task Description

    [FULL TEXT of task from plan - paste it here, don't make subagent read file]

    ## Context

    [Scene-setting: where this fits, dependencies, architectural context]

    ## Before You Begin

    If you have questions about:
    - The requirements or acceptance criteria
    - The approach or implementation strategy
    - Dependencies or assumptions
    - Anything unclear in the task description

    **Ask them now.** Raise any concerns before starting work.

    ## Build/Test Protocol — CRITICAL

    You MUST NOT run any Gradle, build, or test commands yourself.
    Multiple agents share the same build directory — parallel Gradle
    runs corrupt the build cache and cause cascading failures.

    When you need a build or test result (e.g., to verify RED or GREEN):
    1. STOP your current work
    2. Return a message starting with `GRADLE_REQUEST:` followed by:
       - The exact command to run
       - What you expect (e.g., "expect FAIL — class not found")
    3. The orchestrator will run it and send you the output
    4. Continue your work based on the output

    Example:
    ```
    GRADLE_REQUEST: cd android && ./gradlew test --tests "com.example.MyTest" 2>&1 | tail -20
    Expected: FAIL — MyClass not found (RED phase of TDD)
    ```

    ## Your Job

    Once you're clear on requirements:
    1. Implement exactly what the task specifies
    2. Write tests (following TDD if task says to)
    3. Request build/test verification from orchestrator (do NOT run yourself)
    4. Do NOT commit (orchestrator handles commits)
    5. Self-review (see below)
    6. Report back

    Work from: [directory]

    **While you work:** If you encounter something unexpected or unclear, **ask questions**.
    It's always OK to pause and clarify. Don't guess or make assumptions.

    ## Before Reporting Back: Self-Review

    Review your work with fresh eyes. Ask yourself:

    **Completeness:**
    - Did I fully implement everything in the spec?
    - Did I miss any requirements?
    - Are there edge cases I didn't handle?

    **Quality:**
    - Is this my best work?
    - Are names clear and accurate (match what things do, not how they work)?
    - Is the code clean and maintainable?

    **Discipline:**
    - Did I avoid overbuilding (YAGNI)?
    - Did I only build what was requested?
    - Did I follow existing patterns in the codebase?

    **Testing:**
    - Do tests actually verify behavior (not just mock behavior)?
    - Did I follow TDD if required?
    - Are tests comprehensive?

    If you find issues during self-review, fix them now before reporting.

    ## Report Format

    When done, report:
    - What you implemented
    - What you tested and test results
    - Files changed
    - Self-review findings (if any)
    - Any issues or concerns
```
