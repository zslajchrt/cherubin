package org.iquality.cherubin;

import javax.sound.midi.*;
import java.util.function.Consumer;

public class SoundSender {

    public static final int SOUND_DUMP_DELAY = 500;

    private final ShortMessage probeNoteOn;
    private final ShortMessage probeNoteOff;

    public SoundSender() {
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

    private void withReceiver(MidiDevice outputDevice, Consumer<Receiver> worker) {
        try (Receiver receiver = outputDevice.getReceiver()) {
            outputDevice.open();
            worker.accept(receiver);
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void probeNoteOn(MidiDevice outputDevice) {
        withReceiver(outputDevice, rcv -> rcv.send(probeNoteOn, -1));
    }

    public void probeNoteOff(MidiDevice outputDevice) {
        withReceiver(outputDevice, rcv -> rcv.send(probeNoteOff, -1));
    }

    public void sendSoundWithDelay(MidiDevice outputDevice, MidiMessage message) {
        sendSound(outputDevice, message);
        try {
            Thread.sleep(SOUND_DUMP_DELAY);
        } catch (InterruptedException ignored) {
        }
    }

    public void sendSound(MidiDevice outputDevice, MidiMessage message) {
        withReceiver(outputDevice, rcv -> rcv.send(message, -1));
    }
}
