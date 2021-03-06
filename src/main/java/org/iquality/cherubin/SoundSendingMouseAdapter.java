package org.iquality.cherubin;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public abstract class SoundSendingMouseAdapter<T> extends MouseAdapter {

    private int outputVariant = -1;
    private T sound = null;

    protected abstract T getValueAt(int row);

    protected abstract void sendSoundOn(T sound, int outputVariant);

    protected abstract void sendSoundOff(T sound, int outputVariant);

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && !e.isConsumed()) {
            if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
                e.consume();

                JTable target = (JTable) e.getSource();
                int row = target.getSelectedRow();
                this.sound = getValueAt(row);
                outputVariant = MidiDeviceManager.getOutputVariant(e);

                sendSoundOn(sound, outputVariant);
            }
        }
        if (e.getClickCount() == 1) {
            if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
                if (sound != null) {
                    e.consume();
                    sendSoundOff(sound, outputVariant);
                    sound = null;
                    outputVariant = -1;
                }
            }
        }
    }
}
