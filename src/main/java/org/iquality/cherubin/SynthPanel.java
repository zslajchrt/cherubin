package org.iquality.cherubin;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.iquality.cherubin.SoundDbPanel.SOUND_DB_FLAVOR;
import static org.iquality.cherubin.SoundDbPanel.soundClipboardFlavors;

public class SynthPanel extends JPanel implements AppExtension {

    private final SynthModel synthModel;
    private final JTabbedPane tabbedPane;
    private final List<JTable> tabTables = new ArrayList<>();
    private final SynthPanelSoundEditorModelListener editorModelListener = new SynthPanelSoundEditorModelListener();

    private final JLabel synthInfo = new JLabel("No synth");
    private final EditedSoundStatus editedSoundStatus;

    private final PasteAction pasteAction;
    private final CopyAction copyAction;

    private boolean isSelected;

    public SynthPanel(SynthModel synthModel) {
        super(new BorderLayout());
        this.synthModel = synthModel;

        this.editedSoundStatus = new EditedSoundStatus(synthModel);

        tabbedPane = new JTabbedPane();

        buildBankTabs();

        pasteAction = new PasteAction();
        copyAction = new CopyAction();
    }

    private void buildBankTabs() {
        synthInfo.setText(synthModel.getSynth().toString());

        tabbedPane.removeAll();
        tabTables.clear();
        synthModel.removeSoundEditorModelListener(editorModelListener);

        for (int i = 0; i < synthModel.getSynthFactory().getBankCount(); i++) {
            SynthTableModel synthBankTableModel = new SynthTableModel(synthModel, i);
            SynthBankTable synthBankTable = new SynthBankTable(synthBankTableModel);
            synthModel.installTableBehavior(new SingleSoundEditorTableBehavior(synthBankTable));
            tabbedPane.add(new JScrollPane(synthBankTable), "" + (char) ('A' + i));
            tabTables.add(synthBankTable);
        }

        if (synthModel.getSynthFactory().hasMultiBank()) {
            SynthMultiTableModel synthMultiTableModel = new SynthMultiTableModel(synthModel);
            SynthMultiTable synthMultiTable = new SynthMultiTable(synthMultiTableModel);
            synthModel.installTableBehavior(new MultiSoundEditorTableBehavior(synthMultiTable));
            tabbedPane.add(new JScrollPane(synthMultiTable), "Multi");
            tabTables.add(synthMultiTable);
        }

        add(tabbedPane, BorderLayout.CENTER);

        this.synthModel.addSoundEditorModelListener(editorModelListener);

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
        return "Virtual Synth";
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
        JScrollPane selectedComponent = (JScrollPane) tabbedPane.getSelectedComponent();
        if (selectedComponent != null) {
            selectedComponent.getViewport().getView().requestFocusInWindow();
        }
    }

    @Override
    public void onDeselected() {
        isSelected = false;
    }

    @Override
    public Component getMainPanel() {
        return this;
    }

    @Override
    public List<Component> getToolBarComponents() {
        List<Component> buttons = new ArrayList<>();
        buttons.add(makeNewButton());
        buttons.add(makeLoadButton());
        buttons.add(makeSaveButton());
        buttons.add(makeDeleteButton());
        buttons.add(makeUploadButton());

        JCheckBox soundDumpCheckBox = synthModel.makeSoundDumpCheckBox(() -> isSelected);
        buttons.add(soundDumpCheckBox);
        synthModel.addSynthModelListener(synth -> soundDumpCheckBox.setSelected(synthModel.isListeningToDump()));

        JCheckBox auditionCheckBox = synthModel.makeAuditionCheckBox();
        buttons.add(auditionCheckBox);

        return buttons;
    }

    @Override
    public List<Component> getStatusBarComponents() {
        List<Component> components = new ArrayList<>();
        components.add(synthInfo);
        components.add(editedSoundStatus);
        return components;
    }

