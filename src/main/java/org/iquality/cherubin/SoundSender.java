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

    public void sendSoundWithDelay(SingleSound sound) {
        sendSoundWithDelay(sound, getDefaultDirection());
    }

    public void sendSoundWithDelay(SingleSound sound, AppModel.OutputDirection outputDirection) {
        sendSound(sound, outputDirection);
        try {
            Thread.sleep(SOUND_DUMP_DELAY);
        } catch (InterruptedException ignored) {
        }
    }

    public void sendSound(SingleSound sound, boolean sendToEditBuffer) {
        sendSound(sound, sendToEditBuffer, getDefaultDirection());
    }

    private AppModel.OutputDirection getDefaultDirection() {
        return appModel.getDefaultOutputDirection();
    }

    public void sendSound(SingleSound sound, AppModel.OutputDirection outputDirection) {
        sendSound(sound, false, outputDirection);
    }

    public void sendSound(SingleSound sound, boolean sendToEditBuffer, AppModel.OutputDirection outputDirection) {
        if (sound.isEmpty()) {
            return;
        }
        if (sendToEditBuffer) {
            withReceiver(outputDirection, rcv -> rcv.send(sound.cloneForEditBuffer().getSysEx(), -1));
        } else {
            System.out.println("Sending " + sound.getName() + " to bank " + sound.getBank() + " and slot " + sound.getSlot());
            withReceiver(outputDirection, rcv -> rcv.send(sound.getSysEx(), -1));
        }
    }
}
