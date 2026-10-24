package com.xutrain.windowxpp.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// 固定行列的字符网格，负责光标移动、擦除与滚动
public final class TerminalBuffer {
    public record Cell(char character, CellStyle style) {
        static final Cell EMPTY = new Cell(' ', CellStyle.DEFAULT);
    }

    // 宽字符第二格的占位标记
    private static final char CONTINUATION = '\u0000';
    private static final int TAB_WIDTH = 8;

    private int cols;
    private int rows;
    private char[] characters;
    private CellStyle[] styles;
    private int cursorX;
    private int cursorY;
    private int savedX;
    private int savedY;
    private int scrollTop;
    private int scrollBottom;
    private boolean cursorVisible = true;

    public TerminalBuffer(int cols, int rows) {
        this.cols = Math.max(1, cols);
        this.rows = Math.max(1, rows);
        this.characters = new char[this.cols * this.rows];
        this.styles = new CellStyle[this.cols * this.rows];
        this.scrollBottom = this.rows - 1;
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
        scrollTop = 0;
        scrollBottom = rows - 1;
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
        if (character == '\t') {
            tab();
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

    public synchronized boolean cursorVisible() {
        return cursorVisible;
    }

    public synchronized void setCursorVisible(boolean visible) {
        this.cursorVisible = visible;
    }

    public synchronized void saveCursor() {
        savedX = cursorX;
        savedY = cursorY;
    }

    public synchronized void restoreCursor() {
        cursorX = Math.max(0, Math.min(cols - 1, savedX));
        cursorY = Math.max(0, Math.min(rows - 1, savedY));
    }

    public synchronized void tab() {
        int next = ((cursorX / TAB_WIDTH) + 1) * TAB_WIDTH;
        if (next >= cols) {
            cursorX = 0;
            newline();
            return;
        }
        cursorX = next;
    }

    public synchronized void newline() {
        cursorY++;
        if (cursorY > scrollBottom) {
            scrollRegionUp(1);
            cursorY = scrollBottom;
        }
        cursorY = Math.min(cursorY, rows - 1);
    }

    public synchronized void backspace() {
        if (cursorX > 0) {
            cursorX--;
            clearCellAtCursor();
            return;
        }
        // 光标在行首时回退到上一行末尾，删除跨行的输入
        if (cursorY > 0) {
            cursorY--;
            cursorX = cols - 1;
            while (cursorX > 0 && characters[index(cursorX, cursorY)] == CONTINUATION) {
                cursorX--;
            }
            clearCellAtCursor();
        }
    }

    private void clearCellAtCursor() {
        characters[index(cursorX, cursorY)] = ' ';
        styles[index(cursorX, cursorY)] = CellStyle.DEFAULT;
    }

    // 设置滚动区域，边界为 0 基且自动排序
    public synchronized void setScrollRegion(int top, int bottom) {
        int start = Math.max(0, Math.min(rows - 1, top));
        int end = Math.max(0, Math.min(rows - 1, bottom));
        if (start >= end) {
            return;
        }
        scrollTop = start;
        scrollBottom = end;
        cursorX = 0;
        cursorY = start;
    }

    public synchronized void resetScrollRegion() {
        scrollTop = 0;
        scrollBottom = rows - 1;
    }

    // 1 基行号，供查询展示
    public synchronized String scrollRegion() {
        return (scrollTop + 1) + "-" + (scrollBottom + 1);
    }

    // mode 0 光标到末尾，1 开头到光标，2 整屏
    public synchronized void eraseDisplay(int mode) {
        switch (mode) {
            case 0 -> {
                Arrays.fill(characters, index(cursorX, cursorY), characters.length, ' ');
                Arrays.fill(styles, index(cursorX, cursorY), styles.length, CellStyle.DEFAULT);
            }
            case 1 -> {
                int end = index(cursorX, cursorY) + 1;
                Arrays.fill(characters, 0, end, ' ');
                Arrays.fill(styles, 0, end, CellStyle.DEFAULT);
            }
            default -> {
                Arrays.fill(characters, ' ');
                Arrays.fill(styles, CellStyle.DEFAULT);
            }
        }
    }

    // mode 0 光标到行尾，1 行首到光标，2 整行
    public synchronized void eraseLine(int mode) {
        int rowStart = cursorY * cols;
        switch (mode) {
            case 0 -> {
                Arrays.fill(characters, index(cursorX, cursorY), rowStart + cols, ' ');
                Arrays.fill(styles, index(cursorX, cursorY), rowStart + cols, CellStyle.DEFAULT);
            }
            case 1 -> {
                Arrays.fill(characters, rowStart, index(cursorX, cursorY) + 1, ' ');
                Arrays.fill(styles, rowStart, index(cursorX, cursorY) + 1, CellStyle.DEFAULT);
            }
            default -> {
                Arrays.fill(characters, rowStart, rowStart + cols, ' ');
                Arrays.fill(styles, rowStart, rowStart + cols, CellStyle.DEFAULT);
            }
        }
    }

    // 擦除光标右侧 n 个字符，光标不动
    public synchronized void eraseCharacters(int count) {
        int amount = Math.max(1, Math.min(cols - cursorX, count));
        int from = index(cursorX, cursorY);
        Arrays.fill(characters, from, from + amount, ' ');
        Arrays.fill(styles, from, from + amount, CellStyle.DEFAULT);
    }

    // 删除光标右侧 n 个字符，后面内容左移
    public synchronized void deleteCharacters(int count) {
        int amount = Math.max(1, Math.min(cols - cursorX, count));
        int from = index(cursorX, cursorY);
        int rowEnd = cursorY * cols + cols;
        System.arraycopy(characters, from + amount, characters, from, rowEnd - from - amount);
        System.arraycopy(styles, from + amount, styles, from, rowEnd - from - amount);
        Arrays.fill(characters, rowEnd - amount, rowEnd, ' ');
        Arrays.fill(styles, rowEnd - amount, rowEnd, CellStyle.DEFAULT);
    }

    // 在光标处插入 n 个空格，右侧内容右移
    public synchronized void insertCharacters(int count) {
        int amount = Math.max(1, Math.min(cols - cursorX, count));
        int from = index(cursorX, cursorY);
        int rowEnd = cursorY * cols + cols;
        System.arraycopy(characters, from, characters, from + amount, rowEnd - from - amount);
        System.arraycopy(styles, from, styles, from + amount, rowEnd - from - amount);
        Arrays.fill(characters, from, from + amount, ' ');
        Arrays.fill(styles, from, from + amount, CellStyle.DEFAULT);
    }

    public synchronized void insertLines(int count) {
        int amount = Math.max(1, Math.min(rows - cursorY, count));
        int from = cursorY * cols;
        int shift = amount * cols;
        System.arraycopy(characters, from, characters, from + shift, cols * (rows - cursorY - amount));
        System.arraycopy(styles, from, styles, from + shift, cols * (rows - cursorY - amount));
        Arrays.fill(characters, from, from + shift, ' ');
        Arrays.fill(styles, from, from + shift, CellStyle.DEFAULT);
    }

    public synchronized void deleteLines(int count) {
        int amount = Math.max(1, Math.min(rows - cursorY, count));
        int from = cursorY * cols;
        int shift = amount * cols;
        System.arraycopy(characters, from + shift, characters, from, cols * (rows - cursorY - amount));
        System.arraycopy(styles, from + shift, styles, from, cols * (rows - cursorY - amount));
        Arrays.fill(characters, cols * (rows - amount), cols * rows, ' ');
        Arrays.fill(styles, cols * (rows - amount), cols * rows, CellStyle.DEFAULT);
    }

    public synchronized void scrollRegionUp(int count) {
        int height = scrollBottom - scrollTop + 1;
        int amount = Math.max(1, Math.min(height, count));
        int from = scrollTop * cols;
        int to = (scrollBottom + 1) * cols;
        int shift = amount * cols;
        System.arraycopy(characters, from + shift, characters, from, to - from - shift);
        System.arraycopy(styles, from + shift, styles, from, to - from - shift);
        Arrays.fill(characters, to - shift, to, ' ');
        Arrays.fill(styles, to - shift, to, CellStyle.DEFAULT);
    }

    public synchronized void scrollRegionDown(int count) {
        int height = scrollBottom - scrollTop + 1;
        int amount = Math.max(1, Math.min(height, count));
        int from = scrollTop * cols;
        int to = (scrollBottom + 1) * cols;
        int shift = amount * cols;
        System.arraycopy(characters, from, characters, from + shift, to - from - shift);
        System.arraycopy(styles, from, styles, from + shift, to - from - shift);
        Arrays.fill(characters, from, from + shift, ' ');
        Arrays.fill(styles, from, from + shift, CellStyle.DEFAULT);
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
        scrollTop = 0;
        scrollBottom = rows - 1;

        if (wrapped.isEmpty()) {
            cursorX = 0;
            cursorY = 0;
        } else {
            int lastIndex = Math.min(wrapped.size() - 1, firstLine + targetRows - 1);
            cursorY = Math.min(rows - 1, lastIndex - firstLine);
            cursorX = Math.min(cols - 1, wrapped.get(lastIndex).size());
        }
    }

    private int index(int x, int y) {
        return y * cols + x;
    }
}
