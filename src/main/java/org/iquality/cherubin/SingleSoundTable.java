package org.iquality.cherubin;

import org.japura.gui.CheckComboBox;
import org.japura.gui.event.ListCheckListener;
import org.japura.gui.model.ListCheckModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class SingleSoundTable extends JFrame {
    private final JTable table;
    private final SingleSoundTableModel tableModel;
    private final String midiDeviceInName;
    private final String midiDeviceOutName;

    public SingleSoundTable(DbManager dbManager, String midiDeviceInName, String midiDeviceOutName) {
        super("Waldorf Blofeld Librarian");

        this.midiDeviceInName = midiDeviceInName;
        this.midiDeviceOutName = midiDeviceOutName;

        tableModel = new SingleSoundTableModel(dbManager);
        table = new JTable(tableModel);
        table.addMouseListener(new MouseAdapter() {
            SoundSender sender;

            @Override
            public void mouseClicked(MouseEvent e) {
                if (sender == null) {
                    try {
                        sender = new SoundSender(midiDeviceOutName);
                    } catch (Exception ex) {
                        sender = null;
                        return;
                    }
                }

                if (e.getClickCount() == 2) {
                    sender.probeNoteOff();
                    JTable target = (JTable) e.getSource();
                    int row = target.getSelectedRow();
                    int column = SingleSoundTableModel.COLUMN_NAME;
                    SingleSound sound = (SingleSound) table.getValueAt(row, column);
                    sender.sendSound(sound);
                    sender.probeNoteOn();
                }
                if (e.getClickCount() == 1) {
                    sender.probeNoteOff();
                }
            }
        });


        table.getColumnModel().getColumn(SingleSoundTableModel.COLUMN_ID).setPreferredWidth(15);
        table.getColumnModel().getColumn(SingleSoundTableModel.COLUMN_NAME).setPreferredWidth(180);
        table.getColumnModel().getColumn(SingleSoundTableModel.COLUMN_CATEGORY).setPreferredWidth(30);
        table.getColumnModel().getColumn(SingleSoundTableModel.COLUMN_SOUNDSET).setPreferredWidth(40);

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(cellRenderer);

        table.setAutoCreateRowSorter(true);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();

        sortKeys.add(new RowSorter.SortKey(SingleSoundTableModel.COLUMN_CATEGORY, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(SingleSoundTableModel.COLUMN_NAME, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);

        sorter.setComparator(SingleSoundTableModel.COLUMN_CATEGORY, Comparator.comparing(Object::toString));

        sorter.sort();

        JToolBar toolBar = new JToolBar("Still draggable");
        addButtons(toolBar);
        add(toolBar, BorderLayout.PAGE_START);
        add(new JScrollPane(table), BorderLayout.CENTER);

        tableModel.fire();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                tableModel.close();
            }
        });

        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new SingleSoundTable(new DbManager("jdbc:h2:/Users/zslajchrt/Music/Waldorf/Blofeld/Cherubin/allsounds;IFEXISTS=TRUE", "zbynek", "Ovation1"), "CoreMIDI4J - IAC Driver Virtual MIDI cable 1", "CoreMIDI4J - IAC Driver Virtual MIDI cable 2").setVisible(true);
            }
        });
    }

    protected void addButtons(JToolBar toolBar) {
        JButton button = null;

//        button = makeSaveButton();
//        toolBar.add(button);
        button = makeLoadButton();
        toolBar.add(button);
        button = makeCaptureButton();
        toolBar.add(button);

        toolBar.add(makeCategoryFilterCombo());
        toolBar.add(makeSoundSetFilterCombo());
    }

    protected JButton makeLoadButton() {
        JButton button = new JButton();
        button.setActionCommand("loadSoundSet");
        button.setToolTipText("Load a sound set");
        button.setText("Load");

        button.addActionListener(new AbstractAction() {
            final char[] sym = new char[]{'-', '\\', '|', '/'};
            int cnt = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
                int result = fileChooser.showOpenDialog(SingleSoundTable.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    String soundSetName = JOptionPane.showInputDialog("Please input sound set name: ", selectedFile.getName());
                    MidiFileLoader midiFileLoader = new MidiFileLoader();

                    button.setEnabled(false);

                    Thread loadThread = new Thread(() -> midiFileLoader.load(selectedFile, soundSetName, new MidiFileLoader.LoadListener() {
                        @Override
                        public void onSound(SingleSound sound) {
                            button.setText("Loading " + sym[cnt % sym.length]);
                            cnt++;
                        }

                        @Override
                        public void onFinished(SoundSet<SingleSound> soundSet) {
                            button.setEnabled(true);
                            button.setText("Load");
                            cnt = 0;
                            tableModel.addSoundSet(soundSet);
                        }

                        @Override
                        public void onError(Exception e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(SingleSoundTable.this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);

                            button.setEnabled(true);
                            button.setText("Load");
                            cnt = 0;
                        }

                    }));

                    loadThread.start();
                }
            }
        });

        return button;
    }

    protected JButton makeCaptureButton() {
        JButton button = new JButton();
        button.setActionCommand("captureDump");
        button.setToolTipText("Captures a sound dump");
        button.setText("Capture Dump");

        button.addActionListener(new AbstractAction() {
            final char[] sym = new char[]{'-', '\\', '|', '/'};
            int cnt = 0;
            SoundCapture capture;

            @Override
            public void actionPerformed(ActionEvent e) {
                if ("captureDump".equals(e.getActionCommand())) {
                    String dumpName = JOptionPane.showInputDialog("Please input dump name: ", "Dump-" + System.currentTimeMillis());
                    try {
                        button.setActionCommand("stopDump");
                        capture = new SoundCapture(midiDeviceInName, dumpName);
                        button.setText("Stop");
                        capture.start();
                        capture.addDumpListener(sound -> {
                            button.setText("Stop " + sym[cnt % sym.length]);
                            cnt++;
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else if ("stopDump".equals(e.getActionCommand())) {
                    capture.stop();
                    tableModel.addSoundSet(capture.soundSet);
                    capture = null;
                    cnt = 0;
                    button.setText("Capture Dump");
                    button.setActionCommand("captureDump");
                }
            }
        });
        return button;
    }

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
