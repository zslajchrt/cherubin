package org.iquality.cherubin;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VirtualBlofeldPanel extends JPanel implements AppExtension {

    private final VirtualBlofeldModel blofeldModel;
    private final JTabbedPane tabbedPane;
    private final List<JTable> tabTables = new ArrayList<>();

    private final JLabel editedSound = new JLabel("Edited sound: N/A");

    private boolean isSelected;

    public VirtualBlofeldPanel(VirtualBlofeldModel blofeldModel) {
        super(new BorderLayout());
        this.blofeldModel = blofeldModel;
        tabbedPane = new JTabbedPane();
        for (int i = 0; i < VirtualBlofeldModel.BANKS_NUMBER; i++) {
            VirtualBlofeldTableModel blofeldBankTableModel = new VirtualBlofeldTableModel(blofeldModel, i);
            VirtualBlofeldTable blofeldBankTable = new VirtualBlofeldTable(blofeldBankTableModel);
            tabbedPane.add(new JScrollPane(blofeldBankTable), "" + (char) ('A' + i));
            tabTables.add(blofeldBankTable);
        }

        VirtualBlofeldMultiTable blofeldMultiTable = new VirtualBlofeldMultiTable(new VirtualBlofeldMultiTableModel(blofeldModel, blofeldModel.getMultiBank()));
        tabbedPane.add(new JScrollPane(blofeldMultiTable), "Multi");
        tabTables.add(blofeldMultiTable);

        add(tabbedPane, BorderLayout.CENTER);

        blofeldModel.addSoundEditorModelListener(new SoundEditorModel.SoundEditorModelListener() {
            @Override
            public void editedSoundSelected(SingleSound sound) {
                editedSound.setText("Edited sound: " + sound);
            }

            @Override
            public void editedSoundUpdated(SingleSound sound) {
                editedSound.setText("Edited sound: " + sound + "*");
                tabTables.forEach(jTable -> ((AbstractTableModel) jTable.getModel()).fireTableDataChanged());

                int[] tabAndRow = new int[2];
                findSoundInTable(sound, tabAndRow);
                int tab = tabAndRow[0];
                int soundRow = tabAndRow[1];
                if (tab >= 0 && soundRow >= 0) {
                    tabbedPane.setSelectedIndex(tab);
                    JTable table = tabTables.get(tab);
                    table.getSelectionModel().setSelectionInterval(soundRow, soundRow);
                    table.scrollRectToVisible(table.getCellRect(soundRow, 0, true));
                    SwingUtilities.invokeLater(() -> onSelected());
                }

            }
        });

    }

    private void findSoundInTable(SingleSound sound, int[] tabAndRow) {
        for (int tableIndex = 0; tableIndex < tabTables.size(); tableIndex++) {
            JTable table = tabTables.get(tableIndex);
            for (int row = 0; row < table.getRowCount(); row++) {
                Sound soundAtRow = (Sound) table.getValueAt(row, SoundDbTableModel.COLUMN_NAME);
                if (soundAtRow == sound) {
                    tabAndRow[0] = tableIndex;
                    tabAndRow[1] = row;
                    return;
                }
            }
        }
        tabAndRow[0] = -1;
        tabAndRow[1] = -1;
    }

    @Override
    public String getExtensionName() {
        return "Virtual Blofeld";
    }

    @Override
    public void initialize() {

    }

    @Override
    public void close() {

    }

    @Override
    public void onSelected() {
        isSelected = true;
        ((JScrollPane) tabbedPane.getSelectedComponent()).getViewport().getView().requestFocusInWindow();
    }

    @Override
    public void onDeselected() {
        isSelected = false;
    }

    @Override
    public Component getMainPanel() {
        return new JScrollPane(this);
    }

    @Override
    public List<Component> getToolBarComponents() {
        List<Component> buttons = new ArrayList<>();
        buttons.add(makeNewButton());
        buttons.add(makeLoadButton());
        buttons.add(makeSaveButton());
        buttons.add(makeDeleteButton());
        buttons.add(makeUploadButton());
        buttons.add(blofeldModel.makeSoundDumpCheckBox(() -> isSelected));
        return buttons;
    }

    @Override
    public List<Component> getStatusBarComponents() {
        List<Component> components = new ArrayList<>();
        components.add(editedSound);
        return components;
    }

    protected JButton makeNewButton() {
        return AppFrame.makeButton("newBlofeld", "New Virtual Blofeld", "New", (actionEvent -> {
            String name = JOptionPane.showInputDialog("Please enter the name: ");
            if (name != null) {

                if (blofeldModel.exists(name)) {
                    int input = JOptionPane.showConfirmDialog(null, "A Blofeld of this name already exists. Overwrite?", "Overwrite?", JOptionPane.YES_NO_OPTION);
                    switch (input) {
                        case 0: // YES
                            break;
                        case 1: // NO
                            return;
                    }
                }

                if (blofeldModel.getBlofeld().isDirty()) {
                    int input = JOptionPane.showConfirmDialog(null, "Save edits?");
                    switch (input) {
                        case 0: // YES
                            blofeldModel.saveBlofeld();
                            break;
                        case 1: // NO
                            break;
                        case 2: // CANCEL
                            return;
                    }
                }
                blofeldModel.newBlofeld(name);
                tabTables.forEach(tbl -> ((AbstractTableModel) tbl.getModel()).fireTableDataChanged());
            }
        }));
    }

    protected JButton makeLoadButton() {
        return AppFrame.makeButton("loadBlofeld", "Load a Virtual Blofeld", "Load", (actionEvent -> {
            List<String> blofeldNamesList = blofeldModel.getBlofeldNames();
            String[] blofeldNames = blofeldNamesList.toArray(new String[0]);
            String blofeldName = (String) JOptionPane.showInputDialog(null, "Choose Blofeld...",
                    "Load Virtual Blofeld", JOptionPane.QUESTION_MESSAGE, null, blofeldNames, "");
            if (blofeldName != null && !"".equals(blofeldName)) {

                if (blofeldModel.getBlofeld().isDirty()) {
                    int input = JOptionPane.showConfirmDialog(null, "Save edits?");
                    switch (input) {
                        case 0: // YES
                            blofeldModel.saveBlofeld();
                            break;
                        case 1: // NO
                            break;
                        case 2: // CANCEL
                            return;
                    }
                }

                blofeldModel.loadBlofeld(blofeldName);
            }
        }));
    }

    protected JButton makeSaveButton() {
        return AppFrame.makeButton("saveBlofeld", "Save the Virtual Blofeld", "Save", (actionEvent -> {
            blofeldModel.saveBlofeld();
        }));
    }

    protected JButton makeDeleteButton() {
        return AppFrame.makeButton("deleteBlofeld", "Delete the Virtual Blofeld", "Delete", (actionEvent -> {
            if (blofeldModel.getBlofeld().isInitial()) {
                JOptionPane.showMessageDialog(null, "Initial Virtual Blofeld cannot be deleted", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int input = JOptionPane.showConfirmDialog(null, "Are you sure?", "Delete Virtual Blofeld", JOptionPane.YES_NO_OPTION);
            switch (input) {
                case 0: // YES
                    break;
                case 1: // NO
                    return;
            }

            blofeldModel.deleteBlofeld();
        }));
    }

    protected JButton makeUploadButton() {
        return AppFrame.makeButton("uploadBlofeld", "Uploads virtual Blofeld to real Blofeld", "Upload", (actionEvent -> {
            int input = JOptionPane.showConfirmDialog(null, "Are you sure?", "Upload Virtual Blofeld to Real Blofeld", JOptionPane.YES_NO_OPTION);
            switch (input) {
                case 0: // YES
                    break;
                case 1: // NO
                    return;
            }

            blofeldModel.uploadBlofeld();
        }));
    }

    @Override
    public Action getPasteAction() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Clipboard clipboard = getSystemClipboard();
                try {
                    if (!clipboard.isDataFlavorAvailable(SoundDbTable.BLOFELD_SOUND_FLAVOR)) {
                        return;
                    }
                    SingleSound sound = (SingleSound) clipboard.getData(SoundDbTable.BLOFELD_SOUND_FLAVOR);

                    JTable selectedTab = tabTables.get(tabbedPane.getSelectedIndex());
                    if (selectedTab instanceof VirtualBlofeldTable) {
                        int savedSelection = selectedTab.getSelectionModel().getMinSelectionIndex();
                        ((VirtualBlofeldTable) selectedTab).updateSound(sound);
                        selectedTab.getSelectionModel().setSelectionInterval(savedSelection, savedSelection);
                    }

                } catch (UnsupportedFlavorException | IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }
}
