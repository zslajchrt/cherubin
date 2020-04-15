package org.iquality.cherubin;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SoundEditorModel {

    private static final Object SEND_SOUND = "sendSound";

    private final SoundSender soundSender;

    private MidiDevice dumpInputDevice;
    private Transmitter dumpTransmitter;
    private SoundDumpReceiver dumpReceiver;

    public interface SoundEditorModelListener {
        void editedSoundSelected(Sound sound);

        void editedSoundUpdated(Sound sound);
    }

    protected final AppModel appModel;
    private final List<SoundEditorModelListener> soundEditorModelListeners = new ArrayList<>();

    private Sound editedSound;

    public SoundEditorModel(AppModel appModel) {
        this.appModel = appModel;
        this.soundSender = new SoundSender(appModel);
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

    public Sound getEditedSound() {
        return editedSound;
    }

    public void setEditedSound(Sound sound) {
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
    }

    public void sendSoundOn(Sound sound) {
        soundSender.probeNoteOff();
        soundSender.sendSound(sound, true, true);
        soundSender.probeNoteOn();
    }

    public void sendSoundOff() {
        soundSender.probeNoteOff();
    }

    public void sendSound(Sound sound, AppModel.OutputDirection outputDirection) {
        soundSender.sendSound(sound, true, outputDirection, true);
    }

    public void sendSoundWithDelayIgnoreEmpty(Sound sound) {
        soundSender.sendSoundWithDelay(sound, false);
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

    <T extends Sound> void installTableBehavior(JTable table, int soundColumn, int categoriesColumn) {
        table.addMouseListener(new SoundSendingMouseAdapter<T>() {
            @Override
            protected T getValueAt(int row) {
                return (T) table.getValueAt(row, soundColumn);
            }

            @Override
            protected void sendSound(T sound, AppModel.OutputDirection direction) {
                SoundEditorModel.this.sendSound(sound, direction);
                SoundEditorModel.this.setEditedSound(sound);
            }

            @Override
            protected void sendSoundOn(T sound) {
                SoundEditorModel.this.sendSoundOn(sound);
                SoundEditorModel.this.setEditedSound(sound);
            }

            @Override
            protected void sendSoundOff() {
                SoundEditorModel.this.sendSoundOff();
            }
        });


        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(enter, SEND_SOUND);
        table.getActionMap().put(SEND_SOUND, new SendSoundAction(table, soundColumn) {
            @Override
            protected void onSound(Sound sound, boolean on) {
                if (on) {
                    SoundEditorModel.this.sendSoundOn(sound);
                    SoundEditorModel.this.setEditedSound(sound);
                } else {
                    SoundEditorModel.this.sendSoundOff();
                }
            }
        });

        table.setColumnSelectionAllowed(true);

        if (categoriesColumn >= 0) {
            JComboBox<SoundCategory> categoryComboBox = new JComboBox<>(SoundCategory.CATEGORIES);
            table.getColumnModel().getColumn(SoundDbTableModel.COLUMN_CATEGORY).setCellEditor(new DefaultCellEditor(categoryComboBox));
        }
    }

}
