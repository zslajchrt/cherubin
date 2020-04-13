package org.iquality.cherubin;

import javax.sound.midi.MidiEvent;
import java.util.Collections;
import java.util.List;

public class MidiEvents {

    private final List<MidiEvent> events;

    public MidiEvents(List<MidiEvent> events) {
        this.events = events;
    }

    public List<MidiEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }
}