    protected JButton makeNewButton() {
        return AppFrame.makeButton("newSynth", "New Virtual Synth", "New", (actionEvent -> {
            String name = JOptionPane.showInputDialog("Please enter the name: ");
            if (name != null) {

                if (synthModel.exists(name)) {
                    int input = JOptionPane.showConfirmDialog(null, "A synth of this name already exists. Overwrite?", "Overwrite?", JOptionPane.YES_NO_OPTION);
                    switch (input) {
                        case 0: // YES
                            break;
                        case 1: // NO
                            return;
                    }
                }

                ArrayList<SynthFactory> synthFactories = new ArrayList<>();
                SynthFactoryRegistry.INSTANCE.getSynthFactories().forEach(synthFactories::add);
                SynthFactory[] synthFactoriesArray = synthFactories.toArray(new SynthFactory[0]);

                SynthFactory synthFactory = (SynthFactory) JOptionPane.showInputDialog(null, "Choose Synth Type ...",
                        "Load Virtual Synth", JOptionPane.QUESTION_MESSAGE, null, synthFactoriesArray, "");
                if (synthFactory == null) {
                    return;
                }

                if (synthModel.getSynth().isDirty()) {
                    int input = JOptionPane.showConfirmDialog(null, "Save edits?");
                    switch (input) {
                        case 0: // YES
                            synthModel.saveSynth();
                            ;
                            break;
                        case 1: // NO
                            break;
                        case 2: // CANCEL
                            return;
                    }
                }
                synthModel.newSynth(name, synthFactory);
                buildBankTabs();
            }
        }));
    }

    protected JButton makeLoadButton() {
        return AppFrame.makeButton("loadSynth", "Load a Virtual Synth", "Load", (actionEvent -> {
            List<SynthHeader> synthInstList = synthModel.getSynthInstances();
            SynthHeader[] synthHeaders = synthInstList.toArray(new SynthHeader[synthInstList.size()]);
            SynthHeader selectedSynthHeader = (SynthHeader) JOptionPane.showInputDialog(null, "Choose a Synth...",
                    "Load Virtual Synth", JOptionPane.QUESTION_MESSAGE, null, synthHeaders, "");
            if (selectedSynthHeader != null) {

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

                synthModel.loadSynth(selectedSynthHeader);
                buildBankTabs();
            }
        }));
    }

    protected JButton makeSaveButton() {
        return AppFrame.makeButton("saveSynth", "Save the Virtual Synth", "Save", (actionEvent -> {
            synthModel.saveSynth();
        }));
    }

