package org.iquality.cherubin;

import com.sun.media.sound.MidiUtils;

import javax.sound.midi.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class MidiServices {

    private static final int DEFAULT_DELAY = 50; // just a tentative value

    private final ShortMessage probeNoteOn;
    private final ShortMessage probeNoteOff;

    private Transmitter recordingTransmitter;
    private Transmitter playingTransmitter;
    private ReceiverWrapper playingReceiver;
    private ReceiverWrapper recordingReceiver;

    private MidiDevice recordingInputMidiDevice;
    private MidiDevice playingOutputMidiDevice;

    private Sequencer sequencer;


    public MidiServices() {
        try {
            probeNoteOn = new ShortMessage();
            // Start playing the note Middle C (60),
            // moderately loud (velocity = 93).
            probeNoteOn.setMessage(ShortMessage.NOTE_ON, 0, 60, 93);

            probeNoteOff = new ShortMessage();
            // Start playing the note Middle C (60),
            // moderately loud (velocity = 93).
            probeNoteOff.setMessage(ShortMessage.NOTE_OFF, 0, 60, 0);

            this.sequencer = MidiSystem.getSequencer(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Sequence loadSequence(File file) {

        try {
            if (file.getName().endsWith(".syx")) {
                // Load a single sysex file and create a single-track/single-event MIDI sequence
                Sequence sequence = new Sequence(Sequence.SMPTE_24, 10);
                Track track = sequence.createTrack();

                int length = (int) file.length();
                byte[] data = new byte[length];
                try (FileInputStream fis = new FileInputStream(file)) {
                    int rc = fis.read(data);
                    assert rc == length;
                    track.add(new MidiEvent(new SysexMessage(data, data.length), 0));
                    return sequence;
                }

            } else {
                // Load a MIDI file
                return MidiSystem.getSequence(file);
            }
        } catch (InvalidMidiDataException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveSequence(Sequence sequence, File file) {
        try {
            int[] midiFileTypes = MidiSystem.getMidiFileTypes(sequence);
            MidiSystem.write(sequence, midiFileTypes[0], file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static MidiDevice openDevice(MidiDevice device) throws MidiUnavailableException {
        device.open();
        return device;
    }

    private static void withReceiver(MidiDevice outputDevice, Consumer<Receiver> worker) {
        try (MidiDevice dev = openDevice(outputDevice); Receiver receiver = dev.getReceiver()) {
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

    public void sendMessage(MidiDevice outputDevice, MidiMessage message) {
        withReceiver(outputDevice, rcv -> rcv.send(Utils.printSysExDump(message), -1));
    }

    public static void sendAllSoundsOff() {
        MidiDeviceManager.broadcast(device -> {
            withReceiver(device, rcv -> {
                System.out.println("Sending all sounds off to " + device + " started");
                // do an all sounds off (some synths don't properly respond to all notes off)
                for (int i = 0; i < 16; i++) {
                    rcv.send(newControlChangeMessage(i, 120), -1);
                    delay(DEFAULT_DELAY);
                }
                // do an all notes off (some synths don't properly respond to all sounds off)
                for (int i = 0; i < 16; i++) {
                    rcv.send(newControlChangeMessage(i, 123), -1);
                    delay(50);
                }
                System.out.println("Sending all sounds off to " + device + " finished");
            });
        });
    }

    public static void delay(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException ignored) {
        }
    }

    public static void delay() {
        delay(DEFAULT_DELAY);
    }

    private static ShortMessage newControlChangeMessage(int i1, int i2) {
        try {
            return new ShortMessage(ShortMessage.CONTROL_CHANGE, i1, i2, 0);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMidiMessage(MidiMessage message, MidiDevice outputDevice) {
        withReceiver(outputDevice, receiver -> {
            receiver.send(message, -1);
        });
    }

    public void sendMidiMessages(List<MidiMessage> midiMessages, MidiDevice outputDevice) {
        withReceiver(outputDevice, receiver -> {
            for (MidiMessage midiMessage : midiMessages) {
                receiver.send(midiMessage, -1);
                delay();
            }
        });
    }

    public void recordSequence(Sequence sequence, Receiver listener, MidiDevice inputDevice) {
        try {
            sequencer.open();
            sequencer.setSequence(sequence);
            sequencer.setMasterSyncMode(Sequencer.SyncMode.MIDI_SYNC);
            sequencer.recordEnable(sequence.getTracks()[0], -1);
            recordingInputMidiDevice = inputDevice;
            recordingInputMidiDevice.open();
            long tickOffset = MidiUtils.tick2microsecond(sequence, sequence.getTickLength(), null);
            recordingTransmitter = recordingInputMidiDevice.getTransmitter();
            recordingReceiver = new ReceiverWrapper(sequencer.getReceiver(), listener, tickOffset);
            recordingTransmitter.setReceiver(recordingReceiver);
            sequencer.startRecording();
            //recordingInputMidiDevice.getTransmitter().setReceiver(sequencer.getReceiver());
            sequencer.setTickPosition(sequence.getTickLength());
        } catch (MidiUnavailableException | InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopRecordingSequence() {
        if (sequencer.isRecording()) {
            sequencer.stopRecording();
        }
        if (recordingTransmitter != null) {
            recordingTransmitter.close();
            recordingTransmitter = null;
        }
        if (recordingReceiver != null) {
            recordingReceiver.close();
            recordingReceiver = null;
        }
        if (recordingInputMidiDevice != null) {
            recordingInputMidiDevice.close();
            recordingInputMidiDevice = null;
        }
        sequencer.recordDisable(sequencer.getSequence().getTracks()[0]);
        fixTracks(sequencer.getSequence());
    }

    private void fixTracks(Sequence sequence) {
        for (Track oldTrack : sequence.getTracks()) {
            Track newTrack = sequence.createTrack();
            fixTrack(oldTrack, newTrack);
            sequence.deleteTrack(oldTrack);
        }
    }

    private static void fixTrack(Track trackSrc, Track trackDst) {
        for (int i = 0; i < trackSrc.size(); i++) {
            MidiEvent midiEvent = trackSrc.get(i);
            MidiMessage newMessage = fixMessage(midiEvent.getMessage());
            midiEvent = new MidiEvent(newMessage, midiEvent.getTick());
            trackDst.add(midiEvent);
        }
    }

    private static MidiMessage fixMessage(MidiMessage message) {
        try {
            if (message instanceof ShortMessage) {
                return new ShortMessage(message.getStatus(), ((ShortMessage) message).getData1(), ((ShortMessage) message).getData2());
            }
            return message;
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    public void playSequence(Sequence sequence, Receiver listener, MidiDevice outputDevice) {
        try {
            sequencer = MidiSystem.getSequencer(false);
            sequencer.open();
            sequencer.setTickPosition(0);
            playingOutputMidiDevice = outputDevice;
            playingOutputMidiDevice.open();
            sequencer.setSequence(sequence);
            playingTransmitter = sequencer.getTransmitter();
            playingReceiver = new ReceiverWrapper(playingOutputMidiDevice.getReceiver(), listener, 0);
            playingTransmitter.setReceiver(playingReceiver);
            //sequencer.getTransmitter().setReceiver(playingOutputMidiDevice.getReceiver());
            sequencer.start();
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopPlayingSequence() {
        if (sequencer.isRunning()) {
            sequencer.stop();
        }
        if (playingTransmitter != null) {
            playingTransmitter.close();
            playingTransmitter = null;
        }
        if (playingReceiver != null) {
            playingReceiver.close();
            playingReceiver = null;
        }
        if (playingOutputMidiDevice != null) {
            playingOutputMidiDevice.close();
            playingOutputMidiDevice = null;
        }
    }

    public static Sequence createSequenceWithOneTrack() {
        try {
            //Sequence sequence = new Sequence(Sequence.PPQ, 10);
            Sequence sequence = new Sequence(Sequence.SMPTE_24, 10);
            sequence.createTrack();
            return sequence;
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ReceiverWrapper implements Receiver {
        final Receiver delegate;
        private final Receiver listener;
        final long tickOffset;
        final long startTime = System.nanoTime();

        public ReceiverWrapper(Receiver delegate, Receiver listener, long tickOffset) {
            this.delegate = delegate;
            this.listener = listener;
            this.tickOffset = tickOffset;
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (timeStamp < 0) {
                timeStamp = (System.nanoTime() - startTime) / 1000;
            }
            timeStamp += tickOffset;
            message = fixMessage(message);
            delegate.send(message, timeStamp);
            if (listener != null) {
                listener.send(message, timeStamp);
            }
        }

        @Override
        public void close() {
            delegate.close();
            if (listener != null) {
                listener.close();
            }
        }
    }


}
