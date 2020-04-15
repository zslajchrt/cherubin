package org.iquality.cherubin;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SequenceTable extends JTable {

    private final SequenceTableModel sequenceTableModel;

    public SequenceTable(SequenceTableModel dm) {
        super(dm);

        this.sequenceTableModel = dm;

        TableColumnModel columnModel = getColumnModel();

        columnModel.getColumn(SequenceTableModel.COLUMN_TIMESTAMP).setPreferredWidth(25);
        columnModel.getColumn(SequenceTableModel.COLUMN_STATUS).setPreferredWidth(15);
        columnModel.getColumn(SequenceTableModel.COLUMN_LENGTH).setPreferredWidth(15);
        columnModel.getColumn(SequenceTableModel.COLUMN_DESCRIPTION).setPreferredWidth(200);

        setDefaultRenderer(MidiEvent.class, new MidiEventCellRenderer());

        addMouseListener(new SoundSendingMouseAdapter<MidiMessage>() {
            @Override
            protected MidiMessage getValueAt(int row) {
                MidiEvent ev = sequenceTableModel.getTrack().get(row);
                return ev.getMessage();
            }

            @Override
            protected void sendSound(MidiMessage message, AppModel.OutputDirection direction) {
                sequenceTableModel.sendMidiMessage(message, direction);
            }

            @Override
            protected void sendSoundOn(MidiMessage message) {
                sequenceTableModel.sendMidiMessage(message, AppModel.OutputDirection.def);
            }

            @Override
            protected void sendSoundOff() {
            }
        });

        JPopupMenu popupMenu = new JPopupMenu();
        HexViewAction hexViewAction = new HexViewAction();
        JMenuItem hexViewItem = new JMenuItem(hexViewAction);
        getSelectionModel().addListSelectionListener(hexViewAction);
        SaveSysExAction saveSysExAction = new SaveSysExAction();
        getSelectionModel().addListSelectionListener(saveSysExAction);
        JMenuItem saveSysExItem = new JMenuItem(saveSysExAction);
        popupMenu.add(hexViewItem);
        popupMenu.add(saveSysExItem);

        setComponentPopupMenu(popupMenu);
    }

    private static class MidiEventCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel cellComp = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            MidiEvent event = (MidiEvent) value;
            String text;
            if (event.getMessage() instanceof ShortMessage) {
                ShortMessage sm = (ShortMessage) event.getMessage();
                text = String.format("%s: %02X %02X %02X", sm.getClass().getSimpleName(), sm.getStatus(), sm.getData1(), sm.getData2());
            } else if (event.getMessage() instanceof SysexMessage) {
                SysexMessage sysex = (SysexMessage) event.getMessage();
                SynthFactory synthFactory = SynthFactoryRegistry.INSTANCE.getSynthFactory(sysex);
                if (synthFactory != null) {
                    if (synthFactory.isMulti(sysex)) {
                        text = String.format("%s multi", synthFactory.getSynthId());
                    } else {
                        text = String.format("%s single", synthFactory.getSynthId());
                    }
                } else {
                    text = String.format("sysex: %02X", event.getMessage().getStatus());
                }
            } else {
                text = String.format("%s: %02X", event.getMessage().getClass().getSimpleName(), event.getMessage().getStatus());
            }

            cellComp.setText(text);
            return cellComp;
        }
    }

    public java.util.List<MidiEvent> getSelectedEvents() {
        ListSelectionModel selectionModel = getSelectionModel();
        int firstSelRow = selectionModel.getMinSelectionIndex();
        if (firstSelRow < 0) {
            return null;
        }
        int lastSelRow = selectionModel.getMaxSelectionIndex();
        List<MidiEvent> events = new ArrayList<>();
        for (int row = firstSelRow; row <= lastSelRow; row++) {
            if (selectionModel.isSelectedIndex(row)) {
                MidiEvent event = (MidiEvent) getValueAt(row, SequenceTableModel.COLUMN_DESCRIPTION);
                events.add(event);
            }
        }
        return events;
    }

    private abstract class SysExAction extends AbstractAction implements ListSelectionListener {

        protected SysExAction(String name) {
            super(name);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            List<MidiEvent> selectedEvents = getSelectedEvents();
            if (!selectedEvents.isEmpty() && selectedEvents.get(0).getMessage() instanceof SysexMessage) {
                setEnabled(true);
            } else {
                setEnabled(true);
            }
        }
    }

    private class HexViewAction extends SysExAction {
        public HexViewAction() {
            super("Hex View");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            List<MidiEvent> selectedEvents = getSelectedEvents();
            if (selectedEvents.isEmpty()) {
                return;
            }
            Window mainWindow = SwingUtilities.windowForComponent(SequenceTable.this);
            new HexViewFrame(mainWindow, selectedEvents.get(0).getMessage().getMessage()).setVisible(true);
        }
    }

    private class SaveSysExAction extends SysExAction {
        public SaveSysExAction() {
            super("Save SysEx");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            List<MidiEvent> selectedEvents = getSelectedEvents();
            if (selectedEvents.isEmpty()) {
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            int result = fileChooser.showSaveDialog(SequenceTable.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] sysEx = selectedEvents.get(0).getMessage().getMessage();
                    fos.write(sysEx);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Action failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    throw new RuntimeException(ex);
                }
            }
        }
    }

}
