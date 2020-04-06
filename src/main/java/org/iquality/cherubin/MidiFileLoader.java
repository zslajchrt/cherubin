package org.iquality.cherubin;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import java.io.File;

public class MidiFileLoader {

    private final Sequencer sequencer;

    public interface LoadListener {
        void onSound(SingleSound sound);
        void onFinished(SoundSet<SingleSound> soundSet);
        void onError(Exception e);
    }

    public MidiFileLoader() {
        try {
            sequencer = MidiSystem.getSequencer();
            if (sequencer == null) {
                throw new RuntimeException("No sequencer available");
            }
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public SoundSet<SingleSound> load(File file, String dumpName, LoadListener listener) {
        try {
            SoundCapture soundCapture = new SoundCapture(sequencer, dumpName);
            if (listener != null) {
                soundCapture.addDumpListener((SoundCapture.DumpListener<SingleSound>) listener::onSound);
            }
            sequencer.open();
            try {
                Sequence seq = MidiSystem.getSequence(file);
                sequencer.setSequence(seq);

                soundCapture.start();
                sequencer.start();

                while (sequencer.isRunning()) {
                    Thread.sleep(100);
                }

                if (listener != null) {
                    listener.onFinished(soundCapture.soundSet);
                }

                return soundCapture.soundSet;

            } finally {
                sequencer.close();
            }

        } catch (Exception e) {
            if (listener != null) {
                listener.onError(e);
            }
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        MidiFileLoader midiFileLoader = new MidiFileLoader();
        SoundSet<SingleSound> dump = midiFileLoader.load(new File(args[0]), args[1], new LoadListener() {
            @Override
            public void onSound(SingleSound sound) {
                System.out.print(".");
            }

            @Override
            public void onFinished(SoundSet<SingleSound> soundSet) {
                System.out.println("");
                System.out.println("Done");
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
        System.out.println(dump);
    }
}
