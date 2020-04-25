package org.iquality.cherubin;

import javax.sound.midi.*;
import java.util.function.Consumer;

public class SoundSender {

    public static final int ALL_SOUNDS_OFF_DELAY = 50; // just a tentative value

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

    private static void withReceiver(MidiDevice outputDevice, Consumer<Receiver> worker) {
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

    public static void sendAllSoundsOff() {
        MidiDeviceManager.broadcast(device -> {
            withReceiver(device, rcv -> {
                System.out.println("Sending all sounds off to " + device + " started");
                // do an all sounds off (some synths don't properly respond to all notes off)
                for (int i = 0; i < 16; i++) {
                    rcv.send(newControlChangeMessage(i, 120), -1);
                    MidiDeviceManager.delay(ALL_SOUNDS_OFF_DELAY);
                }
                // do an all notes off (some synths don't properly respond to all sounds off)
                for (int i = 0; i < 16; i++) {
                    rcv.send(newControlChangeMessage(i, 123), -1);
                    MidiDeviceManager.delay(50);
                }
                System.out.println("Sending all sounds off to " + device + " finished");
            });
        });
    }

    private static ShortMessage newControlChangeMessage(int i1, int i2) {
        try {
            return new ShortMessage(ShortMessage.CONTROL_CHANGE, i1, i2, 0);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

}
