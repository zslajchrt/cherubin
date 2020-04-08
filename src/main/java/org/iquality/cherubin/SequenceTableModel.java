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

    private final AppModel appModel;
    private Sequence sequence;

    public SequenceTableModel(AppModel appModel) {
        this.appModel = appModel;
        this.sequence = appModel.createSequenceWithOneTrack();
    }

    public Track getTrack() {
        return sequence.getTracks()[0];
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
                returnValue = ""; // TODO
                break;
            default:
                throw new IllegalArgumentException("Invalid column index");
        }
        return returnValue;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
    }

    public void setSequence(Sequence sequence) {
        this.sequence = sequence;
        fireTableDataChanged();
    }

    public void loadSequence(File file) {
        sequence = appModel.loadSequence(file);
        fireTableDataChanged();
    }

    public void saveSequence(File file) {
        appModel.saveSequence(sequence, file);
    }

    public void clearSequence() {
        sequence = appModel.createSequenceWithOneTrack();
        fireTableDataChanged();
    }

    public void recordSequence(Receiver listener) {
        appModel.recordSequence(sequence, listener);
    }

    public void stopRecordingSequence() {
        appModel.stopRecordingSequence();
        fireTableDataChanged();
    }

    public void playSequence(Receiver listener) {
        appModel.playSequence(sequence, listener);
    }

    public void stopPlayingSequence() {
        appModel.stopPlayingSequence();
    }

}
