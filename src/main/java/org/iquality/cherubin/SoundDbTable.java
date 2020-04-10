package org.iquality.cherubin;

import org.japura.gui.CheckComboBox;
import org.japura.gui.event.ListCheckListener;
import org.japura.gui.model.ListCheckModel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class SoundDbTable extends JTable implements AppExtension {
    final SoundDbTableModel tableModel;

    public SoundDbTable(SoundDbTableModel soundDbTableModel) {
        super(soundDbTableModel);

        tableModel = (SoundDbTableModel) getModel();

        addMouseListener(new SoundSendingMouseAdapter() {
            @Override
            protected SingleSound getValueAt(int row, int column) {
                return (SingleSound) SoundDbTable.this.getValueAt(row, column);
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

        TableColumnModel columnModel = getColumnModel();
        columnModel.getColumn(SoundDbTableModel.COLUMN_ID).setPreferredWidth(15);
        columnModel.getColumn(SoundDbTableModel.COLUMN_NAME).setPreferredWidth(180);
        columnModel.getColumn(SoundDbTableModel.COLUMN_CATEGORY).setPreferredWidth(30);
        columnModel.getColumn(SoundDbTableModel.COLUMN_SOUNDSET).setPreferredWidth(40);

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setHorizontalAlignment(JLabel.LEFT);
        columnModel.getColumn(0).setCellRenderer(cellRenderer);

        setAutoCreateRowSorter(true);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(tableModel);
        setRowSorter(sorter);
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();

        sortKeys.add(new RowSorter.SortKey(SoundDbTableModel.COLUMN_CATEGORY, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(SoundDbTableModel.COLUMN_NAME, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        sorter.setComparator(SoundDbTableModel.COLUMN_CATEGORY, Comparator.comparing(Object::toString));
        sorter.sort();
    }

    @Override
    public String getExtensionName() {
        return "Blofeld Sound Base";
    }

    @Override
    public void initialize() {
        tableModel.fire();
    }

    @Override
    public void close() {
    }

    @Override
    public void onSelected() {
        requestFocusInWindow();
    }

    @Override
    public List<Component> getToolbarComponents() {
        List<Component> components = new ArrayList<>();
        components.add(makeCategoryFilterCombo());
        components.add(makeSoundSetFilterCombo());
        return components;
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

                SoundDbTable.this.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
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
                        int row = getSelectedRow();
                        if (row < 0) {
                            return null; // TODO
                        }
                        int column = SoundDbTableModel.COLUMN_NAME; // The sound is the underlying object of the name column
                        SingleSound sound = (SingleSound) getValueAt(row, column);

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


    //    public List<SingleSound> createListEmployees() {
//        List<SingleSound> listSounds = new ArrayList<>();
//
//        SoundSet soundSet1 = new SoundSet("AlienSound");
//        SoundSet soundSet2 = new SoundSet("TechnoHell");
//        listSounds.add(new SingleSound(1, "sound3", SoundCategory.Arp, null, soundSet1));
//        listSounds.add(new SingleSound(2, "sound2", SoundCategory.Bass, null, soundSet2));
//        listSounds.add(new SingleSound(3, "sound1", SoundCategory.Bass, null, soundSet2));
//        listSounds.add(new SingleSound(4, "sound5", SoundCategory.Atmo, null, soundSet1));
//        listSounds.add(new SingleSound(5, "sound4", SoundCategory.Bass, null, soundSet1));
//
//        return listSounds;
//    }

//    protected JButton makeLoadButton() {
//        JButton button = new JButton();
//        button.setActionCommand("loadSoundSet");
//        button.setToolTipText("Load a sound set");
//        button.setText("Load");
//
//        button.addActionListener(new AbstractAction() {
//            final char[] sym = new char[]{'-', '\\', '|', '/'};
//            int cnt = 0;
//
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                JFileChooser fileChooser = new JFileChooser();
//                fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
//                int result = fileChooser.showOpenDialog(SingleSoundTable.this);
//                if (result == JFileChooser.APPROVE_OPTION) {
//                    File selectedFile = fileChooser.getSelectedFile();
//                    String soundSetName = JOptionPane.showInputDialog("Please input sound set name: ", selectedFile.getName());
//                    MidiFileLoader midiFileLoader = new MidiFileLoader();
//
//                    button.setEnabled(false);
//
//                    Thread loadThread = new Thread(() -> midiFileLoader.load(selectedFile, soundSetName, new MidiFileLoader.LoadListener() {
//                        @Override
//                        public void onSound(SingleSound sound) {
//                            button.setText("Loading " + sym[cnt % sym.length]);
//                            cnt++;
//                        }
//
//                        @Override
//                        public void onFinished(SoundSet<SingleSound> soundSet) {
//                            button.setEnabled(true);
//                            button.setText("Load");
//                            cnt = 0;
//                            tableModel.addSoundSet(soundSet);
//                        }
//
//                        @Override
//                        public void onError(Exception e) {
//                            e.printStackTrace();
//                            JOptionPane.showMessageDialog(SingleSoundTable.this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
//
//                            button.setEnabled(true);
//                            button.setText("Load");
//                            cnt = 0;
//                        }
//
//                    }));
//
//                    loadThread.start();
//                }
//            }
//        });
//
//        return button;
//    }

//    protected JButton makeCaptureButton() {
//        JButton button = new JButton();
//        button.setActionCommand("captureDump");
//        button.setToolTipText("Captures a sound dump");
//        button.setText("Capture");
//
//        button.addActionListener(new AbstractAction() {
//            final char[] sym = new char[]{'-', '\\', '|', '/'};
//            int cnt = 0;
//            SoundCapture capture;
//
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                if ("captureDump".equals(e.getActionCommand())) {
//                    String dumpName = JOptionPane.showInputDialog("Please input dump name: ", "Dump-" + System.currentTimeMillis());
//                    try {
//                        button.setActionCommand("stopDump");
//                        capture = new SoundCapture(midiDeviceIn, dumpName);
//                        button.setText("Stop");
//                        capture.start();
//                        capture.addDumpListener(sound -> {
//                            button.setText("Stop " + sym[cnt % sym.length]);
//                            cnt++;
//                        });
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                    }
//                } else if ("stopDump".equals(e.getActionCommand())) {
//                    capture.stop();
//                    tableModel.addSoundSet(capture.soundSet);
//                    capture = null;
//                    cnt = 0;
//                    button.setText("Capture");
//                    button.setActionCommand("captureDump");
//                }
//            }
//        });
//        return button;
//    }

    CheckComboBox makeCategoryFilterCombo() {
        return makeFilterCombo(tableModel.getCategoriesNotifier(), tableModel.getCategoryFilterListener());
    }

    CheckComboBox makeSoundSetFilterCombo() {
        return makeFilterCombo(tableModel.getSoundSetsNotifier(), tableModel.getSoundSetFilterListener());
    }

    private <T> void updateCheckComboBoxMode(CheckComboBox ccb, Iterable<T> values) {
        ListCheckModel comboModel = ccb.getModel();
        comboModel.clear();
        for (Object val : values) {
            comboModel.addElement(val);
        }
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

}
