package com.xutrain.windowxpp.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// 固定行列的字符网格，负责光标移动与滚动
public final class TerminalBuffer {
    public record Cell(char character, CellStyle style) {
        static final Cell EMPTY = new Cell(' ', CellStyle.DEFAULT);
    }

    // 宽字符第二格的占位标记
    private static final char CONTINUATION = '\u0000';

    private int cols;
    private int rows;
    private char[] characters;
    private CellStyle[] styles;
    private int cursorX;
    private int cursorY;

    public TerminalBuffer(int cols, int rows) {
        this.cols = Math.max(1, cols);
        this.rows = Math.max(1, rows);
        this.characters = new char[this.cols * this.rows];
        this.styles = new CellStyle[this.cols * this.rows];
        clear();
    }

    public synchronized int cols() {
        return cols;
    }

    public synchronized int rows() {
        return rows;
    }

    public synchronized int cursorX() {
        return cursorX;
    }

    public synchronized int cursorY() {
        return cursorY;
    }

    public synchronized void clear() {
        Arrays.fill(characters, ' ');
        Arrays.fill(styles, CellStyle.DEFAULT);
        cursorX = 0;
        cursorY = 0;
    }

    public synchronized void write(char character, CellStyle style) {
        if (character == '\r') {
            cursorX = 0;
            return;
        }
        if (character == '\b') {
            if (cursorX > 0) {
                cursorX--;
            }
            return;
        }
        if (character == '\n') {
            // 按 processed output 处理，换行同时回到行首
            cursorX = 0;
            newline();
            return;
        }
        // 中文等宽字符占两格，第二格留占位符
        int cellWidth = isWide(character) ? 2 : 1;
        if (cursorX + cellWidth > cols) {
            cursorX = 0;
            newline();
        }
        characters[index(cursorX, cursorY)] = character;
        styles[index(cursorX, cursorY)] = style;
        if (cellWidth == 2 && cursorX + 1 < cols) {
            characters[index(cursorX + 1, cursorY)] = CONTINUATION;
            styles[index(cursorX + 1, cursorY)] = style;
        }
        cursorX += cellWidth;
        if (cursorX >= cols) {
            cursorX = 0;
            newline();
        }
    }

    public static boolean isContinuation(char character) {
        return character == CONTINUATION;
    }

    public static boolean isWide(char character) {
        return (character >= 0x1100 && character <= 0x115F)
                || (character >= 0x2E80 && character <= 0xA4CF)
                || (character >= 0xAC00 && character <= 0xD7A3)
                || (character >= 0xF900 && character <= 0xFAFF)
                || (character >= 0xFE30 && character <= 0xFE6F)
                || (character >= 0xFF00 && character <= 0xFF60)
                || (character >= 0xFFE0 && character <= 0xFFE6);
    }

    public synchronized void carriageReturn() {
        cursorX = 0;
    }

    public synchronized void newline() {
        cursorY++;
        if (cursorY >= rows) {
            scrollUp();
            cursorY = rows - 1;
        }
    }

    public synchronized void backspace() {
        if (cursorX > 0) {
            cursorX--;
            characters[index(cursorX, cursorY)] = ' ';
            styles[index(cursorX, cursorY)] = CellStyle.DEFAULT;
        }
    }

    public synchronized void clearLine() {
        for (int x = cursorX; x < cols; x++) {
            characters[index(x, cursorY)] = ' ';
            styles[index(x, cursorY)] = CellStyle.DEFAULT;
        }
    }

    public synchronized void clearScreen() {
        Arrays.fill(characters, ' ');
        Arrays.fill(styles, CellStyle.DEFAULT);
        cursorX = 0;
        cursorY = 0;
    }

    public synchronized void moveCursor(int deltaX, int deltaY) {
        cursorX = Math.max(0, Math.min(cols - 1, cursorX + deltaX));
        cursorY = Math.max(0, Math.min(rows - 1, cursorY + deltaY));
    }

