package org.iquality.cherubin;

import java.sql.Time;
import java.sql.Timestamp;

public class SoundMeta {
    private Timestamp timestamp;

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public static Timestamp getTimestamp(Sound sound, Timestamp defVal) {
        SoundMeta customData = (SoundMeta) sound.getCustomData(SoundMeta.class);
        return customData != null ? customData.getTimestamp() : defVal;
    }

}
