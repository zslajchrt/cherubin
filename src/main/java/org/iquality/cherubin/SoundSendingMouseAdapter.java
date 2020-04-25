package org.iquality.cherubin;

import javax.swing.*;
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
        if (e.getClickCount() == 2) {
            JTable target = (JTable) e.getSource();
            int row = target.getSelectedRow();
            this.sound = getValueAt(row);
            outputVariant = MidiDeviceManager.getOutputVariant(e.getModifiersEx());

            sendSoundOn(sound, outputVariant);
        }
        if (e.getClickCount() == 1) {
            if (sound != null) {
                sendSoundOff(sound, outputVariant);
                sound = null;
                outputVariant = -1;
            }
        }
    }
}
