# WindowX++

An addon mod for WindowX.

## Features

### Predicates for `execute if` / `execute unless`

WindowX exposes a large amount of live state. This addon adds predicate support for that state, so it can be tested directly with `execute if predicate` and `execute unless predicate` instead of running a command and inspecting the value it returns.

### Console window (TUI)

Adds terminal-style window support to WindowX. A console window is created by command and addressed by its window id, renders ANSI escape sequences, and can read player input back into the command that requested it.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.234
- WindowX

## License

MIT
