package org.iquality.cherubin;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

public class SynthMultiTable extends JTable {

    private final SynthMultiTableModel tableModel;

    public SynthMultiTable(SynthMultiTableModel dm) {
        super(dm);

        tableModel = dm;

        TableColumnModel columnModel = getColumnModel();

        columnModel.getColumn(SynthMultiTableModel.COLUMN_SLOT).setPreferredWidth(35);
        columnModel.getColumn(SynthMultiTableModel.COLUMN_NAME).setPreferredWidth(190);
        for (int i = 0; i < 16; i++) {
            columnModel.getColumn(SynthMultiTableModel.COLUMN_SLOT_REF + i).setPreferredWidth(20);
        }

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setHorizontalAlignment(JLabel.LEFT);
        columnModel.getColumn(SynthTableModel.COLUMN_SLOT).setCellRenderer(cellRenderer);

        DefaultTableCellRenderer soundNameSellRenderer = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText(((MultiSound) value).getName());
            }
        };
        columnModel.getColumn(SynthTableModel.COLUMN_NAME).setCellRenderer(soundNameSellRenderer);

        tableModel.getSynthModel().installTableBehavior(this, SynthTableModel.COLUMN_NAME);

    }

}
