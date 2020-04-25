package org.iquality.cherubin;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import java.io.File;
import java.util.List;

public class SequenceModel {

    private final MidiDeviceManager midiDeviceManager;
    private final MidiServices midiServices;

    private Sequence sequence;

    public SequenceModel(MidiServices midiServices, MidiDeviceManager midiDeviceManager) {
        this.midiServices = midiServices;
        this.midiDeviceManager = midiDeviceManager;
        this.sequence = MidiServices.createSequenceWithOneTrack();
    }

    public Sequence getSequence() {
        return sequence;
    }

    public void loadSequence(File file) {
        sequence = MidiServices.loadSequence(file);
    }

    public void saveSequence(File file) {
        MidiServices.saveSequence(sequence, file);
    }

    public void initializeSequence() {
        sequence = MidiServices.createSequenceWithOneTrack();
    }

    public void recordSequence(Receiver listener) {
        midiServices.recordSequence(sequence, listener, midiDeviceManager.getInputDevice());
    }

    public void stopRecordingSequence() {
        midiServices.stopRecordingSequence();
    }

    public void playSequence(Receiver listener) {
        midiServices.playSequence(sequence, listener, midiDeviceManager.getOutputDevice());
    }

    public void stopPlayingSequence() {
        midiServices.stopPlayingSequence();
    }

    public void sendMidiMessage(MidiMessage message, int outputVariant) {
        midiServices.sendMidiMessage(message, midiDeviceManager.getOutputDevice(outputVariant));
    }

    public void sendMidiMessages(List<MidiMessage> midiMessages, int outputVariant) {
        midiServices.sendMidiMessages(midiMessages, midiDeviceManager.getOutputDevice(outputVariant));
    }
}
