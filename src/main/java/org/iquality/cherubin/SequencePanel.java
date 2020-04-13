package org.iquality.cherubin;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SequencePanel extends JPanel implements AppExtension {

    private final SequenceModel sequenceModel;
    private final JTabbedPane tabbedPane;
    private final List<SequenceTable> tabTables = new ArrayList<>();

    private final CopyAction copyAction = new CopyAction();

    public SequencePanel(SequenceModel sequenceModel) {
        super(new BorderLayout());
        this.sequenceModel = sequenceModel;
        tabbedPane = new JTabbedPane();

        rebuildTrackTabs();

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void rebuildTrackTabs() {
        tabbedPane.removeAll();
        tabTables.clear();
        for (int i = 0; i < sequenceModel.getSequence().getTracks().length; i++) {
            SequenceTable sequenceTable = new SequenceTable(new SequenceTableModel(sequenceModel, i));
            sequenceTable.getSelectionModel().addListSelectionListener(copyAction);
            tabTables.add(sequenceTable);
            tabbedPane.add(new JScrollPane(sequenceTable), "Track " + (i + 1));
        }
        ((JScrollPane) tabbedPane.getSelectedComponent()).getViewport().getView().requestFocusInWindow();
    }

    @Override
    public String getExtensionName() {
        return "Sequencer";
    }

    @Override
    public void initialize() {

    }

    @Override
    public void close() {

    }

    @Override
    public void onSelected() {
        ((JScrollPane) tabbedPane.getSelectedComponent()).getViewport().getView().requestFocusInWindow();
    }

    @Override
    public Component getMainPanel() {
        return new JScrollPane(this);
    }

    @Override
    public List<Component> getToolBarComponents() {
        List<Component> buttons = new ArrayList<>();
        buttons.add(makeLoadButton());
        buttons.add(makeSaveButton());
        buttons.add(makeRecordButton());
        buttons.add(makePlayButton());
        buttons.add(makeClearButton());
        return buttons;
    }

    @Override
    public List<Component> getStatusBarComponents() {
        return Collections.emptyList();
    }

    protected JButton makeLoadButton() {
        return AppFrame.makeButton("loadSequence", "Load a sequence", "Load", (actionEvent -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            int result = fileChooser.showOpenDialog(SequencePanel.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                sequenceModel.loadSequence(selectedFile);
                rebuildTrackTabs();
            }
        }));
    }

    protected JButton makeSaveButton() {
        return AppFrame.makeButton("saveSequence", "Save a sequence", "Save", (actionEvent -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            int result = fileChooser.showSaveDialog(SequencePanel.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                sequenceModel.saveSequence(file);
            }
        }));
    }

    protected JButton makeRecordButton() {
        return AppFrame.makeButton("recordSequence", "Record a sequence", "Record", (actionEvent -> {
            JButton button = (JButton) actionEvent.getSource();
            if ("recordSequence".equals(actionEvent.getActionCommand())) {
                button.setActionCommand("stopRecording");
                button.setText("Stop");
                sequenceModel.recordSequence(new Receiver() {
                    @Override
                    public void send(MidiMessage message, long timeStamp) {
                        System.out.println("R:" + timeStamp + ":" + message.getStatus());
                    }

                    @Override
                    public void close() {
                    }
                });
            } else if ("stopRecording".equals(actionEvent.getActionCommand())) {
                button.setActionCommand("recordSequence");
                button.setText("Record");
                sequenceModel.stopRecordingSequence();
                rebuildTrackTabs();
            }
        }));
    }

    protected JButton makePlayButton() {
        return AppFrame.makeButton("playSequence", "Play a sequence", "Play", (actionEvent -> {
            JButton button = (JButton) actionEvent.getSource();
            if ("playSequence".equals(actionEvent.getActionCommand())) {
                button.setActionCommand("stopPlaying");
                button.setText("Stop");
                sequenceModel.playSequence(new Receiver() {
                    @Override
                    public void send(MidiMessage message, long timeStamp) {
                        if (message instanceof ShortMessage) {
                            System.out.printf("%s %d: %02X %02X %02X\n", message.getClass().getSimpleName(), timeStamp, message.getStatus(), ((ShortMessage) message).getData1(), ((ShortMessage) message).getData2());
                        } else {
                            //System.out.printf("%s %d: %02X\n", message.getClass().getSimpleName(), timeStamp, message.getStatus());
                        }
                    }

                    @Override
                    public void close() {

                    }
                });
            } else if ("stopPlaying".equals(actionEvent.getActionCommand())) {
                button.setActionCommand("playSequence");
                button.setText("Play");
                sequenceModel.stopPlayingSequence();
            }
        }));
    }

    protected JButton makeClearButton() {
        return AppFrame.makeButton("clearSequence", "Clear the sequence", "Clear", (actionEvent -> {
            sequenceModel.initializeSequence();
            rebuildTrackTabs();
        }));
    }

    public static DataFlavor MIDI_EVENTS_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + MidiEvents.class.getName(), "MIDI Events");

    private static final DataFlavor[] clipboardFlavors;

    static {
        //clipboardFlavors = new DataFlavor[]{DataFlavor.stringFlavor, MIDI_EVENTS_FLAVOR};
        clipboardFlavors = new DataFlavor[]{MIDI_EVENTS_FLAVOR};
    }

    @Override
    public Action getCopyAction() {
        return copyAction;
    }

    @Override
    public Action getPasteAction() {
        return null;
    }

    class CopyAction extends AbstractAction implements ListSelectionListener {

        {
            setEnabled(false);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                setEnabled(true);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Clipboard clipboard = getSystemClipboard();
            clipboard.setContents(new Transferable() {
                @Override
                public DataFlavor[] getTransferDataFlavors() {
                    return clipboardFlavors;
                }

                @Override
                public boolean isDataFlavorSupported(DataFlavor flavor) {
                    for (DataFlavor soundClipboardFlavor : clipboardFlavors) {
                        if (soundClipboardFlavor.equals(flavor)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                    SequenceTable sequenceTable = tabTables.get(tabbedPane.getSelectedIndex());

                    ListSelectionModel selectionModel = sequenceTable.getSelectionModel();
                    int firstSelRow = selectionModel.getMinSelectionIndex();
                    if (firstSelRow < 0) {
                        return null; // TODO
                    }
                    int lastSelRow = selectionModel.getMaxSelectionIndex();
                    List<MidiEvent> events = new ArrayList<>();
                    for (int row = firstSelRow; row <= lastSelRow; row++) {
                        if (selectionModel.isSelectedIndex(row)) {
                            MidiEvent event = (MidiEvent) sequenceTable.getValueAt(row, SequenceTableModel.COLUMN_DESCRIPTION);
                            events.add(event);
                        }
                    }

                    if (MIDI_EVENTS_FLAVOR.equals(flavor)) {
                        return new MidiEvents(events);
                    } else {
                        throw new UnsupportedFlavorException(flavor);
                    }
                }
            }, null);
        }
    }
}
