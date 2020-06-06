package org.iquality.cherubin;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SoundDbTable extends JTable {
    final SoundDbTableModel tableModel;

    public SoundDbTable(SoundDbTableModel soundDbTableModel) {
        super(soundDbTableModel);

        tableModel = (SoundDbTableModel) getModel();

        TableColumnModel columnModel = getColumnModel();
        columnModel.getColumn(SoundDbTableModel.COLUMN_ID).setPreferredWidth(15);
        columnModel.getColumn(SoundDbTableModel.COLUMN_NAME).setPreferredWidth(180);
        columnModel.getColumn(SoundDbTableModel.COLUMN_CATEGORY).setPreferredWidth(20);
        columnModel.getColumn(SoundDbTableModel.COLUMN_SOUNDSET).setPreferredWidth(60);
        columnModel.getColumn(SoundDbTableModel.COLUMN_SYNTH).setPreferredWidth(50);

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setHorizontalAlignment(JLabel.LEFT);
        columnModel.getColumn(0).setCellRenderer(cellRenderer);

        setAutoCreateRowSorter(true);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(tableModel);
        setRowSorter(sorter);
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();

        sortKeys.add(new RowSorter.SortKey(SoundDbTableModel.COLUMN_ID, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(SoundDbTableModel.COLUMN_CATEGORY, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(SoundDbTableModel.COLUMN_NAME, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        sorter.setComparator(SoundDbTableModel.COLUMN_CATEGORY, Comparator.comparing(Object::toString));
        sorter.sort();

    }

    public static DataFlavor VIRTUAL_SYNTH_SOUND_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + Sound.class.getName(), "Sound");

    private static final DataFlavor[] soundClipboardFlavors;

    static {
        soundClipboardFlavors = new DataFlavor[]{DataFlavor.stringFlavor, VIRTUAL_SYNTH_SOUND_FLAVOR};
    }

    public int findSoundRow(Sound sound) {
        int rowCount = getRowCount();
        for (int r = 0; r < rowCount; r++) {
            Sound s = (Sound) getValueAt(r, SoundDbTableModel.COLUMN_NAME);
            if (s == sound) {
                return r;
            }
        }
        return -1;
    }

    public void selectSound(Sound sound) {
        int row = findSoundRow(sound);
        if (row < 0) {
            return;
        }
        getSelectionModel().setSelectionInterval(row, row);
        getColumnModel().getSelectionModel().setSelectionInterval(SoundDbTableModel.COLUMN_ID, SoundDbTableModel.COLUMN_ID);
        Rectangle cellRect = getCellRect(row, SoundDbTableModel.COLUMN_ID, true);
        scrollRectToVisible(cellRect);
    }
}
