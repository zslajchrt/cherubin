package org.iquality.cherubin;

import java.awt.*;
import java.awt.event.KeyEvent;

public abstract class ShiftKeyDispatcher implements KeyEventDispatcher {

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if ((e.getID() == KeyEvent.KEY_PRESSED || e.getID() == KeyEvent.KEY_RELEASED) && e.getKeyChar() == KeyEvent.CHAR_UNDEFINED && e.isShiftDown()) {
            onShiftPressed(e);
        } else if (e.getID() == KeyEvent.KEY_RELEASED && !e.isShiftDown()) {
            onShiftReleased(e);
        }
        return false;
    }

    protected abstract void onShiftPressed(KeyEvent e);

    protected abstract void onShiftReleased(KeyEvent e);

}
