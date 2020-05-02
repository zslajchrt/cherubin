package org.iquality.cherubin;

import javax.swing.*;
import java.awt.event.ActionEvent;

public abstract class SendSoundAction extends AbstractAction {

    private final JTable table;
    private final int soundColumn;

    private Sound sounding;
    private int outputVariant;

    public SendSoundAction(JTable table, int soundColumn) {
        this.table = table;
        this.soundColumn = soundColumn;
    }

    protected abstract void onSound(Sound sound, int outputVariant, boolean on);

    @Override
    public void actionPerformed(ActionEvent e) {
        JTable target = (JTable) e.getSource();
        int row = target.getSelectedRow();
        Sound selectedSound = (Sound) table.getValueAt(row, soundColumn);

        if (sounding == null || selectedSound != sounding) {
            sounding = selectedSound;
            outputVariant = MidiDeviceManager.getOutputVariant(e);
            onSound(sounding, outputVariant, true);
        } else {
            onSound(sounding, outputVariant, false);
            sounding = null;
            outputVariant = -1;
        }
    }
}
