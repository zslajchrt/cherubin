package org.iquality.cherubin;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;

public class AppModel {

    public enum OutputDirection {
        none(0),
        left(1),
        right(2),
        both(3);

        final int mask;

        OutputDirection(int mask) {
            this.mask = mask;
        }

        public static OutputDirection forMask(int newMask) {
            switch (newMask) {
                case 0 : return none;
                case 1 : return left;
                case 2 : return right;
                case 3 :
                default:
                    return both;
            }
        }
    }

    public enum InputDirection {
        left,
        right
    }

    private final MidiDevice leftInputMidiDevice;
    private final MidiDevice leftOutputMidiDevice;
    private final MidiDevice rightInputMidiDevice;
    private final MidiDevice rightOutputMidiDevice;
    private final MidiDevice duplexOutputMidiDevice;
    private final MidiDevice nullOutputMidiDevice = new NullMidiPort();

    private Sequencer sequencer;

    private MidiDevice recordingInputMidiDevice;
    private MidiDevice playingOutputMidiDevice;

    private InputDirection defaultInputDirection = InputDirection.left;
    private OutputDirection defaultOutputDirection = OutputDirection.right;

    public AppModel(MidiDevice leftInputMidiDevice, MidiDevice leftOutputMidiDevice, MidiDevice rightInputMidiDevice, MidiDevice rightOutputMidiDevice) {
        try {
            this.leftInputMidiDevice = leftInputMidiDevice;
            this.leftOutputMidiDevice = leftOutputMidiDevice;
            this.rightInputMidiDevice = rightInputMidiDevice;
            this.rightOutputMidiDevice = rightOutputMidiDevice;
            this.duplexOutputMidiDevice = new DuplexMidiPort(leftOutputMidiDevice, rightOutputMidiDevice);

            this.leftInputMidiDevice.open();
            this.leftOutputMidiDevice.open();
            this.rightInputMidiDevice.open();
            this.rightOutputMidiDevice.open();
            this.duplexOutputMidiDevice.open();
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void setDefaultOutput(OutputDirection direction) {
        this.defaultOutputDirection = direction;
    }

    public void addDefaultOutput(OutputDirection direction) {
        int newMask = defaultOutputDirection.mask | direction.mask;
        defaultOutputDirection = OutputDirection.forMask(newMask);
    }

    public void removeDefaultOutput(OutputDirection direction) {
        int newMask = defaultOutputDirection.mask & ~direction.mask;
        defaultOutputDirection = OutputDirection.forMask(newMask);
    }

    public void setDefaultInput(InputDirection direction) {
        this.defaultInputDirection = direction;
    }

    public OutputDirection getDefaultOutputDirection() {
        return defaultOutputDirection;
    }

    public InputDirection getDefaultInputDirection() {
        return defaultInputDirection;
    }

    public void close() {
        try {
            this.leftInputMidiDevice.close();
        } finally {
            try {
                this.leftOutputMidiDevice.close();
            } finally {
                try {
                    this.rightInputMidiDevice.close();
                } finally {
                    try {
                        this.rightOutputMidiDevice.close();
                    } finally {
                        this.duplexOutputMidiDevice.close();
                    }
                }
            }
        }
    }

    public Sequence loadSequence(File file) {
        try {
            return MidiSystem.getSequence(file);
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

    public MidiDevice getInputDevice(InputDirection inputDirection) {
        switch (inputDirection) {
            case left: return leftInputMidiDevice;
            case right: return rightInputMidiDevice;
        }
        throw new RuntimeException("Should not be here");
    }

    public MidiDevice getOutputDevice(OutputDirection outputDirection) {
        switch (outputDirection) {
            case left: return leftOutputMidiDevice;
            case right: return rightOutputMidiDevice;
            case both: return duplexOutputMidiDevice;
            case none: return nullOutputMidiDevice;
        }
        throw new RuntimeException("Should not be here");
    }

    public void recordSequence(Sequence sequence, Receiver listener, InputDirection inputDirection) {
        try {
            sequencer = MidiSystem.getSequencer(false);
            sequencer.open();
            sequencer.setSequence(sequence);
            sequencer.setMasterSyncMode(Sequencer.SyncMode.MIDI_SYNC);
            sequencer.recordEnable(sequence.getTracks()[0], -1);
            recordingInputMidiDevice = getInputDevice(inputDirection);
            recordingInputMidiDevice.open();
            recordingInputMidiDevice.getTransmitter().setReceiver(new ReceiverWrapper(sequencer.getReceiver(), listener, true));
            //recordingInputMidiDevice.getTransmitter().setReceiver(sequencer.getReceiver());
            recordingInputMidiDevice.open();
            sequencer.startRecording();
        } catch (MidiUnavailableException | InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopRecordingSequence() {
        if (sequencer.isRecording()) {
            sequencer.stopRecording();
            sequencer.close();
            sequencer = null;
            recordingInputMidiDevice.close();
            recordingInputMidiDevice = null;
        }
    }

    public void playSequence(Sequence sequence, Receiver listener, OutputDirection outputDirection) {
        try {
            sequencer = MidiSystem.getSequencer(false);
            sequencer.open();
            playingOutputMidiDevice = getOutputDevice(outputDirection);
            playingOutputMidiDevice.open();
            sequencer.setSequence(sequence);
            sequencer.getTransmitter().setReceiver(new ReceiverWrapper(playingOutputMidiDevice.getReceiver(), listener, false));
            sequencer.start();
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopPlayingSequence() {
        if (sequencer.isRunning()) {
            sequencer.stop();
            sequencer.close();
            sequencer = null;
            playingOutputMidiDevice.close();
            playingOutputMidiDevice = null;
        }
    }

    public Sequence createSequenceWithOneTrack() {
        try {
            Sequence sequence = new Sequence(Sequence.PPQ, 10);
            //Sequence sequence = new Sequence(Sequence.SMPTE_24, 10);
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
        final long start = System.nanoTime();

        public ReceiverWrapper(Receiver delegate, Receiver listener, boolean recording) {
            this.delegate = delegate;
            this.listener = listener;
            this.recording = recording;
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
            delegate.send(message, timeStamp);
            if (recording) {
                sequencer.setMicrosecondPosition((System.nanoTime() - start) / 1000);
//            System.out.println("tick: " + sequencer.getMicrosecondPosition());
            }
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