    public synchronized void setCursor(int x, int y) {
        cursorX = Math.max(0, Math.min(cols - 1, x));
        cursorY = Math.max(0, Math.min(rows - 1, y));
    }

    public synchronized Cell cellAt(int x, int y) {
        if (x < 0 || x >= cols || y < 0 || y >= rows) {
            return Cell.EMPTY;
        }
        int index = index(x, y);
        return new Cell(characters[index], styles[index]);
    }

    // 尺寸变化时按逻辑行重排，保证缩放后文字仍对得上位置
    public synchronized void resize(int newCols, int newRows) {
        int targetCols = Math.max(1, newCols);
        int targetRows = Math.max(1, newRows);
        if (targetCols == cols && targetRows == rows) {
            return;
        }

        List<List<Cell>> sourceLines = new ArrayList<>();
        for (int y = 0; y < rows; y++) {
            int end = cols;
            while (end > 0 && characters[y * cols + end - 1] == ' ') {
                end--;
            }
            List<Cell> line = new ArrayList<>(end);
            for (int x = 0; x < end; x++) {
                char character = characters[y * cols + x];
                // 占位格跟随前一个宽字符，重排时不再单独计入
                if (character == CONTINUATION) {
                    continue;
                }
                line.add(new Cell(character, styles[y * cols + x]));
            }
            sourceLines.add(line);
        }

        List<List<Cell>> wrapped = new ArrayList<>();
        for (List<Cell> line : sourceLines) {
            List<Cell> current = new ArrayList<>();
            int used = 0;
            for (Cell cell : line) {
                int width = isWide(cell.character()) ? 2 : 1;
                if (used + width > targetCols) {
                    wrapped.add(current);
                    current = new ArrayList<>();
                    used = 0;
                }
                current.add(cell);
                used += width;
            }
            wrapped.add(current);
        }

        char[] newCharacters = new char[targetCols * targetRows];
        CellStyle[] newStyles = new CellStyle[targetCols * targetRows];
        Arrays.fill(newCharacters, ' ');
        Arrays.fill(newStyles, CellStyle.DEFAULT);

        int firstLine = Math.max(0, wrapped.size() - targetRows);
        for (int y = 0; y < targetRows; y++) {
            int sourceIndex = firstLine + y;
            if (sourceIndex >= wrapped.size()) {
                break;
            }
            List<Cell> line = wrapped.get(sourceIndex);
            int px = 0;
            for (Cell cell : line) {
                if (px >= targetCols) {
                    break;
                }
                int width = isWide(cell.character()) ? 2 : 1;
                newCharacters[y * targetCols + px] = cell.character();
                newStyles[y * targetCols + px] = cell.style();
                if (width == 2 && px + 1 < targetCols) {
                    newCharacters[y * targetCols + px + 1] = CONTINUATION;
                    newStyles[y * targetCols + px + 1] = cell.style();
                }
                px += width;
            }
        }

        characters = newCharacters;
        styles = newStyles;
        cols = targetCols;
        rows = targetRows;

        if (wrapped.isEmpty()) {
            cursorX = 0;
            cursorY = 0;
        } else {
            int lastIndex = Math.min(wrapped.size() - 1, firstLine + targetRows - 1);
            cursorY = Math.min(rows - 1, lastIndex - firstLine);
            cursorX = Math.min(cols - 1, wrapped.get(lastIndex).size());
        }
    }

    private void scrollUp() {
        System.arraycopy(characters, cols, characters, 0, cols * (rows - 1));
        System.arraycopy(styles, cols, styles, 0, cols * (rows - 1));
        Arrays.fill(characters, cols * (rows - 1), cols * rows, ' ');
        Arrays.fill(styles, cols * (rows - 1), cols * rows, CellStyle.DEFAULT);
    }

    private int index(int x, int y) {
        return y * cols + x;
    }
}
