package org.iquality.cherubin;

import javax.sound.midi.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SoundCapture extends MidiPortCommunicator {

    static final int MESSAGE_ID_OFFSET = 4;
    static final int PROGRAM_BANK_OFFSET = 5;
    static final int PROGRAM_NUMBER_OFFSET = 6;
    static final int SDATA_OFFSET = 7;
    static final int SDATA_LENGTH = 380;
    static final int SINGLE_NAME_OFFSET = 363;
    static final int MULTI_NAME_OFFSET = 0;
    static final int NAME_LENGTH = 16;
    static final int SINGLE_CAT_OFFSET = 379;

    static final byte SINGLE_DUMP = 0x10;
    static final byte MULTI_DUMP = 0x11;

    private final AtomicInteger soundCounter = new AtomicInteger();
    private final Map<SoundCategory, Map<String, SingleSound>> soundBankByCategory = new HashMap<>();
    private final Map<Integer, SingleSound> singleSoundBank = Collections.synchronizedMap(new TreeMap<>());
    private final Map<Integer, MultiSound> multiSoundBank = Collections.synchronizedMap(new TreeMap<>());
    public final SoundSet<SingleSound> soundSet;

    private final List<DumpListener> dumpListeners = Collections.synchronizedList(new ArrayList<>());

    public SoundCapture(MidiDevice inputDevice, String dumpName) throws Exception {
        super(inputDevice);
        soundSet = new SoundSet<>(dumpName);
    }

    public SoundCapture(String deviceName, String dumpName) throws Exception {
        super(deviceName, true);
        soundSet = new SoundSet<>(dumpName);
    }

    class DumpReceiver implements Receiver {

        final SoundSet soundSet;

        DumpReceiver(SoundSet soundSet) {
            this.soundSet = soundSet;
        }

        public void send(MidiMessage message, long timeStamp) {

            byte[] msg = message.getMessage();
            if (!(message instanceof SysexMessage) || (msg.length < SDATA_OFFSET) || !(msg[0] == (byte) 0xF0 && msg[1] == (byte) 0x3E && msg[2] == (byte) 0x13)) {
                return;
            }

            //printSysExDump(message, timeStamp);
            System.out.print(".");

            byte msgId = msg[4];
            Sound sound = null;
            switch (msgId) {
                case SINGLE_DUMP:
                    sound = storeSingle((SysexMessage) message);
                    break;
                case MULTI_DUMP:
                    sound = storeMulti((SysexMessage) message);
                    break;
                default:
                    break;
            }

            if (sound != null) {
                notifyDumpListeners(sound);
            }

        }

        private SingleSound storeSingle(SysexMessage message) {
            byte[] msg = message.getMessage();
            String soundName = new String(msg, SDATA_OFFSET + SINGLE_NAME_OFFSET, NAME_LENGTH);
            int catId = msg[SDATA_OFFSET + SINGLE_CAT_OFFSET];
            SoundCategory category = SoundCategory.CATEGORIES[catId];

            SingleSound sound = new SingleSound(soundCounter.getAndIncrement(), soundName, category, message, soundSet.name);
            singleSoundBank.put(sound.id, sound);
            soundBankByCategory.computeIfAbsent(category, (cat) -> new HashMap<>()).put(soundName, sound);

            return sound;
        }

        private MultiSound storeMulti(SysexMessage message) {
            byte[] msg = message.getMessage();
            String soundName = new String(msg, SDATA_OFFSET + MULTI_NAME_OFFSET, NAME_LENGTH);

            MultiSound sound = new MultiSound(soundCounter.getAndIncrement(), soundName, message, null /* TODO */);
            multiSoundBank.put(sound.id, sound);

            return sound;
        }

        private void printSysExDump(MidiMessage message, long timeStamp) {
            byte[] msg = message.getMessage();

            for (int i = 0; i < msg.length; i++) {
                byte b = msg[i];
                System.out.printf("%02X ", b);
            }

            System.out.println();
        }

        public void close() {

        }
    }

    void receiveMIDI(Transmitter transmitter, SoundSet soundSet) throws MidiUnavailableException {
        Receiver rcv = new DumpReceiver(soundSet);
        transmitter.setReceiver(rcv);
    }

    @FunctionalInterface
    public interface DumpListener<T extends Sound> {
        void dumpReceived(T sound);
    }

    private void notifyDumpListeners(Sound sound) {
        for (DumpListener dumpListener : dumpListeners) {
            dumpListener.dumpReceived(sound);
        }
    }

    public void addDumpListener(DumpListener listener) {
        dumpListeners.add(listener);
    }

    public void removeDumpListener(DumpListener listener) {
        dumpListeners.remove(listener);
    }

    public void start() throws Exception {
        device.open();
        Transmitter transmitter = device.getTransmitter();
        receiveMIDI(transmitter, soundSet);
    }

    public void stop() {
        device.close();
    }
}
