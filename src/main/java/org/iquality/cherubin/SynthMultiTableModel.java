package org.iquality.cherubin;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.List;

public class SynthMultiTableModel extends AbstractTableModel {
    public static final int COLUMN_SLOT = 0;
    public static final int COLUMN_NAME = 1;
    public static final int COLUMN_SLOT_REF = 2;

    private final String[] columnNames;
    private final SynthModel synthModel;
    private final int multiSlotCount;

    public SynthMultiTableModel(SynthModel synthModel) {
        this.synthModel = synthModel;
        this.multiSlotCount = synthModel.getSynth().getSynthFactory().getMultiSlotCount();

        columnNames = new String[2 + multiSlotCount];
        columnNames[0] = "Slot";
        columnNames[1] = "Name";
        for (int i = 0; i < multiSlotCount; i++) {
            columnNames[i + 2] = "" + (i + 1);
        }
    }

    public int getMultiSlotCount() {
        return multiSlotCount;
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
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case COLUMN_SLOT:
                return false;
            case COLUMN_NAME:
                return true;
            default:
                int slotIdx = columnIndex - COLUMN_SLOT_REF;
                return slotIdx <= 16 && slotIdx >= 0;
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        MultiSound sound = getMultiBank().get(rowIndex);
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
                if (slotIdx > multiSlotCount || slotIdx < 0) {
                    throw new IllegalArgumentException("Invalid column index");
                }
                returnValue = sound.getSlotRefs().get(slotIdx);
        }
        return returnValue;
    }

    private List<MultiSound> getMultiBank() {
        return getSynthModel().getSynth().getMulti();
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (columnIndex == COLUMN_NAME) {
            MultiSound sound = getMultiBank().get(rowIndex);
            sound.setName((String) value);
        } else {
            int slotIdx = columnIndex - 2;
            if (slotIdx < multiSlotCount && slotIdx >= 0) {

                String refStr = value.toString();
                boolean isValid = refStr.length() >= 2 && Character.isLetter(refStr.charAt(0));
                if (isValid) {
                    try {
                        int program = Integer.parseInt(refStr.substring(1)) - 1;
                        int bank = refStr.charAt(0) - 'A';

                        MultiSound sound = getMultiBank().get(rowIndex);
                        SoundSlotRef soundSlotRef = sound.getSlotRefs().get(slotIdx);
                        soundSlotRef.setRef(bank, program);

                        synthModel.updateMultiSound(sound);
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(null, "Invalid Bank/Program reference " + refStr + " (Valid examples: A1, B22, C123)", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }

            }
        }
    }

    public void deleteSound(int slot) {
        synthModel.getSynth().deleteMultiSound(slot);
        fireTableDataChanged();
    }

}
