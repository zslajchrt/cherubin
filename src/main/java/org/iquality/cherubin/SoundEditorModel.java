package org.iquality.cherubin;

import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class SoundEditorModel {

    private MidiDevice dumpInputDevice;
    private Transmitter dumpTransmitter;
    private BlofeldDumpReceiver dumpReceiver;

    public interface SoundEditorModelListener {
        void editedSoundSelected(SingleSound sound);

        void editedSoundUpdated(SingleSound sound);
    }

    protected final AppModel appModel;
    private final List<SoundEditorModelListener> soundEditorModelListeners = new ArrayList<>();

    private SingleSound editedSound;

    public SoundEditorModel(AppModel appModel) {
        this.appModel = appModel;
    }

    public void addSoundEditorModelListener(SoundEditorModelListener listener) {
        soundEditorModelListeners.add(listener);
    }

    public void removeSoundEditorModelListener(SoundEditorModelListener listener) {
        soundEditorModelListeners.remove(listener);
    }

    private void fireEditedSoundSelected(SingleSound sound) {
        for (SoundEditorModelListener soundEditorModelListener : soundEditorModelListeners) {
            soundEditorModelListener.editedSoundSelected(sound);
        }
    }

    private void fireEditedSoundUpdated(SingleSound sound) {
        for (SoundEditorModelListener soundEditorModelListener : soundEditorModelListeners) {
            soundEditorModelListener.editedSoundUpdated(sound);
        }
    }

    public SingleSound getEditedSound() {
        return editedSound;
    }

    public void setEditedSound(SingleSound sound) {
        this.editedSound = sound;
        fireEditedSoundSelected(sound);
    }

    public void startListeningToDump(Supplier<Boolean> activeFlag) {
        if (editedSound == null) {
            return;
        }
        try {
            dumpInputDevice = appModel.getInputDevice(appModel.getDefaultInputDirection());
            dumpInputDevice.open();
            dumpTransmitter = dumpInputDevice.getTransmitter();
            dumpReceiver = new BlofeldDumpReceiver() {
                @Override
                protected void onSingleSoundDump(String soundName, SoundCategory category, SysexMessage sysEx) {
                    if (activeFlag.get()) {
                        editedSound.update(soundName, sysEx, "", category);
                        fireEditedSoundUpdated(editedSound);
                    }
                }

                @Override
                protected void onMultiSoundDump(String soundName, SysexMessage sysEx) {
                    // TODO
                }
            };
            dumpTransmitter.setReceiver(dumpReceiver);
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopListeningToDump() {
        if (dumpTransmitter != null) {
            dumpTransmitter.close();
            dumpTransmitter = null;
        }
        if (dumpReceiver != null) {
            dumpReceiver.close();
            dumpReceiver = null;
        }
    }

    public Component makeSoundDumpCheckBox(Supplier<Boolean> activeFlag) {
        JCheckBox ch = new JCheckBox("Listen to dump");
        ch.addActionListener(e -> {
            if (ch.isSelected()) {
                if (getEditedSound() == null) {
                    JOptionPane.showMessageDialog(null, "No sound being edited.", "Error", JOptionPane.ERROR_MESSAGE);
                    ch.setSelected(false);
                    return;
                }
                startListeningToDump(activeFlag);
            } else {
                stopListeningToDump();
            }
        });
        return ch;
    }
}
