package org.iquality.cherubin;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import java.io.File;

public class SequenceModel {

    private final AppModel appModel;

    private Sequence sequence;

    public SequenceModel(AppModel appModel) {
        this.appModel = appModel;
        this.sequence = appModel.createSequenceWithOneTrack();
    }

    public Sequence getSequence() {
        return sequence;
    }

    public void loadSequence(File file) {
        sequence = appModel.loadSequence(file);
    }

    public void saveSequence(File file) {
        appModel.saveSequence(sequence, file);
    }

    public void initializeSequence() {
        sequence = appModel.createSequenceWithOneTrack();
    }

    public void recordSequence(Receiver listener) {
        appModel.recordSequence(sequence, listener, appModel.getDefaultInputDirection());
    }

    public void stopRecordingSequence() {
        appModel.stopRecordingSequence();
    }

    public void playSequence(Receiver listener) {
        appModel.playSequence(sequence, listener, appModel.getDefaultOutputDirection());
    }

    public void stopPlayingSequence() {
        appModel.stopPlayingSequence();
    }

    public void sendMidiMessage(MidiMessage message, AppModel.OutputDirection direction) {
        appModel.sendMidiMessage(message, direction);
    }
}
