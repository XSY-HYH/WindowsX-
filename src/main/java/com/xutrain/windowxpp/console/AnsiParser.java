package com.xutrain.windowxpp.console;

// 解析 ANSI 转义序列并写入字符网格
public final class AnsiParser {
    private enum State {
        TEXT, ESCAPE, CSI
    }

    private State state = State.TEXT;
    private final StringBuilder parameters = new StringBuilder();
    private CellStyle style = CellStyle.DEFAULT;

    public void reset() {
        state = State.TEXT;
        parameters.setLength(0);
        style = CellStyle.DEFAULT;
    }

    public void feed(String text, TerminalBuffer buffer) {
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            switch (state) {
                case TEXT -> {
                    if (character == 0x1B) {
                        state = State.ESCAPE;
                    } else {
                        buffer.write(character, style);
                    }
                }
                case ESCAPE -> {
                    if (character == '[') {
                        state = State.CSI;
                        parameters.setLength(0);
                    } else {
                        state = State.TEXT;
                    }
                }
                case CSI -> {
                    if (character >= 0x40 && character <= 0x7E) {
                        handleCsi(character, buffer);
                        state = State.TEXT;
                    } else {
                        parameters.append(character);
                    }
                }
            }
        }
    }

    private void handleCsi(char command, TerminalBuffer buffer) {
        int[] values = parseValues();
        switch (command) {
            case 'm' -> applySgr(values);
            case 'K' -> buffer.clearLine();
            case 'J' -> buffer.clearScreen();
            case 'H', 'f' -> buffer.setCursor(value(values, 1, 1) - 1, value(values, 0, 1) - 1);
            case 'A' -> buffer.moveCursor(0, -value(values, 0, 1));
            case 'B' -> buffer.moveCursor(0, value(values, 0, 1));
            case 'C' -> buffer.moveCursor(value(values, 0, 1), 0);
            case 'D' -> buffer.moveCursor(-value(values, 0, 1), 0);
            default -> {
            }
        }
    }

    private int[] parseValues() {
        String raw = parameters.toString();
        if (raw.isEmpty()) {
            return new int[]{0};
        }
        String[] parts = raw.split(";", -1);
        int[] values = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                values[i] = parts[i].isEmpty() ? 0 : Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                values[i] = 0;
            }
        }
        return values;
    }

    private int value(int[] values, int index, int fallback) {
        if (index >= values.length || values[index] <= 0) {
            return fallback;
        }
        return values[index];
    }

    private void applySgr(int[] values) {
        for (int i = 0; i < values.length; i++) {
            int code = values[i];
            if (code == 0) {
                style = CellStyle.DEFAULT;
            } else if (code == 1) {
                style = style.withBold(true);
            } else if (code == 4) {
                style = style.withUnderline(true);
            } else if (code == 7) {
                style = style.withInverse(true);
            } else if (code == 22) {
                style = style.withBold(false);
            } else if (code == 24) {
                style = style.withUnderline(false);
            } else if (code == 27) {
                style = style.withInverse(false);
            } else if (code >= 30 && code <= 37) {
                style = style.withForeground(AnsiColors.basic(code - 30));
            } else if (code == 39) {
                style = style.withForeground(AnsiColors.DEFAULT_FOREGROUND);
            } else if (code >= 40 && code <= 47) {
                style = style.withBackground(AnsiColors.basic(code - 40));
            } else if (code == 49) {
                style = style.withBackground(AnsiColors.DEFAULT_BACKGROUND);
            } else if (code >= 90 && code <= 97) {
                style = style.withForeground(AnsiColors.basic(code - 90 + 8));
            } else if (code >= 100 && code <= 107) {
                style = style.withBackground(AnsiColors.basic(code - 100 + 8));
            } else if ((code == 38 || code == 48) && i + 2 < values.length && values[i + 1] == 5) {
                int color = AnsiColors.color256(values[i + 2]);
                style = code == 38 ? style.withForeground(color) : style.withBackground(color);
                i += 2;
            } else if ((code == 38 || code == 48) && i + 4 < values.length && values[i + 1] == 2) {
                int color = values[i + 2] << 16 | values[i + 3] << 8 | values[i + 4];
                style = code == 38 ? style.withForeground(color) : style.withBackground(color);
                i += 4;
            }
        }
    }
}
