# Cursor / Java Language Server (PG workspace)

Red Hat Java LS can crash when indexing this monorepo (`site/`, large CD folders, build outputs).

## Already configured in this repo

- `.vscode/settings.json` — 2GB heap, exclude `site` / `build` / `PG CD` / `tmp_*`
- `.cursorignore` — same heavy paths for AI indexing

## If status bar still shows “Java: Error”

1. `Ctrl+Shift+P` → **Java: Clean Java Language Server Workspace** → Reload and delete
2. Wait until import finishes (status bar coffee cup)
3. If it crashes again: close other heavy folders, ensure only `pg-app` Gradle project is imported
4. Optional: disable **Language Support for Java** temporarily if you only edit `site/` JS

Do not open `PG CD/` or zip archives inside this workspace.
