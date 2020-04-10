package org.iquality.cherubin;

import javax.swing.*;
import javax.swing.table.TableColumnModel;

public class SequenceTable extends JTable {

    public SequenceTable(SequenceTableModel dm) {
        super(dm);

        TableColumnModel columnModel = getColumnModel();

        columnModel.getColumn(SequenceTableModel.COLUMN_TIMESTAMP).setPreferredWidth(25);
        columnModel.getColumn(SequenceTableModel.COLUMN_STATUS).setPreferredWidth(15);
        columnModel.getColumn(SequenceTableModel.COLUMN_LENGTH).setPreferredWidth(15);
        columnModel.getColumn(SequenceTableModel.COLUMN_DESCRIPTION).setPreferredWidth(200);
    }
}
