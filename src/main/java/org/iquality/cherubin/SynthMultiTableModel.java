package org.iquality.cherubin;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.List;

public class SynthMultiTableModel extends AbstractTableModel {
    public static final int COLUMN_SLOT = 0;
    public static final int COLUMN_NAME = 1;
    public static final int COLUMN_SLOT_REF = 2;

    private final String[] columnNames;
    private final List<MultiSound> soundBank;
    private final SynthModel synthModel;
    private final int multiSlotCount;

    public SynthMultiTableModel(SynthModel synthModel, List<MultiSound> soundBank) {
        this.synthModel = synthModel;
        this.soundBank = soundBank;
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
                if (slotIdx > multiSlotCount || slotIdx < 0) {
                    throw new IllegalArgumentException("Invalid column index");
                }
                returnValue = sound.getSlotRefs().get(slotIdx);
        }
        return returnValue;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (columnIndex < multiSlotCount && columnIndex >= 0) {

            String refStr = value.toString();
            boolean isValid = refStr.length() >= 2 && Character.isLetter(refStr.charAt(0));

            if (refStr.length() >= 2 && Character.isLetter(refStr.charAt(0))) {
                try {
                    int program = Integer.parseInt(refStr.substring(1)) - 1;
                    int bank = refStr.charAt(0) - 'A';

                    MultiSound sound = soundBank.get(rowIndex);
                    int slotIdx = columnIndex - 2;
                    MultiSound.SlotRef slotRef = sound.getSlotRefs().get(slotIdx);
                    slotRef.setRef(bank, program);

                    synthModel.updateMultiSound(sound);
                    return;
                } catch (NumberFormatException e) {
                    isValid = false;
                }
            }

            if (!isValid) {
                JOptionPane.showMessageDialog(null, "Invalid Bank/Program reference " + refStr + " (Valid examples: A1, B22, C123)", "Error", JOptionPane.ERROR_MESSAGE);
            }

        }
    }

}
