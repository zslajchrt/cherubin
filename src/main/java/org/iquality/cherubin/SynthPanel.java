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
    private final SynthPanelSoundEditorModelListener editorModelListener = new SynthPanelSoundEditorModelListener();

    private final JLabel synthInfo = new JLabel("No synth");
    private final JLabel editedSound = new JLabel("No sound in buffer");

    private boolean isSelected;

    public SynthPanel(SynthModel synthModel) {
        super(new BorderLayout());
        this.synthModel = synthModel;
        tabbedPane = new JTabbedPane();

        buildBankTabs();
    }

    private void buildBankTabs() {
        synthInfo.setText(synthModel.getSynth().toString());

        tabbedPane.removeAll();
        tabTables.clear();
        synthModel.removeSoundEditorModelListener(editorModelListener);

        for (int i = 0; i < synthModel.getSynthFactory().getBankCount(); i++) {
            SynthTableModel synthBankTableModel = new SynthTableModel(synthModel, i);
            SynthBankTable synthBankTable = new SynthBankTable(synthBankTableModel);
            tabbedPane.add(new JScrollPane(synthBankTable), "" + (char) ('A' + i));
            tabTables.add(synthBankTable);
        }

        if (synthModel.getSynthFactory().hasMultiBank()) {
            SynthMultiTable synthMultiTable = new SynthMultiTable(new SynthMultiTableModel(synthModel, synthModel.getMultiBank()));
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
        components.add(editedSound);
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
            int input = JOptionPane.showConfirmDialog(null, "Are you sure?", "Upload Virtual Synth to Real Synth", JOptionPane.YES_NO_OPTION);
            switch (input) {
                case 0: // YES
                    break;
                case 1: // NO
                    return;
            }

            synthModel.uploadSynth(SynthModel.getOutputVariant(actionEvent.getModifiers()));
        }));
    }

    @Override
    public Action getPasteAction() {
        return new AbstractAction() {
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
        };
    }

    private class SynthPanelSoundEditorModelListener implements SoundEditorModel.SoundEditorModelListener {
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
                SwingUtilities.invokeLater(SynthPanel.this::onSelected);
            }

        }

        @Override
        public void editedSoundCleared(Sound sound) {
            editedSound.setText("No sound in buffer");
        }
    }
}
