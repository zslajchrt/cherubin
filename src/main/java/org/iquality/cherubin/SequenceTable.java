package org.iquality.cherubin;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.swing.*;
import javax.swing.table.TableColumnModel;

public class SequenceTable extends JTable {

    private final SequenceTableModel sequenceTableModel;

    public SequenceTable(SequenceTableModel dm) {
        super(dm);

        this.sequenceTableModel = dm;

        TableColumnModel columnModel = getColumnModel();

        columnModel.getColumn(SequenceTableModel.COLUMN_TIMESTAMP).setPreferredWidth(25);
        columnModel.getColumn(SequenceTableModel.COLUMN_STATUS).setPreferredWidth(15);
        columnModel.getColumn(SequenceTableModel.COLUMN_LENGTH).setPreferredWidth(15);
        columnModel.getColumn(SequenceTableModel.COLUMN_DESCRIPTION).setPreferredWidth(200);

        addMouseListener(new SoundSendingMouseAdapter<MidiMessage>() {
            @Override
            protected MidiMessage getValueAt(int row, int column) {
                MidiEvent ev = sequenceTableModel.getTrack().get(row);
                return ev.getMessage();
            }

            @Override
            protected void sendSound(MidiMessage message, AppModel.OutputDirection direction) {
                sequenceTableModel.sendMidiMessage(message, direction);
            }

            @Override
            protected void sendSoundOn(MidiMessage message) {
                sequenceTableModel.sendMidiMessage(message, AppModel.OutputDirection.def);
            }

            @Override
            protected void sendSoundOff() {
            }
        });

    }
}
