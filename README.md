# WindowX++

An addon mod for WindowX.

## Features

### Console window (TUI)

Adds terminal-style window support to WindowX. A console window is created by command and addressed by its window id, renders ANSI escape sequences, and can read player input back into the game. Input is written to the scoreboard objective given by the command.

### Window state as scoreboard criteria

Window position and size are exposed as objective criteria. An objective created with one of these criteria tracks every player automatically, without any command being run.

## Commands

All console commands are under `windowsxc <targets>`.

| Command | Description |
| --- | --- |
| `add <id> <x> <y> <height> <width> <color> [@options] [title]` | Create a console window |
| `remove <id>` | Close a console window |
| `title <id> <text>` | Set the window title |
| `top <id> <true\|false>` | Keep the window above the game window |
| `bg <id> <color>` | Set the terminal background color |
| `font <id> <size>` | Set the font size, 6 to 48 |
| `font <id> family <name>` | Set the font family |
| `opacity <id> <0.1-1.0>` | Set window opacity |
| `cursor <id> <true\|false>` | Show or hide the cursor |
| `print <id> <text>` | Write text at the cursor |
| `print <id> at <x> <y> <text>` | Write text at a cell, 1 based |
| `clear <id> [before\|after\|all]` | Erase around the cursor or the whole screen |
| `scroll <id> <lines>` | Scroll the scroll region, negative scrolls down |
| `region <id> <top> <bottom>` | Limit scrolling to a row range, `0 0` resets |
| `size <id> <height> <width>` | Resize the window in pixels |
| `move <id> <x> <y>` | Move the window in screen coordinates |
| `read <id> <objective> [focus] [live] [prompt]` | Request input and write it to an objective |
| `input cancel <id> [message]` | Stop the pending input, optionally printing a message |
| `input fill <id> <text>` | Prefill the input buffer without submitting |
| `input send <id> <text>` | Fill the buffer and submit it as if the player pressed enter |
| `input mask <id> <true\|false>` | Echo `*` instead of the typed characters |
| `layer <id> <normal\|top\|passthrough\|solid>` | Set the window layer |
| `palette <id> <name>` | Switch the ANSI color scheme |
| `events <id> <true\|false>` | Report key presses and clicks to the storage |
| `list` | List the console windows of the target players |
| `get <id>` | Print the properties of a console |
| `dump <id>` | Write the screen text to `data storage windowxpp:consoles <id>.dump` |

Palette names: `campbell`, `vintage`, `onehalfdark`, `solarized`, `tango`. Switching a palette also switches the terminal background to that scheme.

`list`, `get` and `dump` are answered by the client. The first two print to chat, `dump` writes the whole screen into the storage path above together with a `lines` count.

`read` submits integer values to the objective and always stores the text under `data storage windowxpp:consoles <id>.input`. A non integer submission still reaches the storage and is reported in the console.

Cancelling an input, pressing `Esc` or closing the window writes `{input: "", length: 0, aborted: true}` to the same storage path, so a datapack can tell an answer from a request that was never answered.

## Events

`events <id> true` turns the console into an input device. While text input is not pending, every key press and left or right click is written to `data storage windowxpp:consoles <id>`:

| Field | Meaning |
| --- | --- |
| `key` | GLFW key code of the last press |
| `keyMods` | GLFW modifier bits, Shift `1`, Control `2`, Alt `4`, Super `8` |
| `keyCount` | Monotonic counter, compare it to detect a new press |
| `clickX` `clickY` | Clicked cell, 1 based |
| `clickButton` | GLFW button, left `0`, right `1` |
| `clickCount` | Monotonic counter |

Only presses are reported, holding a key does not stream events. While `read` is waiting for text, the keyboard belongs to the input line and nothing is reported.

Common key codes: `Space 32`, `A-Z 65-90`, `0-9 48-57`, `Escape 256`, `Enter 257`, `Tab 258`, `Backspace 259`, `Delete 261`, `Right 262`, `Left 263`, `Down 264`, `Up 265`, `Home 268`, `End 269`, `F1-F12 290-301`.

A menu loop reads `keyCount` every tick, and when it grows, reads `key` and dispatches on it.


Keys inside a console window: `Enter` submits, `Esc` cancels, `Backspace` deletes across line breaks, `Ctrl+V` and right click paste, right click with a selection copies, `Ctrl` + wheel changes the font size, `Ctrl+Shift` + wheel changes opacity.

Text arguments accept `\e`, `\n` and `\r` escapes so ANSI sequences can be typed in chat.

`@options` is a comma separated list. Window flags are `noshrink`, `nogrow` and `noclose`. Key-value pairs are `bg=#RRGGBB`.

## Criteria

`windowxpp:window_x`, `windowxpp:window_y`, `windowxpp:window_width`, `windowxpp:window_height`.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.234
- WindowX

## License

MIT
