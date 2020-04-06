package org.iquality.cherubin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SoundSet<T extends Sound> {
    public final String name;

    final List<T> sounds = new ArrayList<>();

    public SoundSet(String name) {
        this.name = name;
    }

    public List<T> getSounds() {
        return Collections.unmodifiableList(sounds);
    }

    @Override
    public String toString() {
        return name;
    }
}
