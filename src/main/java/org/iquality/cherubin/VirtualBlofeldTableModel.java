package org.iquality.cherubin;

import javax.swing.table.AbstractTableModel;
import java.util.List;

import static org.iquality.cherubin.VirtualBlofeldModel.BANK_SIZE;

public class VirtualBlofeldTableModel extends AbstractTableModel implements VirtualBlofeldModel.BlofeldListener {
    public static final int COLUMN_SLOT = 0;
    public static final int COLUMN_NAME = 1;
    public static final int COLUMN_CATEGORY = 2;
    public static final int COLUMN_SOUNDSET = 3;
    public static final int COLUMN_REFID = 4;

    private final String[] columnNames = {"Slot", "Name", "Category", "SoundSet", "RefId"};

    private final int bankNum;

    private final VirtualBlofeldModel blofeldModel;

    public VirtualBlofeldTableModel(VirtualBlofeldModel blofeldModel, int bankNum) {
        this.blofeldModel = blofeldModel;
        this.bankNum = bankNum;
        blofeldModel.addBlofeldListener(this);
    }

    public VirtualBlofeldModel getBlofeldModel() {
        return blofeldModel;
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
        SingleSound sound = getBank().get(rowIndex);
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
                returnValue = sound.id;
                break;
            default:
                throw new IllegalArgumentException("Invalid column index");
        }
        return returnValue;
    }

    private List<SingleSound> getBank() {
        return blofeldModel.getBank(bankNum);
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
    }

    public void updateSound(int slot, SingleSound sound) {
        blofeldModel.getBlofeld().updateSound(bankNum, slot, sound);
        fireTableDataChanged();
    }

    @Override
    public void blofeldChanged(VirtualBlofeld blofeld) {
        fireTableDataChanged();
    }

    public void deleteSound(int slot) {
        blofeldModel.getBlofeld().deleteSound(bankNum, slot);
        fireTableDataChanged();
    }

    public void sendSoundOn(SingleSound sound) {
        blofeldModel.sendSoundOn(sound);
    }

    public void sendSoundOff() {
        blofeldModel.sendSoundOff();
    }

    public void sendSound(SingleSound sound, AppModel.OutputDirection outputDirection) {
        blofeldModel.sendSound(sound, outputDirection);
    }
}
