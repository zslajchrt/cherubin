package org.iquality.cherubin;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.swing.*;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SequenceTable extends JTable implements AppExtension {
    private final SequenceTableModel sequenceTableModel;

    public SequenceTable(SequenceTableModel dm) {
        super(dm);

        sequenceTableModel = dm;

        TableColumnModel columnModel = getColumnModel();

        columnModel.getColumn(SequenceTableModel.COLUMN_TIMESTAMP).setPreferredWidth(25);
        columnModel.getColumn(SequenceTableModel.COLUMN_STATUS).setPreferredWidth(15);
        columnModel.getColumn(SequenceTableModel.COLUMN_LENGTH).setPreferredWidth(15);
        columnModel.getColumn(SequenceTableModel.COLUMN_DESCRIPTION).setPreferredWidth(200);
    }

    @Override
    public String getExtensionName() {
        return "Sequencer";
    }

    @Override
    public void activate() {

    }

    @Override
    public void deactivate() {

    }

    @Override
    public List<Component> getToolbarComponents() {
        List<Component> buttons = new ArrayList<>();
        buttons.add(makeLoadButton());
        buttons.add(makeSaveButton());
        buttons.add(makeRecordButton());
        buttons.add(makePlayButton());
        buttons.add(makeClearButton());
        return buttons;
    }

    protected JButton makeLoadButton() {
        return AppFrame.makeButton("loadSequence", "Load a sequence", "Load", (actionEvent -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            int result = fileChooser.showOpenDialog(SequenceTable.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                sequenceTableModel.loadSequence(selectedFile);
            }
        }));
    }

    protected JButton makeSaveButton() {
        return AppFrame.makeButton("saveSequence", "Save a sequence", "Save", (actionEvent -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            int result = fileChooser.showSaveDialog(SequenceTable.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                sequenceTableModel.saveSequence(file);
            }
        }));
    }

    protected JButton makeRecordButton() {
        return AppFrame.makeButton("recordSequence", "Record a sequence", "Record", (actionEvent -> {
            JButton button = (JButton) actionEvent.getSource();
            if ("recordSequence".equals(actionEvent.getActionCommand())) {
                button.setActionCommand("stopRecording");
                button.setText("Stop");
                sequenceTableModel.recordSequence(new Receiver() {
                    @Override
                    public void send(MidiMessage message, long timeStamp) {
                        System.out.print("*");
                    }

                    @Override
                    public void close() {
                    }
                });
            } else if ("stopRecording".equals(actionEvent.getActionCommand())) {
                button.setActionCommand("recordSequence");
                button.setText("Record");
                sequenceTableModel.stopRecordingSequence();
            }
        }));
    }

    protected JButton makePlayButton() {
        return AppFrame.makeButton("playSequence", "Play a sequence", "Play", (actionEvent -> {
            JButton button = (JButton) actionEvent.getSource();
            if ("playSequence".equals(actionEvent.getActionCommand())) {
                button.setActionCommand("stopPlaying");
                button.setText("Stop");
                sequenceTableModel.playSequence(new Receiver() {
                    @Override
                    public void send(MidiMessage message, long timeStamp) {
                        System.out.print(".");
                    }

                    @Override
                    public void close() {

                    }
                });
            } else if ("stopPlaying".equals(actionEvent.getActionCommand())) {
                button.setActionCommand("playSequence");
                button.setText("Play");
                sequenceTableModel.stopPlayingSequence();
            }
        }));
    }

    protected JButton makeClearButton() {
        return AppFrame.makeButton("clearSequence", "Clear the sequence", "Clear", (actionEvent -> {
            sequenceTableModel.clearSequence();
        }));
    }


    public static void main(String[] args) throws Exception {
        JFrame frame = new JFrame("Cherubin - Midi Librarian");

        SequenceTableModel tableModel = new SequenceTableModel(new AppModel(new NullMidiPort(), new NullMidiPort()));
        SequenceTable table = new SequenceTable(tableModel);

        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
    }
}
