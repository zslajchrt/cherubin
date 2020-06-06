package org.iquality.cherubin;

import jdk.nashorn.internal.scripts.JO;
import org.japura.gui.CheckComboBox;
import org.japura.gui.event.ListCheckListener;
import org.japura.gui.model.ListCheckModel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SoundDbPanel extends JPanel implements AppExtension {

    private final SoundDbModel soundDbModel;
    private final SoundDbTable soundDbTable;
    private final EditedSoundStatus editedSoundStatus;

    private boolean isSelected;

    private final CopyAction copyAction;
    private final PasteAction pasteAction;

    public SoundDbPanel(SoundDbModel soundDbModel) {
        super(new BorderLayout());

        this.soundDbModel = soundDbModel;

        this.editedSoundStatus = new EditedSoundStatus(soundDbModel);

        soundDbTable = new SoundDbTable(new SoundDbTableModel(soundDbModel));
        soundDbModel.installTableBehavior(new SoundEditorTableBehavior() {
            @Override
            public JTable getJTable() {
                return soundDbTable;
            }

            @Override
            public int getSoundColumn() {
                return SoundDbTableModel.COLUMN_NAME;
            }

            @Override
            public int getCategoriesColumn() {
                return SoundDbTableModel.COLUMN_CATEGORY;
            }

            @Override
            public boolean isActive() {
                return isSelected;
            }

            @Override
            public void deleteSound(int row) {
                Sound sound = (Sound) soundDbTable.getValueAt(row, getSoundColumn());
                int response = JOptionPane.showConfirmDialog(null, String.format("Are you sure to delete sound %s?", sound), "Delete sound", JOptionPane.YES_NO_OPTION);
                if (response == JOptionPane.YES_OPTION) {
                    int savedColSelIndex = soundDbTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
                    ((SoundDbTableModel) soundDbTable.getModel()).deleteSound(sound);
                    soundDbTable.getSelectionModel().setSelectionInterval(row, row);
                    soundDbTable.getColumnModel().getSelectionModel().setSelectionInterval(savedColSelIndex, savedColSelIndex);
                }
            }
        });

        add(soundDbTable.getTableHeader(), BorderLayout.PAGE_START);
        add(soundDbTable, BorderLayout.CENTER);

        soundDbModel.addSoundEditorModelListener(new SoundEditorModel.SoundEditorModelListener() {
            @Override
            public void editedSoundSelected(Sound sound) {
            }

            @Override
            public void editedSoundUpdated(Sound sound) {
                soundDbTable.tableModel.fireTableDataChanged();
                int soundRow = findSoundInTable(sound);
                if (soundRow >= 0) {
                    soundDbTable.getSelectionModel().setSelectionInterval(soundRow, soundRow);
                    soundDbTable.scrollRectToVisible(soundDbTable.getCellRect(soundRow, 0, true));
                    SwingUtilities.invokeLater(() -> onSelected());
                }
            }

            @Override
            public void editedSoundCleared(Sound sound) {
            }
        });

        this.copyAction = new CopyAction();
        this.pasteAction = new PasteAction();
    }

    private int findSoundInTable(Sound sound) {
        for (int row = 0; row < soundDbTable.getRowCount(); row++) {
            Sound soundAtRow = (Sound) soundDbTable.getValueAt(row, SoundDbTableModel.COLUMN_NAME);
            if (soundAtRow == sound) {
                return row;
            }
        }
        return -1;
    }

    @Override
    public String getExtensionName() {
        return "Sound Base";
    }

    @Override
    public void initialize() {
        soundDbTable.tableModel.fire();
    }

    @Override
    public void close() {
    }

    @Override
    public void onSelected() {
        isSelected = true;
        soundDbTable.requestFocusInWindow();
    }

    @Override
    public void onDeselected() {
        isSelected = false;
    }

    @Override
    public Component getMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(soundDbTable.getTableHeader(), BorderLayout.PAGE_START);
        mainPanel.add(new JScrollPane(this), BorderLayout.CENTER);
        return mainPanel;
    }

    @Override
    public java.util.List<Component> getToolBarComponents() {
        List<Component> components = new ArrayList<>();
        components.add(makeSynthFilterCombo());
        components.add(makeCategoryFilterCombo());
        components.add(makeSoundSetFilterCombo());
        components.add(soundDbModel.makeSoundDumpCheckBox(() -> isSelected));
        components.add(soundDbModel.makeAuditionCheckBox());
        return components;
    }

    @Override
    public List<Component> getStatusBarComponents() {
        List<Component> components = new ArrayList<>();
        components.add(editedSoundStatus);
        return components;
    }

    CheckComboBox makeSynthFilterCombo() {
        return makeFilterCombo(soundDbTable.tableModel.getSynthNotifier(), soundDbTable.tableModel.getSynthFilterListener());
    }

    CheckComboBox makeCategoryFilterCombo() {
        return makeFilterCombo(soundDbTable.tableModel.getCategoriesNotifier(), soundDbTable.tableModel.getCategoryFilterListener());
    }

    CheckComboBox makeSoundSetFilterCombo() {
        return makeFilterCombo(soundDbTable.tableModel.getSoundSetsNotifier(), soundDbTable.tableModel.getSoundSetFilterListener());
    }

    <T> CheckComboBox makeFilterCombo(Consumer<Consumer<Iterable<T>>> notifier, ListCheckListener checkListener) {
        CheckComboBox ccb = new CheckComboBox();
        ccb.setTextFor(CheckComboBox.NONE, "* any item selected *");
        ccb.setTextFor(CheckComboBox.MULTIPLE, "* multiple items *");
        ccb.setTextFor(CheckComboBox.ALL, "* all selected *");

        notifier.accept(newValues -> updateCheckComboBoxMode(ccb, newValues));

        ccb.getModel().addListCheckListener(checkListener);

        return ccb;
    }


    private <T> void updateCheckComboBoxMode(CheckComboBox ccb, Iterable<T> values) {
        ListCheckModel comboModel = ccb.getModel();
        comboModel.clear();
        for (Object val : values) {
            comboModel.addElement(val);
        }
    }

    @Override
    public Action getCutAction() {
        return null;
    }

    public static DataFlavor SOUND_DB_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + Sound.class.getName(), "Sound");

    static final DataFlavor[] soundClipboardFlavors;

    static {
        soundClipboardFlavors = new DataFlavor[]{DataFlavor.stringFlavor, SOUND_DB_FLAVOR};
    }

    @Override
    public Action getCopyAction() {
        return copyAction;
    }

    @Override
    public Action getPasteAction() {
        return pasteAction;
    }

    private class CopyAction extends AbstractAction {
        {
            setEnabled(false);

            soundDbTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) {
                        setEnabled(true);
                    }
                }
            });
        }

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
                    int row = soundDbTable.getSelectedRow();
                    if (row < 0) {
                        return null; // TODO
                    }
                    Sound sound = (Sound) soundDbTable.getValueAt(row, SoundDbTableModel.COLUMN_NAME);

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
                if (clipboard.isDataFlavorAvailable(SoundDbPanel.SOUND_DB_FLAVOR)) {
                    handleSound(clipboard);
                } else if (clipboard.isDataFlavorAvailable(SequencePanel.MIDI_EVENTS_FLAVOR)) {
                    handleMidiEvent(clipboard);
                }

            } catch (UnsupportedFlavorException | IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        private void handleSound(Clipboard clipboard) throws UnsupportedFlavorException, IOException {
            Sound sound = (Sound) clipboard.getData(SoundDbPanel.SOUND_DB_FLAVOR);
            sound.appendToName("-Copy");
            soundDbTable.tableModel.addSound(sound);

            soundDbTable.selectSound(sound);
        }

        private void handleMidiEvent(Clipboard clipboard) throws UnsupportedFlavorException, IOException {
            MidiEvents events = (MidiEvents) clipboard.getData(SequencePanel.MIDI_EVENTS_FLAVOR);

            String soundSetName = JOptionPane.showInputDialog("Please enter sound set name: ");
            if (soundSetName != null) {
                SoundSet<Sound> soundSet = SoundDbModel.midiEventsToSoundSet(events, soundSetName);
                if (soundSet != null) {
                    soundDbTable.tableModel.addSoundSet(soundSet);
                    JOptionPane.showMessageDialog(null, soundSet.sounds.size() + " new sounds imported from " + events.getEvents().size(), "Sound import summary", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "No synth factory found for sysex", "Sound import error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}
