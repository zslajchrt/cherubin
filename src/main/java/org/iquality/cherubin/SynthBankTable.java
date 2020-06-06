package org.iquality.cherubin;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class SynthBankTable extends JTable {

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
                Sound sound = (Sound) value;
                setText(sound.getName());
                Color color = sound.isInit() ? Color.RED : Color.BLACK;
                setForeground(color);
            }
        };
        columnModel.getColumn(SynthTableModel.COLUMN_NAME).setCellRenderer(soundNameSellRenderer);

        KeyStroke deleteKey = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
        getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(deleteKey, "deleteSound");
        getActionMap().put("deleteSound", new AbstractAction() {
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

    public boolean updateSound(Sound sound) {
        int row = getSelectedRow();
        if (row < 0) {
            row = findFirstAvailableRow();
        }
        return tableModel.updateSound(row, sound);
    }

    private int findFirstAvailableRow() {
        return 0;
    }
}
