# Changelog

All notable changes to **Smart Project Manager** will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html) and [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) conventions.

---

## [1.1.1] — 2026-07-17

### 🔒 Security
- **[DB] Passwords never stored in database** — SSH passwords and PEM key paths are now explicitly set to `null` before writing to any database provider (JSON, SQLite/MySQL, MongoDB). Only `sshHost`, `sshPort`, `sshUser`, `containerId`, and `containerName` are persisted. This eliminates plaintext credential exposure in project database files.

### 🐛 Bug Fixes
- **[AIAnalyzer] Wrong Groq model** — Changed default Groq model from `llama-3.1-8b-instant` to `llama-3.3-70b-versatile`, which is the best available model on the free Groq tier. The old model name was still displayed in the Settings panel and the Assistant model label.
- **[AIAnalyzer] Gemini retry not catching SDK exceptions** — `callWithRetry()` was only inspecting `errorMsg` (the exception's own message), missing wrapped `RuntimeException`s thrown by the Gemini SDK (e.g. `resource_exhausted`). Now both the exception message and its cause are inspected; `RuntimeException` class names are also matched.
- **[AIAnalyzer] `generateTags` regex duplicate character class** — The regex `[^a-zçğıöşüa-z0-9\-]` contained a duplicate `a-z` range. Fixed to `[^a-z0-9çğışöü\-]`.
- **[AIAnalyzer] Lost exception in retry loop** — After exhausting all retries the method threw a bare `new Exception(...)` instead of re-throwing the original exception. The root cause is now preserved via a `lastException` variable.
- **[GitManager] Resource leak in `commitAndPush`** — The `Git` object opened by JGit was only closed in the success path. Error and early-return paths skipped `git.close()`, leaking file handles. Moved `git` declaration outside `try` and added a `finally` block.
- **[DockerScanner] SSH session mismatch on reconnect** — `getOrCreateSession()` compared `null` with `""` for `pemPath`, causing a new session to be opened on every command even when the session was already alive. All parameters are now normalized (`null → ""`) before comparison.
- **[DockerScanner] CPU-wasting busy-wait in `executeSshCommand`** — The old `while (in.available() > 0) + Thread.sleep(100)` polling loop was replaced with a proper blocking `InputStream.read(buf)` loop.

### ✨ Improvements
- **[DockerScanner] Readable project names for SSH/Docker containers** — Container names like `python-analytics-wv470znoydrt7sb58sb9hs6i-203349874770` are now sanitized by stripping Docker Compose random hash suffixes and replica indices. Projects are displayed as `python-analytics [SSH-Docker · 1.2.3.4]` instead of the raw internal container ID string.
- **[DockerScanner] SSH session keepAlive health check** — Before reusing an existing SSH session, `sendKeepAliveMsg()` is called to detect silently dropped connections (e.g. idle timeout on the VDS side). On transient failure, a single 1500 ms retry is attempted; authentication failures are re-thrown immediately without retry.
- **[RemoteConnectionDialog] Connection debounce** — Rapid repeated clicks on the "Connect" button no longer spawn parallel `SwingWorker` tasks. A `volatile boolean isConnecting` flag prevents duplicate connection attempts and is reset in a `finally` block regardless of success or failure.
- **[ProjectAssistant] Conversation history memory cap** — `historyMessages` was growing unbounded. It is now capped at 20 messages (10 turns). When the limit is exceeded, the oldest messages are pruned, preventing context-window overflow errors on long sessions.
- **[ProjectAssistant] SSH/Docker context in assistant prompt** — The project context passed to the AI assistant now includes `sshHost`, `sshPort`, `sshUser`, `containerName`, and `containerId` fields for each project. The system prompt was updated with explicit rules so the assistant can answer questions like "what is the VDS IP?" directly from the stored metadata.
- **[UI] Model name display corrected** — `AssistantPanel` (active model label) and `MainGUI` (settings radio button) now correctly display `llama-3.3-70b-versatile` instead of the outdated `llama-3.1-8b-instant`.

### 📚 Documentation
- **README.md & README_TR.md** — Added `v1.1.1` version badge, AI services & supported models table, SSH rate-limit protection feature description, connection profile system entry, updated technology stack section. Fixed typos (`Ücretlsiz`, `Üretsiz`, `önlünür`).
- **CHANGELOG.md** — This file (initial version).

---

## [1.0.0] — 2026-06-23

### 🎉 Initial Release

- Project workspace management (create, edit, list, persist).
- AI-powered code analysis via Gemini, Groq, and GPT (`AIAnalyzer`).
- Docker environment scanner (containers, images, networks).
- Git & GitHub integration via JGit (commit, push, remote management).
- SSH remote connection: file browser, embedded terminal, remote project scan.
- Flexible database backends: SQLite (default), MySQL, MongoDB.
- Connection profile system via `ConfigManager`.
- Modern Java Swing UI with FlatLaf theme and HiDPI support.
- AI project assistant with conversation history (`ProjectAssistant`).
- Auto-generated README and project tags from AI analysis.
- Windows (`run.bat`) and macOS/Linux (`run.sh`) one-click launchers.

---

*For the full diff of each release, see the [GitHub commit history](https://github.com/bymayfe/JavaProjectManager/commits/main).*
