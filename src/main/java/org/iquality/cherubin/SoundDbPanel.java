package org.iquality.cherubin;

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
    private final JLabel editedSound = new JLabel("Edited sound: N/A");

    private boolean isSelected;

    public SoundDbPanel(SoundDbModel soundDbModel) {
        super(new BorderLayout());

        this.soundDbModel = soundDbModel;

        soundDbTable = new SoundDbTable(new SoundDbTableModel(soundDbModel));
        add(soundDbTable.getTableHeader(), BorderLayout.PAGE_START);
        add(soundDbTable, BorderLayout.CENTER);

        soundDbModel.addSoundEditorModelListener(new SoundEditorModel.SoundEditorModelListener() {
            @Override
            public void editedSoundSelected(SingleSound sound) {
                editedSound.setText("Edited sound: " + sound);
            }

            @Override
            public void editedSoundUpdated(SingleSound sound) {
                editedSound.setText("Edited sound: " + sound + "*");
                soundDbTable.tableModel.fireTableDataChanged();
                int soundRow = findSoundInTable(sound);
                if (soundRow >= 0) {
                    soundDbTable.getSelectionModel().setSelectionInterval(soundRow, soundRow);
                    soundDbTable.scrollRectToVisible(soundDbTable.getCellRect(soundRow, 0, true));
                    SwingUtilities.invokeLater(() -> onSelected());
                }
            }
        });
    }

    private int findSoundInTable(SingleSound sound) {
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
        return "Blofeld Sound Base";
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
        components.add(makeCategoryFilterCombo());
        components.add(makeSoundSetFilterCombo());
        components.add(soundDbModel.makeSoundDumpCheckBox(() -> isSelected));
        return components;
    }

    @Override
    public List<Component> getStatusBarComponents() {
        List<Component> components = new ArrayList<>();
        components.add(editedSound);
        return components;
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

    public static DataFlavor BLOFELD_SOUND_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + SingleSound.class.getName(), "Blofeld Sound");

    private static final DataFlavor[] soundClipboardFlavors;

    static {
        soundClipboardFlavors = new DataFlavor[]{DataFlavor.stringFlavor, BLOFELD_SOUND_FLAVOR};
    }

    @Override
    public Action getCopyAction() {
        return new AbstractAction() {

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
                        int column = SoundDbTableModel.COLUMN_NAME; // The sound is the underlying object of the name column
                        SingleSound sound = (SingleSound) soundDbTable.getValueAt(row, column);

                        if (DataFlavor.stringFlavor.equals(flavor)) {
                            return sound.toString();
                        } else if (BLOFELD_SOUND_FLAVOR.equals(flavor)) {
                            return sound;
                        } else {
                            throw new UnsupportedFlavorException(flavor);
                        }
                    }
                }, null);
            }
        };
    }

    @Override
    public Action getPasteAction() {
        return null;
    }

}