    protected JButton makeDeleteButton() {
        return AppFrame.makeButton("deleteSynth", "Delete the Virtual Synth", "Delete", (actionEvent -> {
            if (synthModel.isInitialSynth()) {
                JOptionPane.showMessageDialog(null, "Initial Virtual Synth cannot be deleted", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int input = JOptionPane.showConfirmDialog(null, "Are you sure?", "Delete Virtual Synth", JOptionPane.YES_NO_OPTION);
            switch (input) {
                case 0: // YES
                    break;
                case 1: // NO
                    return;
            }

            synthModel.deleteSynth();
            buildBankTabs();
        }));
    }

    protected JButton makeUploadButton() {
        return AppFrame.makeButton("uploadSynth", "Uploads Virtual Synth to real Synth", "Upload", (actionEvent -> {
            int outputVariant = MidiDeviceManager.getOutputVariant(actionEvent);
            int input = JOptionPane.showConfirmDialog(null, "Are you sure?", "Upload Virtual Synth to Real Synth", JOptionPane.YES_NO_OPTION);
            switch (input) {
                case 0: // YES
                    break;
                case 1: // NO
                    return;
            }

            synthModel.uploadSynth(outputVariant);
        }));
    }

    @Override
    public Action getPasteAction() {
        return pasteAction;
    }

    @Override
    public Action getCopyAction() {
        return copyAction;
    }

    private class SynthPanelSoundEditorModelListener implements SoundEditorModel.SoundEditorModelListener {
        @Override
        public void editedSoundSelected(Sound sound) {
        }

        @Override
        public void editedSoundUpdated(Sound sound) {
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
                SwingUtilities.invokeLater(SynthPanel.this::onSelected);
            }

        }

        @Override
        public void editedSoundCleared(Sound sound) {
        }
    }

    private class CopyAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            Clipboard clipboard = getSystemClipboard();
            clipboard.setContents(new Transferable() {
                @Override
                public DataFlavor[] getTransferDataFlavors() {
                    return soundClipboardFlavors;
                }

                @Override
                public boolean isDataFlavorSupported(DataFlavor flavor) {
                    for (DataFlavor soundClipboardFlavor : soundClipboardFlavors) {
                        if (soundClipboardFlavor.equals(flavor)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                    int selectedTabIndex = tabbedPane.getSelectedIndex();
                    JTable table = tabTables.get(selectedTabIndex);
                    int row = table.getSelectedRow();
                    if (row < 0) {
                        return null; // TODO
                    }
                    Sound sound = (Sound) table.getValueAt(row, SoundDbTableModel.COLUMN_NAME);

                    if (DataFlavor.stringFlavor.equals(flavor)) {
                        return sound.toString();
                    } else if (SOUND_DB_FLAVOR.equals(flavor)) {
                        return sound.clone();
                    } else {
                        throw new UnsupportedFlavorException(flavor);
                    }
                }
            }, null);
        }
    }

    private class PasteAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            Clipboard clipboard = getSystemClipboard();
            try {
                if (!clipboard.isDataFlavorAvailable(SoundDbTable.VIRTUAL_SYNTH_SOUND_FLAVOR)) {
                    return;
                }
                Sound sound = (Sound) clipboard.getData(SoundDbTable.VIRTUAL_SYNTH_SOUND_FLAVOR);

                JTable selectedTab = tabTables.get(tabbedPane.getSelectedIndex());
                if (selectedTab instanceof SynthBankTable) {
                    int savedSelection = selectedTab.getSelectionModel().getMinSelectionIndex();
                    if (((SynthBankTable) selectedTab).updateSound(sound)) {
                        selectedTab.getSelectionModel().setSelectionInterval(savedSelection, savedSelection);
                    } else {
                        JOptionPane.showMessageDialog(null, String.format("Attempted to paste a sound from %s to %s", sound.getSynthFactory().getSynthId(), synthModel.getSynth().getSynthFactory().getSynthId()), "Incompatible sound", JOptionPane.ERROR_MESSAGE);
                    }
                }

            } catch (UnsupportedFlavorException | IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private class SingleSoundEditorTableBehavior implements SoundEditorTableBehavior {
        private final SynthBankTable synthBankTable;
        private final SynthTableModel synthBankTableModel;

        public SingleSoundEditorTableBehavior(SynthBankTable synthBankTable) {
            this.synthBankTable = synthBankTable;
            this.synthBankTableModel = (SynthTableModel) synthBankTable.getModel();
        }

        @Override
        public JTable getJTable() {
            return synthBankTable;
        }

        @Override
        public int getSoundColumn() {
            return SynthTableModel.COLUMN_NAME;
        }

        @Override
        public int getCategoriesColumn() {
            return SynthTableModel.COLUMN_CATEGORY;
        }

        @Override
        public boolean isActive() {
            return isSelected;
        }

        @Override
        public void deleteSound(int row) {
            int savedColIndex = synthBankTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
            synthBankTableModel.deleteSound(row);
            synthBankTable.getSelectionModel().setSelectionInterval(row, row);
            synthBankTable.getColumnModel().getSelectionModel().setSelectionInterval(savedColIndex, savedColIndex);
        }
    }

    private class MultiSoundEditorTableBehavior implements SoundEditorTableBehavior {
        private final SynthMultiTable synthMultiTable;
        private final SynthMultiTableModel synthMultiTableModel;

        public MultiSoundEditorTableBehavior(SynthMultiTable synthMultiTable) {
            this.synthMultiTable = synthMultiTable;
            this.synthMultiTableModel = (SynthMultiTableModel) synthMultiTable.getModel();
        }

        @Override
        public JTable getJTable() {
            return synthMultiTable;
        }

        @Override
        public int getSoundColumn() {
            return SynthMultiTableModel.COLUMN_NAME;
        }

        @Override
        public int getCategoriesColumn() {
            return -1;
        }

        @Override
        public boolean isActive() {
            return isSelected;
        }

        @Override
        public void deleteSound(int row) {
            int savedColIndex = synthMultiTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
            synthMultiTableModel.deleteSound(row);
            synthMultiTable.getSelectionModel().setSelectionInterval(row, row);
            synthMultiTable.getColumnModel().getSelectionModel().setSelectionInterval(savedColIndex, savedColIndex);
        }
    }
}
