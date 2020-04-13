package org.iquality.cherubin;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.util.function.Consumer;

public class SoundSender {

    public static final int SOUND_DUMP_DELAY = 500;

    private final ShortMessage probeNoteOn;
    private final ShortMessage probeNoteOff;

    private final AppModel appModel;

    public SoundSender(AppModel appModel) {
        this.appModel = appModel;
        try {
            probeNoteOn = new ShortMessage();
            // Start playing the note Middle C (60),
            // moderately loud (velocity = 93).
            probeNoteOn.setMessage(ShortMessage.NOTE_ON, 0, 60, 93);

            probeNoteOff = new ShortMessage();
            // Start playing the note Middle C (60),
            // moderately loud (velocity = 93).
            probeNoteOff.setMessage(ShortMessage.NOTE_OFF, 0, 60, 0);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void withReceiver(AppModel.OutputDirection outputDirection, Consumer<Receiver> worker) {
        MidiDevice device = openDevice(appModel.getOutputDevice(outputDirection));
        try (Receiver receiver = device.getReceiver()) {
            worker.accept(receiver);
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    private MidiDevice openDevice(MidiDevice outputDevice) {
        try {
            outputDevice.open();
            return outputDevice;
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void probeNoteOn() {
        withReceiver(getDefaultDirection(), rcv -> rcv.send(probeNoteOn, -1));
    }

    public void probeNoteOff() {
        withReceiver(getDefaultDirection(), rcv -> rcv.send(probeNoteOff, -1));
    }

    public void sendSoundWithDelay(Sound sound, boolean sendEmpty) {
        sendSoundWithDelay(sound, getDefaultDirection(), sendEmpty);
    }

    public void sendSoundWithDelay(Sound sound, AppModel.OutputDirection outputDirection, boolean sendEmpty) {
        sendSound(sound, outputDirection, sendEmpty);
        try {
            Thread.sleep(SOUND_DUMP_DELAY);
        } catch (InterruptedException ignored) {
        }
    }

    public void sendSound(Sound sound, boolean sendToEditBuffer, boolean sendEmpty) {
        sendSound(sound, sendToEditBuffer, getDefaultDirection(), sendEmpty);
    }

    private AppModel.OutputDirection getDefaultDirection() {
        return appModel.getDefaultOutputDirection();
    }

    public void sendSound(Sound sound, AppModel.OutputDirection outputDirection, boolean sendEmpty) {
        sendSound(sound, false, outputDirection, sendEmpty);
    }

    public void sendSound(Sound sound, boolean sendToEditBuffer, AppModel.OutputDirection outputDirection, boolean sendEmpty) {
        if (!sendEmpty && sound.isEmpty()) {
            return;
        }
        if (sendToEditBuffer) {
            withReceiver(outputDirection, rcv -> rcv.send(sound.cloneForEditBuffer().getSysEx(), -1));
        } else {
            System.out.println("Sending " + sound.getName() + " to bank " + sound.getBank() + " and slot " + sound.getProgram());
            withReceiver(outputDirection, rcv -> rcv.send(sound.getSysEx(), -1));
        }
    }
}
