package org.iquality.cherubin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SynthModel extends SoundEditorModel {

    private final SoundDbModel soundDbModel;
    private final SynthFactory synthFactory;
    private Synth synth;

    public SynthModel(SoundDbModel soundDbModel, SynthFactory synthFactory) {
        super(soundDbModel.getAppModel());

        this.soundDbModel = soundDbModel;
        this.synthFactory = synthFactory;

        loadSynth(synthFactory.getInitialName());
    }

    public SynthFactory getSynthFactory() {
        return synthFactory;
    }

    public void loadSynth(String name) {
        try {
            this.synth = soundDbModel.loadSynth(synthFactory, name);
            assert synth.getBanks().size() == synthFactory.getBankCount();
            fireSynthChanged();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void newSynth(String name) {
        soundDbModel.newSynth(synthFactory, name);
    }

    public void saveSynth() {
        soundDbModel.saveSynth(synth);
    }

    public boolean deleteSynth() {
        if (synthFactory.isInitial(synth)) {
            return false;
        }

        boolean deleted = soundDbModel.deleteSynth(synth);
        loadSynth(synthFactory.getInitialName());
        return deleted;
    }

    public List<Sound> getBank(int i) {
        return Collections.unmodifiableList(synth.getBanks().get(i));
    }

    public List<MultiSound> getMultiBank() {
        return Collections.unmodifiableList(synth.getMulti());
    }

    public Synth getSynth() {
        return synth;
    }

    private final List<SynthModelListener> listeners = new ArrayList<>();

    public void uploadSynth() {
        synth.getBanks().forEach(bank -> bank.stream().filter(Sound::nonEmpty).forEach(this::sendSoundWithDelayIgnoreEmpty));
    }

    public boolean exists(String name) {
        return soundDbModel.synthExists(name);
    }

    public List<String> getSynthInstances() {
        return soundDbModel.getSynthModels(synthFactory.getSynthId());
    }

    public interface SynthModelListener {
        void synthChanged(Synth synth);
    }

    private void fireSynthChanged() {
        for (SynthModelListener listener : listeners) {
            listener.synthChanged(synth);
        }
    }

    public void addSynthModelListener(SynthModelListener listener) {
        listeners.add(listener);
    }

    public void removeSynthModelListener(SynthModelListener listener) {
        listeners.remove(listener);
    }
}
