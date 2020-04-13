package org.iquality.cherubin;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class SynthTableModel extends AbstractTableModel implements SynthModel.SynthModelListener {
    public static final int COLUMN_SLOT = 0;
    public static final int COLUMN_NAME = 1;
    public static final int COLUMN_CATEGORY = 2;
    public static final int COLUMN_SOUNDSET = 3;
    public static final int COLUMN_REFID = 4;

    private final String[] columnNames = {"Slot", "Name", "Category", "SoundSet", "RefId"};

    private final int bankNum;

    private final SynthModel synthModel;

    public SynthTableModel(SynthModel synthModel, int bankNum) {
        this.synthModel = synthModel;
        this.bankNum = bankNum;
        synthModel.addSynthModelListener(this);
    }

    public SynthModel getSynthModel() {
        return synthModel;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return synthModel.getSynthFactory().getBankSize();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return getValueAt(0, columnIndex).getClass();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Sound sound = getBank().get(rowIndex);
        Object returnValue;
        switch (columnIndex) {
            case COLUMN_SLOT:
                returnValue = rowIndex + 1;
                break;
            case COLUMN_NAME:
                returnValue = sound;
                break;
            case COLUMN_CATEGORY:
                returnValue = sound.getCategory();
                break;
            case COLUMN_SOUNDSET:
                returnValue = sound.getSoundSetName();
                break;
            case COLUMN_REFID:
                returnValue = sound.getId();
                break;
            default:
                throw new IllegalArgumentException("Invalid column index");
        }
        return returnValue;
    }

    private List<Sound> getBank() {
        return synthModel.getBank(bankNum);
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
    }

    public void updateSound(int slot, Sound sound) {
        synthModel.getSynth().updateSound(bankNum, slot, sound);
        fireTableDataChanged();
    }

    @Override
    public void synthChanged(Synth synth) {
        fireTableDataChanged();
    }

    public void deleteSound(int slot) {
        synthModel.getSynth().deleteSound(bankNum, slot);
        fireTableDataChanged();
    }
}
