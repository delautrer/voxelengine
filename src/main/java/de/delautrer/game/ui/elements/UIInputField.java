package de.delautrer.game.ui.elements;

import de.delautrer.engine.graphics.IFont;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.ui.UIMeshBuilder;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.stb.STBTTBakedChar;

public class UIInputField extends UIElement {
    private String text = "";
    private String placeholder;
    private boolean isFocused = false;
    private int maxLength;
    private int cursorIndex = 0;
    private int selectAnchor = -1;

    private static final int GRID_X_NORMAL = 0;
    private static final int GRID_Y_NORMAL = 0;
    private static final int GRID_X_HOVER = 1;
    private static final int GRID_Y_HOVER = 0;
    private static final float CORNER_SIZE = 8.0f;

    public UIInputField(float x, float y, float width, float height, String placeholder, int maxLength) {
        super(x, y, width, height);
        this.placeholder = placeholder;
        this.maxLength = maxLength;
    }

    private void sanitizeIndices() {
        if (text == null) text = "";
        cursorIndex = Math.max(0, Math.min(text.length(), cursorIndex));
        if (selectAnchor != -1) {
            selectAnchor = Math.max(0, Math.min(text.length(), selectAnchor));
            if (selectAnchor == cursorIndex) {
                selectAnchor = -1;
            }
        }
    }

    public void setFocused(boolean focused) {
        this.isFocused = focused;
        if (!focused) {
            this.selectAnchor = -1;
        }
        sanitizeIndices();
    }

    public boolean isFocused() {
        return isFocused;
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
        this.cursorIndex = this.text.length();
        this.selectAnchor = -1;
        this.scrollOffset = 0.0f;
        sanitizeIndices();
    }

    public String getText() {
        return text;
    }

    public boolean hasSelection() {
        sanitizeIndices();
        return selectAnchor != -1 && selectAnchor != cursorIndex;
    }

    public int getSelectionStart() {
        sanitizeIndices();
        if (selectAnchor == -1) return cursorIndex;
        return Math.min(selectAnchor, cursorIndex);
    }

    public int getSelectionEnd() {
        sanitizeIndices();
        if (selectAnchor == -1) return cursorIndex;
        return Math.max(selectAnchor, cursorIndex);
    }

    public String getSelectedText() {
        sanitizeIndices();
        if (!hasSelection()) return "";
        int start = getSelectionStart();
        int end = getSelectionEnd();
        if (start < 0 || end > text.length() || start >= end) return "";
        return text.substring(start, end);
    }

    public void deleteSelection() {
        sanitizeIndices();
        if (hasSelection()) {
            int start = getSelectionStart();
            int end = getSelectionEnd();
            text = text.substring(0, start) + text.substring(end);
            cursorIndex = start;
            selectAnchor = -1;
            sanitizeIndices();
        }
    }

    public void typeChar(char c) {
        if (!isFocused) return;
        if (hasSelection()) {
            deleteSelection();
        }
        if (text.length() < maxLength) {
            text = text.substring(0, cursorIndex) + c + text.substring(cursorIndex);
            cursorIndex++;
            selectAnchor = -1;
        }
    }

    public void backspace() {
        if (!isFocused) return;
        if (hasSelection()) {
            deleteSelection();
        } else if (cursorIndex > 0) {
            text = text.substring(0, cursorIndex - 1) + text.substring(cursorIndex);
            cursorIndex--;
        }
    }

    public void delete() {
        if (!isFocused) return;
        if (hasSelection()) {
            deleteSelection();
        } else if (cursorIndex < text.length()) {
            text = text.substring(0, cursorIndex) + text.substring(cursorIndex + 1);
        }
    }

    private boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private int findWordStartLeft(int pos) {
        if (pos <= 0) return 0;
        int idx = pos - 1;
        while (idx > 0 && !isWordChar(text.charAt(idx))) {
            idx--;
        }
        while (idx > 0 && isWordChar(text.charAt(idx - 1))) {
            idx--;
        }
        return idx;
    }

    private int findWordEndRight(int pos) {
        int len = text.length();
        if (pos >= len) return len;
        int idx = pos;
        while (idx < len && !isWordChar(text.charAt(idx))) {
            idx++;
        }
        while (idx < len && isWordChar(text.charAt(idx))) {
            idx++;
        }
        return idx;
    }

    public void onMouseDown(float mouseX, float mouseY, IFont font, boolean shiftDown) {
        int clickIndex = getCaretIndexAt(mouseX, font);
        if (shiftDown) {
            if (selectAnchor == -1) selectAnchor = cursorIndex;
            cursorIndex = clickIndex;
        } else {
            cursorIndex = clickIndex;
            selectAnchor = clickIndex;
        }
    }

    public void onMouseDrag(float mouseX, float mouseY, IFont font) {
        if (selectAnchor == -1) selectAnchor = cursorIndex;
        cursorIndex = getCaretIndexAt(mouseX, font);
    }

    public void onDoubleClick(float mouseX, float mouseY, IFont font) {
        int clickIndex = getCaretIndexAt(mouseX, font);
        int wordStart = findWordStartLeft(clickIndex + 1);
        int wordEnd = findWordEndRight(clickIndex);
        selectAnchor = wordStart;
        cursorIndex = wordEnd;
    }

    private float scrollOffset = 0.0f;

