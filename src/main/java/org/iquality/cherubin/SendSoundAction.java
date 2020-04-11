package org.iquality.cherubin;

import javax.swing.*;
import java.awt.event.ActionEvent;

public abstract class SendSoundAction extends AbstractAction {

    private final JTable table;
    private final int soundColumn;

    private SingleSound sounding;

    public SendSoundAction(JTable table, int soundColumn) {
        this.table = table;
        this.soundColumn = soundColumn;
    }

    protected abstract void onSound(SingleSound sound, boolean on);

    @Override
    public void actionPerformed(ActionEvent e) {
        JTable target = (JTable) e.getSource();
        int row = target.getSelectedRow();
        int column = SoundDbTableModel.COLUMN_NAME;
        SingleSound selectedSound = (SingleSound) table.getValueAt(row, soundColumn);

        if (sounding == null || selectedSound != sounding) {
            sounding = selectedSound;
            onSound(sounding, true);
        } else {
            onSound(sounding, false);
            sounding = null;
        }
    }
}
