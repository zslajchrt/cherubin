package org.iquality.cherubin;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class SynthBankTable extends JTable {

    public static final String DELETE_SOUND = "deleteSound";
    public static final String SEND_SOUND = "sendSound";

    private final SynthTableModel tableModel;

    public SynthBankTable(SynthTableModel dm) {
        super(dm);

        this.tableModel = dm;

        columnModel = getColumnModel();

        columnModel.getColumn(SynthTableModel.COLUMN_SLOT).setPreferredWidth(15);
        columnModel.getColumn(SynthTableModel.COLUMN_NAME).setPreferredWidth(190);
        columnModel.getColumn(SynthTableModel.COLUMN_CATEGORY).setPreferredWidth(25);
        columnModel.getColumn(SynthTableModel.COLUMN_SOUNDSET).setPreferredWidth(100);
        columnModel.getColumn(SynthTableModel.COLUMN_REFID).setPreferredWidth(15);

        DefaultTableCellRenderer slotSellRenderer = new DefaultTableCellRenderer();
        slotSellRenderer.setHorizontalAlignment(JLabel.LEFT);
        columnModel.getColumn(SynthTableModel.COLUMN_SLOT).setCellRenderer(slotSellRenderer);

        DefaultTableCellRenderer soundNameSellRenderer = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText(((Sound)value).getName());
            }
        };
        columnModel.getColumn(SynthTableModel.COLUMN_NAME).setCellRenderer(soundNameSellRenderer);

        tableModel.getSynthModel().installTableBehavior(this, SynthTableModel.COLUMN_NAME);

        KeyStroke deleteKey = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
        getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(deleteKey, DELETE_SOUND);
        getActionMap().put(DELETE_SOUND, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = getSelectedRow();
                if (row < 0) {
                    return;
                }

                int column = SoundDbTableModel.COLUMN_NAME; // The sound is the underlying object of the name column
                Sound sound = (Sound) getValueAt(row, column);
                System.out.println("Delete sound " + sound);
                tableModel.deleteSound(row);
            }
        });
    }

    public void updateSound(Sound sound) {
        int row = getSelectedRow();
        if (row < 0) {
            row = findFirstAvailableRow();
        }
        tableModel.updateSound(row, sound);
    }

    private int findFirstAvailableRow() {
        return 0;
    }
}