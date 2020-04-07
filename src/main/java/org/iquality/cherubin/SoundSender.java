package org.iquality.cherubin;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

public class SoundSender extends MidiPortCommunicator {


    private final ShortMessage probeNoteOn;
    private final ShortMessage probeNoteOff;
    private final Receiver receiver;

    public SoundSender(MidiDevice midiOutPort) {
        super(midiOutPort);

        try {
            probeNoteOn = new ShortMessage();
            // Start playing the note Middle C (60),
            // moderately loud (velocity = 93).
            probeNoteOn.setMessage(ShortMessage.NOTE_ON, 0, 60, 93);

            probeNoteOff = new ShortMessage();
            // Start playing the note Middle C (60),
            // moderately loud (velocity = 93).
            probeNoteOff.setMessage(ShortMessage.NOTE_OFF, 0, 60, 0);

            device.open();
            receiver = device.getReceiver();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void probeNoteOn() {
        receiver.send(probeNoteOn, -1);
    }

    public void probeNoteOff() {
        receiver.send(probeNoteOff, -1);
    }

    public void sendSound(SingleSound sound) {
        receiver.send(sound.cloneForEditBuffer().dump, -1);
    }
}
