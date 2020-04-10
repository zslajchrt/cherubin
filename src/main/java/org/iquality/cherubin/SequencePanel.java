package org.iquality.cherubin;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SequencePanel extends JPanel implements AppExtension {

    private final SequenceModel sequenceModel;
    private final JTabbedPane tabbedPane;

    public SequencePanel(SequenceModel sequenceModel) {
        super(new BorderLayout());
        this.sequenceModel = sequenceModel;
        tabbedPane = new JTabbedPane();

        rebuildTrackTabs();

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void rebuildTrackTabs() {
        tabbedPane.removeAll();
        for (int i = 0; i < sequenceModel.getSequence().getTracks().length; i++) {
            SequenceTable sequenceTable = new SequenceTable(new SequenceTableModel(sequenceModel, i));
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
                        System.out.println("P:" + timeStamp + ":" + message.getStatus());
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

}
