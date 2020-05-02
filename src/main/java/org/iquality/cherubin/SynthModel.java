package org.iquality.cherubin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SynthModel extends SoundEditorModel {

    public static final SynthHeader INIT_MODEL_NAME = new SynthHeader(-1, "INIT", NullSynthFactory.INSTANCE);
    private final SoundDbModel soundDbModel;

    private Synth synth;

    public SynthModel(SoundDbModel soundDbModel) {
        super(soundDbModel.getAppModel());

        this.soundDbModel = soundDbModel;

        loadSynth(INIT_MODEL_NAME);
    }

    public SynthFactory getSynthFactory() {
        return synth.getSynthFactory();
    }

    public void loadSynth(SynthHeader synthHeader) {
        try {
            this.synth = soundDbModel.loadSynth(synthHeader);
            stopListeningToDump();
            clearEditedSound();
            assert synth.getBanks().size() == this.synth.getSynthFactory().getBankCount();
            fireSynthChanged();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Synth newSynth(String name, SynthFactory synthFactory) {
        synth = soundDbModel.newSynth(synthFactory, name);
        stopListeningToDump();
        clearEditedSound();
        fireSynthChanged();
        return synth;
    }

    public void saveSynth() {
        soundDbModel.saveSynth(synth);
    }

    public boolean deleteSynth() {
        if (isInitialSynth()) {
            return false;
        }

        boolean deleted = soundDbModel.deleteSynth(synth);
        loadSynth(INIT_MODEL_NAME);
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

    public void uploadSynth(int outputVariant) {
        synth.getBanks().forEach(bank -> bank.stream().filter(Sound::isRegular).forEach(s -> {
            try {
                s.verify();
                if (sendSoundWithDelayIgnoreEmpty(s, outputVariant)) {
                    System.out.println("Sound " + s + " uploaded");
                }
            } catch (Sound.VerificationException e) {
                e.printStackTrace();
            }
        }));

        synth.getMulti().stream().filter(Sound::isRegular).forEach(s -> {
            try {
                s.verify();
                if (sendSoundWithDelayIgnoreEmpty(s, outputVariant)) {
                    System.out.println("Multi Sound " + s + " uploaded");
                }
            } catch (Sound.VerificationException e) {
                e.printStackTrace();
            }
        });

    }

    public boolean exists(String name) {
        return soundDbModel.synthExists(name);
    }

    public List<SynthHeader> getSynthInstances() {
        return soundDbModel.getSynthModels();
    }

    public void updateSound(Sound sound) {
        if (sound instanceof SingleSound) {
            updateSingleSound((SingleSound) sound);
        } else {
            assert sound instanceof MultiSound;
            updateMultiSound((MultiSound) sound);
        }
    }

    public void updateSingleSound(SingleSound sound) {
        soundDbModel.updateBankSound(sound);
    }

    public void updateMultiSound(MultiSound sound) {
        soundDbModel.updateBankSound(sound);
    }

    public boolean isInitialSynth() {
        return INIT_MODEL_NAME.getName().equals(synth.getName());
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
