package org.iquality.cherubin;

import com.sun.media.sound.MidiUtils;

import javax.sound.midi.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class AppModel {

    private Transmitter recordingTransmitter;
    private Transmitter playingTransmitter;
    private ReceiverWrapper playingReceiver;
    private ReceiverWrapper recordingReceiver;

//    public enum OutputDirection {
//        none(0),
//        left(1),
//        right(2),
//        both(3),
//        def(4);
//
//        final int mask;
//
//        OutputDirection(int mask) {
//            this.mask = mask;
//        }
//
//        public static OutputDirection forMask(int newMask) {
//            switch (newMask) {
//                case 0:
//                    return none;
//                case 1:
//                    return left;
//                case 2:
//                    return right;
//                case 3:
//                default:
//                    return both;
//            }
//        }
//    }
//
//    public enum InputDirection {
//        left,
//        right
//    }

    private Sequencer sequencer;

    private final MidiDeviceManager midiDeviceManager;

    private MidiDevice recordingInputMidiDevice;
    private MidiDevice playingOutputMidiDevice;

//    private InputDirection defaultInputDirection = InputDirection.left;
//    private OutputDirection defaultOutputDirection = OutputDirection.right;

    public AppModel(MidiDeviceManager midiDeviceManager) {
        this.midiDeviceManager = midiDeviceManager;
        try {
            this.sequencer = MidiSystem.getSequencer(false);
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

//    public void setDefaultOutput(OutputDirection direction) {
//        if (direction == OutputDirection.def) {
//            throw new RuntimeException("Invalid default output direction");
//        }
//        this.defaultOutputDirection = direction;
//    }
//
//    public void addDefaultOutput(OutputDirection direction) {
//        if (direction == OutputDirection.def) {
//            throw new RuntimeException("Invalid default output direction");
//        }
//        int newMask = defaultOutputDirection.mask | direction.mask;
//        defaultOutputDirection = OutputDirection.forMask(newMask);
//    }
//
//    public void removeDefaultOutput(OutputDirection direction) {
//        if (direction == OutputDirection.def) {
//            throw new RuntimeException("Invalid default output direction");
//        }
//        int newMask = defaultOutputDirection.mask & ~direction.mask;
//        defaultOutputDirection = OutputDirection.forMask(newMask);
//    }
//
//    public void setDefaultInput(InputDirection direction) {
//        this.defaultInputDirection = direction;
//    }
//
//    public OutputDirection getDefaultOutputDirection() {
//        return defaultOutputDirection;
//    }
//
//    public InputDirection getDefaultInputDirection() {
//        return defaultInputDirection;
//    }

    public void close() {
    }

    public Sequence loadSequence(File file) {

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

    public void saveSequence(Sequence sequence, File file) {
        try {
            int[] midiFileTypes = MidiSystem.getMidiFileTypes(sequence);
            MidiSystem.write(sequence, midiFileTypes[0], file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MidiDevice getInputDevice() {
        return midiDeviceManager.getInputDevice();
    }

    public MidiDevice getOutputDevice() {
        return midiDeviceManager.getOutputDevice();
    }

    public MidiDevice getOutputDevice(int outputVariant) {
        return midiDeviceManager.getOutputDevice(outputVariant);
    }

    public MidiDevice getOutputDevice(SynthFactory synthFactory, int outputVariant) {
        return midiDeviceManager.getOutputDevice(synthFactory, outputVariant);
    }

    public void sendMidiMessage(MidiMessage message, MidiDevice outputDevice) {
        try {
            Receiver receiver = outputDevice.getReceiver();
            outputDevice.open();
            receiver.send(message, -1);
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
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
            recordingReceiver = new ReceiverWrapper(sequencer.getSequence(), sequencer.getReceiver(), listener, true, tickOffset);
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
        recordingInputMidiDevice = null;
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
            playingReceiver = new ReceiverWrapper(sequencer.getSequence(), playingOutputMidiDevice.getReceiver(), listener, false, 0);
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
        playingOutputMidiDevice = null;
    }

    public Sequence createSequenceWithOneTrack() {
        try {
            //Sequence sequence = new Sequence(Sequence.PPQ, 10);
            Sequence sequence = new Sequence(Sequence.SMPTE_24, 10);
            sequence.createTrack();
            return sequence;
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    private class ReceiverWrapper implements Receiver {
        final Receiver delegate;
        private final Receiver listener;
        private final boolean recording;
        private Sequence sequence;
        final long tickOffset;
        final long startTime = System.nanoTime();

        public ReceiverWrapper(Sequence sequence, Receiver delegate, Receiver listener, boolean recording, long tickOffset) {
            this.delegate = delegate;
            this.listener = listener;
            this.recording = recording;
            this.tickOffset = tickOffset;
            this.sequence = sequence;
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
//            int status = message.getStatus();
//            if (!recording && status != 0x90 && status != 0x80) {
//                return;
//            }
//            if (message instanceof ShortMessage) {
//                //(command & 0xF0) | (channel & 0x0F)
//                if (status == 0x90) {
//                    try {
//                        if (((ShortMessage) message).getData2() == 0) {
//                            message = new ShortMessage(0x80, ((ShortMessage) message).getData1(), 0);
//                        } else {
//                            message = new ShortMessage(0x90, ((ShortMessage) message).getData1(), ((ShortMessage) message).getData2());
//                        }
//                    } catch (InvalidMidiDataException e) {
//                        e.printStackTrace();
//                    }
//                }
//                status = message.getStatus();
//                if (status != 0xF8 && status != 0xFE) {
//                    System.out.printf("%s %d: %02X %02X %02X\n", message.getClass().getSimpleName(), System.currentTimeMillis(), status, ((ShortMessage) message).getData1(), ((ShortMessage) message).getData2());
//                }
//            }
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