    public int getCaretIndexAt(float mouseX, IFont font) {
        float relativeX = mouseX - (x + 8.0f) + scrollOffset;
        if (relativeX <= 0 || font == null || font.getCharData() == null || text.isEmpty()) return 0;

        STBTTBakedChar.Buffer charData = font.getCharData();
        float currentX = 0.0f;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            float advance = 0.0f;
            if (c >= 32 && c < 256) {
                advance = charData.get(c - 32).xadvance();
            }
            if (relativeX < currentX + (advance / 2.0f)) {
                return i;
            }
            currentX += advance;
        }
        return text.length();
    }

    @Override
    public void render(UIMeshBuilder builder, IFont font, float mouseX, float mouseY) {
        if (!isVisible) return;
        sanitizeIndices();

        int gridX = isFocused ? GRID_X_HOVER : GRID_X_NORMAL;
        int gridY = isFocused ? GRID_Y_HOVER : GRID_Y_NORMAL;

        builder.add9Slice(x, y, 0.1f, width, height, gridX, gridY, CORNER_SIZE);

        float innerWidth = width - 16.0f;
        float caretX = (font != null && !text.isEmpty()) ? builder.getTextWidth(text.substring(0, cursorIndex), font) : 0.0f;

        if (caretX - scrollOffset > innerWidth) {
            scrollOffset = caretX - innerWidth;
        } else if (caretX - scrollOffset < 0) {
            scrollOffset = caretX;
        }
        if (text.isEmpty()) {
            scrollOffset = 0.0f;
        }
        scrollOffset = Math.max(0.0f, scrollOffset);

        float renderStartX = x + 8.0f - scrollOffset;
        float textY = y + (height / 2.0f) - 10.0f;

        // Clip text & caret within [x + 8, x + width - 8]
        builder.setClipRect(x + 8.0f, y, innerWidth, height);

        // Render Selection Box
        if (hasSelection() && font != null) {
            String leftSub = text.substring(0, getSelectionStart());
            String selSub = text.substring(0, getSelectionEnd());
            float startX = renderStartX + builder.getTextWidth(leftSub, font);
            float endX = renderStartX + builder.getTextWidth(selSub, font);
            builder.addRect(startX, textY - 2.0f, 0.15f, endX - startX, 22.0f, 0.2f, 0.4f, 0.8f, 0.5f);
        }

        // Render text or placeholder
        boolean showPlaceholder = text.isEmpty() && !isFocused;
        String displayText = showPlaceholder ? placeholder : text;
        float colorVal = showPlaceholder ? 0.6f : 1.0f;

        builder.drawText(displayText, renderStartX, textY, 0.2f, font, colorVal, colorVal, colorVal, 1.0f, 1.0f);

        // Render blinking caret (530ms ON / 530ms OFF)
        if (isFocused && (System.currentTimeMillis() % 1060 < 530)) {
            float absoluteCaretX = renderStartX + caretX;
            builder.addRect(absoluteCaretX, textY - 2.0f, 0.25f, 2.0f, 22.0f, 1.0f, 1.0f, 1.0f, 1.0f);
        }

        builder.clearClipRect();
    }

    public void handleInput(InputManager input) {
        if (!isFocused) return;

        for (char c : input.consumeTypedChars()) {
            typeChar(c);
        }

        boolean shift = input.isShiftDown();
        boolean ctrl = input.isControlDown();
        int lastKey = input.consumeLastKey();

        if (lastKey == GLFW.GLFW_KEY_LEFT) {
            int newIndex = ctrl ? findWordStartLeft(cursorIndex) : Math.max(0, cursorIndex - 1);
            if (shift) {
                if (selectAnchor == -1) selectAnchor = cursorIndex;
                cursorIndex = newIndex;
            } else {
                cursorIndex = newIndex;
                selectAnchor = -1;
            }
        } else if (lastKey == GLFW.GLFW_KEY_RIGHT) {
            int newIndex = ctrl ? findWordEndRight(cursorIndex) : Math.min(text.length(), cursorIndex + 1);
            if (shift) {
                if (selectAnchor == -1) selectAnchor = cursorIndex;
                cursorIndex = newIndex;
            } else {
                cursorIndex = newIndex;
                selectAnchor = -1;
            }
        } else if (lastKey == GLFW.GLFW_KEY_HOME) {
            if (shift) {
                if (selectAnchor == -1) selectAnchor = cursorIndex;
                cursorIndex = 0;
            } else {
                cursorIndex = 0;
                selectAnchor = -1;
            }
        } else if (lastKey == GLFW.GLFW_KEY_END) {
            if (shift) {
                if (selectAnchor == -1) selectAnchor = cursorIndex;
                cursorIndex = text.length();
            } else {
                cursorIndex = text.length();
                selectAnchor = -1;
            }
        } else if (lastKey == GLFW.GLFW_KEY_BACKSPACE) {
            if (ctrl) {
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursorIndex > 0) {
                    int wordStart = findWordStartLeft(cursorIndex);
                    text = text.substring(0, wordStart) + text.substring(cursorIndex);
                    cursorIndex = wordStart;
                    selectAnchor = -1;
                }
            } else {
                backspace();
            }
        } else if (lastKey == GLFW.GLFW_KEY_DELETE) {
            delete();
        } else if (ctrl && lastKey == GLFW.GLFW_KEY_A) {
            selectAnchor = 0;
            cursorIndex = text.length();
        } else if (ctrl && lastKey == GLFW.GLFW_KEY_C) {
            String copyText = hasSelection() ? getSelectedText() : text;
            if (!copyText.isEmpty()) {
                input.setClipboardString(copyText);
            }
        } else if (ctrl && lastKey == GLFW.GLFW_KEY_X) {
            if (hasSelection()) {
                input.setClipboardString(getSelectedText());
                deleteSelection();
            }
        } else if (ctrl && lastKey == GLFW.GLFW_KEY_V) {
            String clipboard = input.getClipboardString();
            if (clipboard != null) {
                clipboard = clipboard.replace("\r", "").replace("\n", "");
                if (hasSelection()) {
                    deleteSelection();
                }
                for (char c : clipboard.toCharArray()) {
                    typeChar(c);
                }
            }
        }
    }
}
