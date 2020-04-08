package org.iquality.cherubin;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

public class VirtualBlofeldTable extends JTable {

    private final VirtualBlofeldTableModel tableModel;

    public VirtualBlofeldTable(VirtualBlofeldTableModel dm) {
        super(dm);

        tableModel = dm;

        TableColumnModel columnModel = getColumnModel();

        columnModel.getColumn(VirtualBlofeldTableModel.COLUMN_SLOT).setPreferredWidth(15);
        columnModel.getColumn(VirtualBlofeldTableModel.COLUMN_NAME).setPreferredWidth(190);
        columnModel.getColumn(VirtualBlofeldTableModel.COLUMN_CATEGORY).setPreferredWidth(25);
        columnModel.getColumn(VirtualBlofeldTableModel.COLUMN_SOUNDSET).setPreferredWidth(100);
        columnModel.getColumn(VirtualBlofeldTableModel.COLUMN_REFID).setPreferredWidth(15);

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setHorizontalAlignment(JLabel.LEFT);
        columnModel.getColumn(0).setCellRenderer(cellRenderer);

    }

}
