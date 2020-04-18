package org.iquality.cherubin;

import javax.sound.midi.*;
import java.util.function.Consumer;

import static org.iquality.cherubin.SoundEditorModel.SOUND_DUMP_DELAY;

public class SoundSender {

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

    public void sendSound(MidiDevice outputDevice, MidiMessage message) {
        withReceiver(outputDevice, rcv -> rcv.send(Utils.printSysExDump(message), -1));
    }

    public void sendAllSoundsOff() {
        withReceiver(null, (rcv) -> {
            try {
                // do an all sounds off (some synths don't properly respond to all notes off)
                for(int i = 0; i < 16; i++)
                    rcv.send(new ShortMessage(ShortMessage.CONTROL_CHANGE, i, 120, 0), -1);
                // do an all notes off (some synths don't properly respond to all sounds off)
                for(int i = 0; i < 16; i++)
                    rcv.send(new ShortMessage(ShortMessage.CONTROL_CHANGE, i, 123, 0), -1);
            } catch (InvalidMidiDataException e) {
                e.printStackTrace();
            }
        });
    }

}
