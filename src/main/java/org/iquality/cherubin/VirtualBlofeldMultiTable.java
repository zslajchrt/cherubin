package org.iquality.cherubin;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

public class VirtualBlofeldMultiTable extends JTable {

    private final VirtualBlofeldMultiTableModel tableModel;

    public VirtualBlofeldMultiTable(VirtualBlofeldMultiTableModel dm) {
        super(dm);

        tableModel = dm;

        TableColumnModel columnModel = getColumnModel();

        columnModel.getColumn(VirtualBlofeldTableModel.COLUMN_SLOT).setPreferredWidth(15);
        columnModel.getColumn(VirtualBlofeldTableModel.COLUMN_NAME).setPreferredWidth(190);

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setHorizontalAlignment(JLabel.LEFT);
        columnModel.getColumn(0).setCellRenderer(cellRenderer);

    }

}
