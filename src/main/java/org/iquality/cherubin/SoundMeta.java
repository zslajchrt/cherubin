package org.iquality.cherubin;

import java.sql.Timestamp;

public class SoundMeta {
    private Timestamp created;
    private String note;

    public Timestamp getCreated() {
        return created;
    }

    public void setCreated(Timestamp created) {
        this.created = created;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public static SoundMeta get(Sound sound) {
        return (SoundMeta) sound.getCustomData(SoundMeta.class);
    }

    public static SoundMeta getOrCreate(Sound sound) {
        SoundMeta soundMeta = get(sound);
        if (soundMeta == null) {
            soundMeta = new SoundMeta();
            sound.setCustomData(SoundMeta.class, soundMeta);
        }
        return soundMeta;
    }

    public static Timestamp getCreated(Sound sound, Timestamp defVal) {
        SoundMeta customData = (SoundMeta) sound.getCustomData(SoundMeta.class);
        return customData != null ? customData.getCreated() : defVal;
    }

    public static String getNote(Sound sound, String defVal) {
        SoundMeta customData = (SoundMeta) sound.getCustomData(SoundMeta.class);
        return customData != null ? customData.getNote() : defVal;
    }
}
