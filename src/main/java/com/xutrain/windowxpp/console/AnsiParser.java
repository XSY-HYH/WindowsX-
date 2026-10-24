package com.xutrain.windowxpp.console;

// 解析 ANSI 转义序列并写入字符网格
public final class AnsiParser {
    private enum State {
        TEXT, ESCAPE, CSI
    }

    private State state = State.TEXT;
    private final StringBuilder parameters = new StringBuilder();
    private CellStyle style = CellStyle.DEFAULT;
    private boolean privateMode;
    private char lastCharacter = ' ';
    private CellStyle lastStyle = CellStyle.DEFAULT;
    private TerminalPalette palette = TerminalPalette.defaultPalette();

    public void setPalette(TerminalPalette palette) {
        if (palette != null) {
            this.palette = palette;
        }
    }

    public void reset() {
        state = State.TEXT;
        parameters.setLength(0);
        style = CellStyle.DEFAULT;
        privateMode = false;
        lastCharacter = ' ';
        lastStyle = CellStyle.DEFAULT;
    }

    public void feed(String text, TerminalBuffer buffer) {
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            switch (state) {
                case TEXT -> {
                    if (character == 0x1B) {
                        state = State.ESCAPE;
                    } else if (character == 0x07) {
                        // 响铃无对应行为，忽略
                    } else {
                        buffer.write(character, style);
                        lastCharacter = character;
                        lastStyle = style;
                    }
                }
                case ESCAPE -> {
                    if (character == '[') {
                        state = State.CSI;
                        parameters.setLength(0);
                        privateMode = false;
                    } else {
                        state = State.TEXT;
                    }
                }
                case CSI -> {
                    if (character >= 0x40 && character <= 0x7E) {
                        handleCsi(character, buffer);
                        state = State.TEXT;
                    } else if (character == '?' && parameters.length() == 0) {
                        privateMode = true;
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
            case 'J' -> buffer.eraseDisplay(mode(values));
            case 'K' -> buffer.eraseLine(mode(values));
            case 'X' -> buffer.eraseCharacters(value(values, 0, 1));
            case 'P' -> buffer.deleteCharacters(value(values, 0, 1));
            case '@' -> buffer.insertCharacters(value(values, 0, 1));
            case 'S' -> buffer.scrollRegionUp(value(values, 0, 1));
            case 'T' -> buffer.scrollRegionDown(value(values, 0, 1));
            case 'b' -> repeatCharacter(values.length == 0 ? 1 : Math.max(0, values[0]), buffer);
            case 'r' -> applyScrollRegion(values, buffer);
            case 'H', 'f' -> buffer.setCursor(value(values, 1, 1) - 1, value(values, 0, 1) - 1);
            case 'A' -> buffer.moveCursor(0, -value(values, 0, 1));
            case 'B' -> buffer.moveCursor(0, value(values, 0, 1));
            case 'C' -> buffer.moveCursor(value(values, 0, 1), 0);
            case 'D' -> buffer.moveCursor(-value(values, 0, 1), 0);
            case 'G' -> buffer.setCursor(value(values, 0, 1) - 1, buffer.cursorY());
            case 'd' -> buffer.setCursor(buffer.cursorX(), value(values, 0, 1) - 1);
            case 's' -> buffer.saveCursor();
            case 'u' -> buffer.restoreCursor();
            case 'L' -> buffer.insertLines(value(values, 0, 1));
            case 'M' -> buffer.deleteLines(value(values, 0, 1));
            case 'h' -> applyPrivateMode(values, true, buffer);
            case 'l' -> applyPrivateMode(values, false, buffer);
            default -> {
            }
        }
        privateMode = false;
    }

    private int mode(int[] values) {
        return values.length == 0 ? 0 : values[0];
    }

    // 无参数时重置为整屏
    private void applyScrollRegion(int[] values, TerminalBuffer buffer) {
        if (values.length == 1 && values[0] == 0) {
            buffer.resetScrollRegion();
            buffer.setCursor(0, 0);
            return;
        }
        int top = value(values, 0, 1) - 1;
        int bottom = value(values, 1, buffer.rows()) - 1;
        buffer.setScrollRegion(top, bottom);
    }

    private void repeatCharacter(int count, TerminalBuffer buffer) {
        // 0 表示不重复，用于宏生成的可变长度进度条
        int limit = Math.max(0, Math.min(buffer.cols(), count));
        for (int i = 0; i < limit; i++) {
            buffer.write(lastCharacter, lastStyle);
        }
    }

    // 只处理光标可见性，其余私有模式忽略
    private void applyPrivateMode(int[] values, boolean enable, TerminalBuffer buffer) {
        if (!privateMode) {
            return;
        }
        for (int value : values) {
            if (value == 25) {
                buffer.setCursorVisible(enable);
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
            } else if (code == 2) {
                style = style.withFaint(true);
            } else if (code == 4 || code == 21) {
                style = style.withUnderline(true);
            } else if (code == 5) {
                style = style.withBlink(true);
            } else if (code == 7) {
                style = style.withInverse(true);
            } else if (code == 9) {
                style = style.withStrike(true);
            } else if (code == 22) {
                style = style.withBold(false).withFaint(false);
            } else if (code == 24) {
                style = style.withUnderline(false);
            } else if (code == 25) {
                style = style.withBlink(false);
            } else if (code == 27) {
                style = style.withInverse(false);
            } else if (code == 29) {
                style = style.withStrike(false);
            } else if (code >= 30 && code <= 37) {
                style = style.withForeground(palette.basic(code - 30));
            } else if (code == 39) {
                style = style.withForeground(CellStyle.UNSET);
            } else if (code >= 40 && code <= 47) {
                style = style.withBackground(palette.basic(code - 40));
            } else if (code == 49) {
                style = style.withBackground(CellStyle.UNSET);
            } else if (code >= 90 && code <= 97) {
                style = style.withForeground(palette.basic(code - 90 + 8));
            } else if (code >= 100 && code <= 107) {
                style = style.withBackground(palette.basic(code - 100 + 8));
            } else if ((code == 38 || code == 48) && i + 2 < values.length && values[i + 1] == 5) {
                int color = palette.color256(values[i + 2]);
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
