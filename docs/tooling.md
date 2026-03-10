# Tooling

## Checkstyle

Enforced via `maven-checkstyle-plugin` 3.6.0 with rules in `checkstyle.xml`:

- UnusedImports, IllegalImport (`sun.*` — except `com.sun.net.httpserver` which is allowed for tests)
- EmptyCatchBlock, StringLiteralEquality, EqualsAvoidNull, FallThrough
- OneStatementPerLine, MultipleVariableDeclarations
- SimplifyBooleanExpression, SimplifyBooleanReturn

Run manually:
```bash
mvn checkstyle:check
```

## Claude Code hook

`.claude/settings.json` registers a `PostToolUse` hook that fires after every `Edit` or `Write` tool call:

1. Reads the edited file path from stdin JSON (via `python3`)
2. Skips non-Java files
3. Runs `mvn checkstyle:check -q`

Violations appear as warnings in Claude's output. Exit code 0 = clean.

Hook script: `.claude/scripts/checkstyle-hook.sh`

## Maven

`mvn` is not on PATH. Use the IntelliJ bundled Maven:

```bash
cmd //c "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2023.3.5\plugins\maven\lib\maven3\bin\mvn.cmd" <goals>
```
