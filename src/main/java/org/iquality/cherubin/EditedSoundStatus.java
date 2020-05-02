package org.iquality.cherubin;

import javax.sound.midi.MidiDevice;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class EditedSoundStatus extends JPanel {

    private final SoundEditorModel soundEditorModel;
    private final JLabel editedSoundLabel;

    public EditedSoundStatus(SoundEditorModel soundEditorModel) {
        super(new FlowLayout(FlowLayout.LEFT));

        this.soundEditorModel = soundEditorModel;

        editedSoundLabel = new JLabel("No sound in buffer");

        soundEditorModel.addSoundEditorModelListener(new SoundEditorModel.SoundEditorModelListener() {
            @Override
            public void editedSoundSelected(Sound sound) {
                updateEditedSoundLabel();
            }

            @Override
            public void editedSoundUpdated(Sound sound) {
                updateEditedSoundLabel();
            }

            @Override
            public void editedSoundCleared(Sound sound) {
                updateEditedSoundLabel();
            }
        });

        add(editedSoundLabel);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new ShiftKeyDispatcher() {
            @Override
            protected void onShiftPressed(KeyEvent e) {
                Sound editedSound = soundEditorModel.getEditedSound();
                if (editedSound != null) {
                    int outputVariant = MidiDeviceManager.getOutputVariant(e);
                    MidiDevice outputDevice = MidiDeviceManager.INSTANCE.getOutputDevice(editedSound.getSynthFactory(), outputVariant);
                    editedSoundLabel.setText("" + outputDevice);
                } else {
                    editedSoundLabel.setText("");
                }
            }

            @Override
            protected void onShiftReleased(KeyEvent e) {
                updateEditedSoundLabel();
            }
        });
    }

    void updateEditedSoundLabel() {
        Sound editedSound = soundEditorModel.getEditedSound();
        if (editedSound != null) {
            editedSoundLabel.setText("" + editedSound);
        } else {
            editedSoundLabel.setText("No sound in buffer");
        }
    }

}
