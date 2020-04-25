package org.iquality.cherubin;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SoundEditorModel {

    private static final Object SEND_SOUND = "sendSound";

    private final MidiServices midiServices;

    private MidiDevice dumpInputDevice;
    private Transmitter dumpTransmitter;
    private SoundDumpReceiver dumpReceiver;
    private boolean audition = true;

    public interface SoundEditorModelListener {
        void editedSoundSelected(Sound sound);

        void editedSoundUpdated(Sound sound);

        void editedSoundCleared(Sound sound);
    }

    protected final AppModel appModel;
    private final List<SoundEditorModelListener> soundEditorModelListeners = new ArrayList<>();

    private Sound editedSound;

    public SoundEditorModel(AppModel appModel) {
        this.appModel = appModel;
        this.midiServices = new MidiServices();
    }

    public void addSoundEditorModelListener(SoundEditorModelListener listener) {
        soundEditorModelListeners.add(listener);
    }

    public void removeSoundEditorModelListener(SoundEditorModelListener listener) {
        soundEditorModelListeners.remove(listener);
    }

    private void fireEditedSoundSelected(Sound sound) {
        for (SoundEditorModelListener soundEditorModelListener : soundEditorModelListeners) {
            soundEditorModelListener.editedSoundSelected(sound);
        }
    }

    private void fireEditedSoundUpdated(Sound sound) {
        for (SoundEditorModelListener soundEditorModelListener : soundEditorModelListeners) {
            soundEditorModelListener.editedSoundUpdated(sound);
        }
    }

    private void fireEditedSoundCleared(Sound sound) {
        for (SoundEditorModelListener soundEditorModelListener : soundEditorModelListeners) {
            soundEditorModelListener.editedSoundCleared(sound);
        }
    }

    public Sound getEditedSound() {
        return editedSound;
    }

    public void setEditedSound(Sound sound) {
        this.editedSound = sound;
        fireEditedSoundSelected(sound);
    }

    public void clearEditedSound() {
        Sound cleared = this.editedSound;
        this.editedSound = null;
        fireEditedSoundCleared(cleared);
    }

    public void startListeningToDump(Supplier<Boolean> activeFlag) {
        if (editedSound == null) {
            return;
        }
        try {
            dumpInputDevice = appModel.getInputDevice(editedSound.getSynthFactory(), 0);
            dumpInputDevice.open();
            dumpTransmitter = dumpInputDevice.getTransmitter();
            dumpReceiver = new SoundDumpReceiver() {
                @Override
                protected void onSingleSoundDump(SysexMessage sysEx) {
                    if (activeFlag.get() && !(editedSound instanceof MultiSound)) {
                        editedSound.setSysEx(sysEx);
                        fireEditedSoundUpdated(editedSound);
                    }
                }

                @Override
                protected void onMultiSoundDump(SysexMessage sysEx) {
                    if (activeFlag.get() && (editedSound instanceof MultiSound)) {
                        editedSound.setSysEx(sysEx);
                        fireEditedSoundUpdated(editedSound);
                    }
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
        if (dumpInputDevice != null) {
            dumpInputDevice.close();
        }
    }

    public boolean isListeningToDump() {
        return dumpTransmitter != null;
    }

    public void sendSoundOn(Sound sound, int outputVariant) {
        MidiDevice outputDevice = appModel.getOutputDevice(sound.getSynthFactory(), outputVariant);
        midiServices.probeNoteOff(outputDevice);
        if (!sound.isInit()) {
            // Do not update the synth's edit buffer with the initial (empty) sound
            SysexMessage sysEx = sound.cloneForEditBuffer().getSysEx();
            midiServices.sendSound(outputDevice, sysEx);
        }
        if (audition) {
            midiServices.probeNoteOn(outputDevice);
        }
    }

    public void sendSoundOff(Sound sound, int outputVariant) {
        MidiDevice outputDevice = appModel.getOutputDevice(sound.getSynthFactory(), outputVariant);
        midiServices.probeNoteOff(outputDevice);
    }

    public void sendSoundWithDelayIgnoreEmpty(Sound sound, int outputVariant) {
        if (sound.isInit()) {
            return;
        }
        MidiDevice outputDevice = appModel.getOutputDevice(sound.getSynthFactory(), outputVariant);
        midiServices.sendSound(outputDevice, sound.getSysEx());
        MidiServices.delay();
    }

    public JCheckBox makeSoundDumpCheckBox(Supplier<Boolean> activeFlag) {
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

    public JCheckBox makeAuditionCheckBox() {
        JCheckBox ch = new JCheckBox("Audition");
        ch.setSelected(isAudition());
        ch.addActionListener(e -> {
            setAudition(ch.isSelected());
        });
        return ch;
    }

    public void setAudition(boolean flag) {
        audition = flag;
    }

    public boolean isAudition() {
        return audition;
    }

    <T extends Sound> void installTableBehavior(JTable table, int soundColumn, int categoriesColumn) {
        table.addMouseListener(new SoundSendingMouseAdapter<T>() {
            @Override
            protected T getValueAt(int row) {
                return (T) table.getValueAt(row, soundColumn);
            }

            @Override
            protected void sendSoundOn(T sound, int outputVariant) {
                SoundEditorModel.this.setEditedSound(sound);

                appModel.getExecutor().execute(() -> {
                    SoundEditorModel.this.sendSoundOn(sound, outputVariant);
                    MidiServices.delay();
                });
            }

            @Override
            protected void sendSoundOff(T sound, int outputVariant) {
                SoundEditorModel.this.sendSoundOff(sound, outputVariant);
            }
        });


        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(enter, SEND_SOUND);
        table.getActionMap().put(SEND_SOUND, new SendSoundAction(table, soundColumn) {
            @Override
            protected void onSound(Sound sound, int outputVariant, boolean on) {
                if (on) {
                    SoundEditorModel.this.sendSoundOn(sound, outputVariant);
                    SoundEditorModel.this.setEditedSound(sound);
                } else {
                    SoundEditorModel.this.sendSoundOff(sound, outputVariant);
                }
            }
        });

        table.setColumnSelectionAllowed(true);

        if (categoriesColumn >= 0) {
            JComboBox<SoundCategory> categoryComboBox = new JComboBox<>(SoundCategory.values());
            table.getColumnModel().getColumn(SoundDbTableModel.COLUMN_CATEGORY).setCellEditor(new DefaultCellEditor(categoryComboBox));
        }
    }

}
