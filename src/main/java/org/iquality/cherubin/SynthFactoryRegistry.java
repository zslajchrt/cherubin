package org.iquality.cherubin;

import javax.sound.midi.SysexMessage;
import java.util.HashMap;
import java.util.Map;

public class SynthFactoryRegistry {

    public static final SynthFactoryRegistry INSTANCE = new SynthFactoryRegistry();

    private final Map<String, SynthFactory> factoryMap = new HashMap<>();

    public SynthFactoryRegistry() {
        factoryMap.put("Blofeld", BlofeldFactory.INSTANCE);
    }

    public SynthFactory getSynthFactory(String synthId) {
        return factoryMap.computeIfAbsent(synthId, (sid) -> {
            throw new RuntimeException("No synth factory for " + synthId);
        });
    }

    public SynthFactory getSynthFactory(SysexMessage message) {
        for (SynthFactory synthFactory : factoryMap.values()) {
            if (synthFactory.accepts(message)) {
                return synthFactory;
            }
        }
        return null;
    }

    public Iterable<SynthFactory> getSynthFactories() {
        return factoryMap.values();
    }
}
