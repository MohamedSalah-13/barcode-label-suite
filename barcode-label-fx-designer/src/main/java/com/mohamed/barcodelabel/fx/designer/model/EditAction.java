package com.mohamed.barcodelabel.fx.designer.model;

/** What an undoable edit did, so a UI can name the undo and redo steps in its own language. */
public enum EditAction {
    ADD, DELETE, DUPLICATE, PASTE, MOVE, RESIZE, CHANGE_BOUNDS, CHANGE_CONTENT, CHANGE_STATE, ROTATE,
    CHANGE_TEXT_STYLE, CHANGE_IMAGE, BRING_TO_FRONT, SEND_TO_BACK, BRING_FORWARD, SEND_BACKWARD, ALIGN, CHANGE_LAYOUT
}
