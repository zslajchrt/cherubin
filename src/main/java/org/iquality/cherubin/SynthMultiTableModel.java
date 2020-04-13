package org.iquality.cherubin;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class SynthMultiTableModel extends AbstractTableModel {
    public static final int COLUMN_SLOT = 0;
    public static final int COLUMN_NAME = 1;
    public static final int COLUMN_SLOT_REF = 2;

    private static final String[] columnNames = new String[18];

    static {
        columnNames[0] = "Slot";
        columnNames[1] = "Name";
        for (int i = 1; i <= 16; i++) {
            columnNames[i + 1] = "" + i;
        }
    }

    private final List<MultiSound> soundBank;

    private final SynthModel synthModel;

    public SynthMultiTableModel(SynthModel synthModel, List<MultiSound> soundBank) {
        this.synthModel = synthModel;
        this.soundBank = soundBank;
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
        return synthModel.getSynthFactory().getMultiBankSize();
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
        MultiSound sound = soundBank.get(rowIndex);
        Object returnValue;
        switch (columnIndex) {
            case COLUMN_SLOT:
                returnValue = rowIndex + 1;
                break;
            case COLUMN_NAME:
                returnValue = sound;
                break;
            default:
                int slotIdx = columnIndex - COLUMN_SLOT_REF;
                if (slotIdx > 16 || slotIdx < 0) {
                    throw new IllegalArgumentException("Invalid column index");
                }
                returnValue = sound.getSlotRefs()[slotIdx];
        }
        return returnValue;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
    }

}
