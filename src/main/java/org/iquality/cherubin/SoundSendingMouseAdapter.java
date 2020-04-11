package org.iquality.cherubin;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public abstract class SoundSendingMouseAdapter<T> extends MouseAdapter {

    protected abstract T getValueAt(int row, int column);

    protected abstract void sendSound(T sound, AppModel.OutputDirection direction);

    protected abstract void sendSoundOn(T sound);

    protected abstract void sendSoundOff();

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            JTable target = (JTable) e.getSource();
            int row = target.getSelectedRow();
            int column = SoundDbTableModel.COLUMN_NAME;
            T sound = getValueAt(row, column);

            if ((e.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK)) == (InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK)) {
                sendSound(sound, AppModel.OutputDirection.both);
            } else if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK) {
                sendSound(sound, AppModel.OutputDirection.left);
            } else if ((e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) == InputEvent.ALT_DOWN_MASK) {
                sendSound(sound, AppModel.OutputDirection.right);
            } else {
                sendSoundOn(sound);
            }
        }
        if (e.getClickCount() == 1) {
            //tableModel.sendSoundOff();
            sendSoundOff();
        }
    }
}
