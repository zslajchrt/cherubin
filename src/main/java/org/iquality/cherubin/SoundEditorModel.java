package org.iquality.cherubin;

import javax.sound.midi.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SoundEditorModel {

    private static final Object SEND_SOUND = "sendSound";

    private final MidiServices midiServices;

    private MidiDevice dumpInputDevice;
    private Transmitter dumpTransmitter;
    private SoundDumpReceiver dumpReceiver;
    private boolean audition = true;

    private final Map<MidiDevice, Receiver> auditionReceivers = new HashMap<>();

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

    public MidiDevice startListeningToDump(Supplier<Boolean> activeFlag, int inputVariant) {
        if (editedSound == null) {
            return null;
        }
        try {
            dumpInputDevice = appModel.getInputDevice(editedSound.getSynthFactory(), inputVariant);
            dumpInputDevice.open();
            dumpTransmitter = dumpInputDevice.getTransmitter();
            dumpReceiver = new SoundDumpReceiver() {
                @Override
                protected void onSingleSoundDump(SysexMessage sysEx) {
                    if (activeFlag.get() && !(editedSound instanceof MultiSound)) {
                        editedSound.setSysEx(sysEx, true);
                        fireEditedSoundUpdated(editedSound);
                    }
                }

                @Override
                protected void onMultiSoundDump(SysexMessage sysEx) {
                    if (activeFlag.get() && (editedSound instanceof MultiSound)) {
                        editedSound.setSysEx(sysEx, true);
                        fireEditedSoundUpdated(editedSound);
                    }
                }
            };
            dumpTransmitter.setReceiver(dumpReceiver);

            return dumpInputDevice;
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
            //SysexMessage sysEx = sound.getSysEx();
            midiServices.sendMessage(outputDevice, sysEx);
        }
        if (audition) {
            midiServices.probeNoteOn(outputDevice);
        }
    }

    public void sendSoundOff(Sound sound, int outputVariant) {
        MidiDevice outputDevice = appModel.getOutputDevice(sound.getSynthFactory(), outputVariant);
        midiServices.probeNoteOff(outputDevice);
    }

    public boolean sendSoundWithDelayIgnoreEmpty(Sound sound, int outputVariant) {
        if (sound.isInit()) {
            return false;
        }

        MidiDevice outputDevice = appModel.getOutputDevice(sound.getSynthFactory(), outputVariant);
        midiServices.sendMessage(outputDevice, sound.getSysEx());
        MidiServices.delay();

        return true;
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
                MidiDevice dumpInDevice = startListeningToDump(activeFlag, MidiDeviceManager.getOutputVariant(e));
                if (dumpInDevice != null) {
                    ch.setToolTipText("Listening to " + dumpInDevice);
                }
            } else {
                stopListeningToDump();
                ch.setToolTipText("Inactive");
            }
        });
        ch.setToolTipText("Inactive");
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
        if (!audition) {
            closeAuditionReceivers();
        }
    }

    private synchronized void closeAuditionReceivers() {
        for (Map.Entry<MidiDevice, Receiver> deviceReceiverEntry : auditionReceivers.entrySet()) {
            try {
                deviceReceiverEntry.getValue().close();
            } finally {
                deviceReceiverEntry.getKey().close();
            }
        }
        auditionReceivers.clear();
    }

    public boolean isAudition() {
        return audition;
    }

    private synchronized Receiver getAuditionReceiver(MidiDevice device) {
        return auditionReceivers.computeIfAbsent(device, (dev) -> {
            try {
                dev.open();
                return dev.getReceiver();
            } catch (MidiUnavailableException e) {
                throw new RuntimeException(e);
            }
        });
    }

    <T extends Sound> void installTableBehavior(SoundEditorTableBehavior soundEditorTable) {
        JTable table = soundEditorTable.getJTable();
        Supplier<Boolean> activeFlag = soundEditorTable::isActive;

        appModel.getMidiDeviceManager().addGlobalActivityListener((device, message, timeStamp) -> {
            if (activeFlag.get() && audition && editedSound != null && device.isSystemDevice(MidiDeviceManager.SystemDeviceType.controller)) {
                MidiDevice outputDevice = appModel.getOutputDevice(editedSound.getSynthFactory());
                Receiver receiver = getAuditionReceiver(outputDevice);
                receiver.send(message, -1);
            }
            return message;
        }, true);

        table.addMouseListener(new SoundSendingMouseAdapter<T>() {
            @Override
            protected T getValueAt(int row) {
                return (T) table.getValueAt(row, soundEditorTable.getSoundColumn());
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
        table.getActionMap().put(SEND_SOUND, new SendSoundAction(table, soundEditorTable.getSoundColumn()) {
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

        table.getColumnModel().getColumn(soundEditorTable.getSoundColumn()).setCellEditor(new DefaultCellEditor(new JTextField()));

        if (soundEditorTable.getCategoriesColumn() >= 0) {
            JComboBox<SoundCategory> categoryComboBox = new JComboBox<>(SoundCategory.values());
            table.getColumnModel().getColumn(soundEditorTable.getCategoriesColumn()).setCellEditor(new DefaultCellEditor(categoryComboBox));
        }

        KeyStroke deleteKey = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
        table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(deleteKey, "deleteSound");
        table.getActionMap().put("deleteSound", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) {
                    return;
                }

                soundEditorTable.deleteSound(row);

//                Sound sound = (Sound) table.getValueAt(row, soundEditorTable.getSoundColumn());
//                System.out.println("Delete sound " + sound);
//                soundEditorTable.deleteSound(row);
            }
        });

    }

}
