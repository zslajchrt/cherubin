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

public class SynthPanel extends JPanel implements AppExtension {

    private final SynthModel synthModel;
    private final JTabbedPane tabbedPane;
    private final List<JTable> tabTables = new ArrayList<>();

    private final JLabel editedSound = new JLabel("No sound in buffer");

    private boolean isSelected;

    public SynthPanel(SynthModel synthModel) {
        super(new BorderLayout());
        this.synthModel = synthModel;
        tabbedPane = new JTabbedPane();
        for (int i = 0; i < synthModel.getSynthFactory().getBankCount(); i++) {
            SynthTableModel synthBankTableModel = new SynthTableModel(synthModel, i);
            SynthBankTable synthBankTable = new SynthBankTable(synthBankTableModel);
            tabbedPane.add(new JScrollPane(synthBankTable), "" + (char) ('A' + i));
            tabTables.add(synthBankTable);
        }

        SynthMultiTable blofeldMultiTable = new SynthMultiTable(new SynthMultiTableModel(synthModel, synthModel.getMultiBank()));
        tabbedPane.add(new JScrollPane(blofeldMultiTable), "Multi");
        tabTables.add(blofeldMultiTable);

        add(tabbedPane, BorderLayout.CENTER);

        synthModel.addSoundEditorModelListener(new SoundEditorModel.SoundEditorModelListener() {
            @Override
            public void editedSoundSelected(Sound sound) {
                editedSound.setText("" + sound);
            }

            @Override
            public void editedSoundUpdated(Sound sound) {
                editedSound.setText("" + sound + "*");
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

    private void findSoundInTable(Sound sound, int[] tabAndRow) {
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
        buttons.add(synthModel.makeSoundDumpCheckBox(() -> isSelected));
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

                if (synthModel.exists(name)) {
                    int input = JOptionPane.showConfirmDialog(null, "A Blofeld of this name already exists. Overwrite?", "Overwrite?", JOptionPane.YES_NO_OPTION);
                    switch (input) {
                        case 0: // YES
                            break;
                        case 1: // NO
                            return;
                    }
                }

                if (synthModel.getSynth().isDirty()) {
                    int input = JOptionPane.showConfirmDialog(null, "Save edits?");
                    switch (input) {
                        case 0: // YES
                            synthModel.saveSynth();;
                            break;
                        case 1: // NO
                            break;
                        case 2: // CANCEL
                            return;
                    }
                }
                synthModel.newSynth(name);
                tabTables.forEach(tbl -> ((AbstractTableModel) tbl.getModel()).fireTableDataChanged());
            }
        }));
    }

    protected JButton makeLoadButton() {
        return AppFrame.makeButton("loadBlofeld", "Load a Virtual Blofeld", "Load", (actionEvent -> {
            List<String> synthInstList = synthModel.getSynthInstances();
            String[] synthInstNames = synthInstList.toArray(new String[0]);
            String synthInstName = (String) JOptionPane.showInputDialog(null, "Choose Blofeld...",
                    "Load Virtual Blofeld", JOptionPane.QUESTION_MESSAGE, null, synthInstNames, "");
            if (synthInstName != null && !"".equals(synthInstName)) {

                if (synthModel.getSynth().isDirty()) {
                    int input = JOptionPane.showConfirmDialog(null, "Save edits?");
                    switch (input) {
                        case 0: // YES
                            synthModel.saveSynth();
                            break;
                        case 1: // NO
                            break;
                        case 2: // CANCEL
                            return;
                    }
                }

                synthModel.loadSynth(synthInstName);
            }
        }));
    }

    protected JButton makeSaveButton() {
        return AppFrame.makeButton("saveBlofeld", "Save the Virtual Blofeld", "Save", (actionEvent -> {
            synthModel.saveSynth();
        }));
    }

    protected JButton makeDeleteButton() {
        return AppFrame.makeButton("deleteBlofeld", "Delete the Virtual Blofeld", "Delete", (actionEvent -> {
            if (synthModel.getSynthFactory().isInitial(synthModel.getSynth())) {
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

            synthModel.deleteSynth();
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

            synthModel.uploadSynth();
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
                    Sound sound = (Sound) clipboard.getData(SoundDbTable.BLOFELD_SOUND_FLAVOR);

                    JTable selectedTab = tabTables.get(tabbedPane.getSelectedIndex());
                    if (selectedTab instanceof SynthBankTable) {
                        int savedSelection = selectedTab.getSelectionModel().getMinSelectionIndex();
                        ((SynthBankTable) selectedTab).updateSound(sound);
                        selectedTab.getSelectionModel().setSelectionInterval(savedSelection, savedSelection);
                    }

                } catch (UnsupportedFlavorException | IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }
}
