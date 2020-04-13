package org.iquality.cherubin;

import javax.sound.midi.*;
import javax.swing.table.AbstractTableModel;
import java.io.File;

public class SequenceTableModel extends AbstractTableModel {
    public static final int COLUMN_TIMESTAMP = 0;
    public static final int COLUMN_STATUS = 1;
    public static final int COLUMN_LENGTH = 2;
    public static final int COLUMN_DESCRIPTION = 3;

    private final String[] columnNames = {"Timestamp", "Status", "Length", "Description"};

    private final SequenceModel sequenceModel;
    private final int trackNumber;

    public SequenceTableModel(SequenceModel sequenceModel, int trackNumber) {
        this.sequenceModel = sequenceModel;
        this.trackNumber = trackNumber;
    }

    public Track getTrack() {
        return sequenceModel.getSequence().getTracks()[trackNumber];
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return getTrack().size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (getTrack().size() == 0) {
            return Object.class;
        }
        return getValueAt(0, columnIndex).getClass();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        MidiEvent ev = getTrack().get(rowIndex);
        Object returnValue;
        switch (columnIndex) {
            case COLUMN_TIMESTAMP:
                returnValue = ev.getTick();
                break;
            case COLUMN_STATUS:
                returnValue = ev.getMessage().getStatus();
                break;
            case COLUMN_LENGTH:
                returnValue = ev.getMessage().getLength();
                break;
            case COLUMN_DESCRIPTION:
                returnValue = ev;
                break;
//                if (ev.getMessage() instanceof ShortMessage) {
//                    ShortMessage sm = (ShortMessage) ev.getMessage();
//                    return String.format("%s: %02X %02X %02X", sm.getClass().getSimpleName(), sm.getStatus(), sm.getData1(), sm.getData2());
//                } else {
//                    return String.format("%s: %02X", ev.getMessage().getClass().getSimpleName(), ev.getMessage().getStatus());
//                }
            default:
                throw new IllegalArgumentException("Invalid column index");
        }
        return returnValue;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
    }

    public void sendMidiMessage(MidiMessage message, AppModel.OutputDirection direction) {
        sequenceModel.sendMidiMessage(message, direction);
    }
}
