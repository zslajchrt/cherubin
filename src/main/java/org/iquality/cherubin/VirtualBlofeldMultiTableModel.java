package org.iquality.cherubin;

import javax.swing.table.AbstractTableModel;
import java.util.List;

import static org.iquality.cherubin.VirtualBlofeldModel.BANK_SIZE;

public class VirtualBlofeldMultiTableModel extends AbstractTableModel {
    public static final int COLUMN_SLOT = 0;
    public static final int COLUMN_NAME = 1;

    private final String[] columnNames = {"Slot", "Name"};

    private final List<MultiSound> soundBank;

    private final VirtualBlofeldModel blofeldModel;

    public VirtualBlofeldMultiTableModel(VirtualBlofeldModel blofeldModel, List<MultiSound> soundBank) {
        this.blofeldModel = blofeldModel;
        assert soundBank.size() == BANK_SIZE;
        this.soundBank = soundBank;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return BANK_SIZE;
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
                throw new IllegalArgumentException("Invalid column index");
        }
        return returnValue;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
    }

}
