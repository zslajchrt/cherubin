package org.iquality.cherubin;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;

public class AppModel {

    private Sequencer sequencer;
    private MidiDevice currentInputMidiDevice;
    private MidiDevice recordingInputMidiDevice;
    private MidiDevice currentOutputMidiDevice;
    private MidiDevice playingOutputMidiDevice;

    public AppModel(MidiDevice inputMidiDevice, MidiDevice outputMidiDevice) {
        currentInputMidiDevice = inputMidiDevice;
        currentOutputMidiDevice = outputMidiDevice;
    }

    public void close() {
        sequencer.close();
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

    public MidiDevice getCurrentInputDevice() {
        return currentInputMidiDevice;
    }

    public MidiDevice getCurrentOutputDevice() {
        return currentOutputMidiDevice;
    }

    public void recordSequence(Sequence sequence, Receiver listener) {
        try {
            sequencer = MidiSystem.getSequencer(false);
            sequencer.open();
            sequencer.setSequence(sequence);
            sequencer.recordEnable(sequence.getTracks()[0], -1);
            recordingInputMidiDevice = getCurrentInputDevice();
            recordingInputMidiDevice.open();
            recordingInputMidiDevice.getTransmitter().setReceiver(new ReceiverWrapper(sequencer.getReceiver(), listener));
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

    public void playSequence(Sequence sequence, Receiver listener) {
        try {
            sequencer = MidiSystem.getSequencer(false);
            sequencer.open();
            playingOutputMidiDevice = currentOutputMidiDevice;
            playingOutputMidiDevice.open();
            sequencer.setSequence(sequence);
            sequencer.getTransmitter().setReceiver(new ReceiverWrapper(playingOutputMidiDevice.getReceiver(), listener));
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
            sequence.createTrack();
            return sequence;
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    private class ReceiverWrapper implements Receiver {
        final Receiver delegate;
        private final Receiver listener;

        public ReceiverWrapper(Receiver delegate, Receiver listener) {
            this.delegate = delegate;
            this.listener = listener;
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
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
