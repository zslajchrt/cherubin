package org.iquality.cherubin;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class VirtualBlofeldTable extends JTable {

    public static final String DELETE_SOUND = "deleteSound";

    private final VirtualBlofeldTableModel tableModel;

    public VirtualBlofeldTable(VirtualBlofeldTableModel dm) {
        super(dm);

        this.tableModel = dm;

        columnModel = getColumnModel();

        columnModel.getColumn(VirtualBlofeldTableModel.COLUMN_SLOT).setPreferredWidth(15);
        columnModel.getColumn(VirtualBlofeldTableModel.COLUMN_NAME).setPreferredWidth(190);
        columnModel.getColumn(VirtualBlofeldTableModel.COLUMN_CATEGORY).setPreferredWidth(25);
        columnModel.getColumn(VirtualBlofeldTableModel.COLUMN_SOUNDSET).setPreferredWidth(100);
        columnModel.getColumn(VirtualBlofeldTableModel.COLUMN_REFID).setPreferredWidth(15);

        DefaultTableCellRenderer slotSellRenderer = new DefaultTableCellRenderer();
        slotSellRenderer.setHorizontalAlignment(JLabel.LEFT);
        columnModel.getColumn(VirtualBlofeldTableModel.COLUMN_SLOT).setCellRenderer(slotSellRenderer);

        DefaultTableCellRenderer soundNameSellRenderer = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText(((SingleSound) value).name);
            }
        };
        columnModel.getColumn(VirtualBlofeldTableModel.COLUMN_NAME).setCellRenderer(soundNameSellRenderer);

        addMouseListener(new SoundSendingMouseAdapter() {
            @Override
            protected SingleSound getValueAt(int row, int column) {
                return (SingleSound) VirtualBlofeldTable.this.getValueAt(row, column);
            }

            @Override
            protected void sendSound(SingleSound sound, AppModel.OutputDirection direction) {
                tableModel.sendSound(sound, AppModel.OutputDirection.both);
            }

            @Override
            protected void sendSoundOn(SingleSound sound) {
                tableModel.sendSoundOn(sound);
            }

            @Override
            protected void sendSoundOff() {
                tableModel.sendSoundOff();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    JTable target = (JTable) e.getSource();
                    int row = target.getSelectedRow();
                    int column = SoundDbTableModel.COLUMN_NAME;
                    SingleSound sound = (SingleSound) getValueAt(row, column);

                    tableModel.sendSoundOn(sound);
                }
                if (e.getClickCount() == 1) {
                    tableModel.sendSoundOff();
                }
            }
        });

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
                SingleSound sound = (SingleSound) getValueAt(row, column);
                System.out.println("Delete sound " + sound);
                tableModel.deleteSound(row);
            }
        });

    }

    public void updateSound(SingleSound sound) {
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
