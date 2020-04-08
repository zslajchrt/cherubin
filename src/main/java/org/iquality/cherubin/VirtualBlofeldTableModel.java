package org.iquality.cherubin;

import javax.swing.table.AbstractTableModel;
import java.util.List;

import static org.iquality.cherubin.VirtualBlofeldModel.BANK_SIZE;

public class VirtualBlofeldTableModel extends AbstractTableModel {
    public static final int COLUMN_SLOT = 0;
    public static final int COLUMN_NAME = 1;
    public static final int COLUMN_CATEGORY = 2;
    public static final int COLUMN_SOUNDSET = 3;
    public static final int COLUMN_REFID = 4;

    private final String[] columnNames = {"Slot", "Name", "Category", "SoundSet", "RefId"};

    private final List<SingleSound> soundBank;

    private final VirtualBlofeldModel blofeldModel;

    public VirtualBlofeldTableModel(VirtualBlofeldModel blofeldModel, List<SingleSound> soundBank) {
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
        SingleSound sound = soundBank.get(rowIndex);
        Object returnValue;
        switch (columnIndex) {
            case COLUMN_SLOT:
                returnValue = rowIndex + 1;
                break;
            case COLUMN_NAME:
                returnValue = sound;
                break;
            case COLUMN_CATEGORY:
                returnValue = sound.category;
                break;
            case COLUMN_SOUNDSET:
                returnValue = sound.soundSetName;
                break;
            case COLUMN_REFID:
                returnValue = sound.id;
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
