package org.iquality.cherubin;

import uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider;

import javax.sound.midi.*;
import java.awt.event.InputEvent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class MidiDeviceManager {

    public interface MidiDevicesChangeListener {
        void beforeMidiDevicesChange(List<MidiDevice> devices, boolean midiIn);

        void afterMidiDevicesChange(List<MidiDevice> devices, boolean midiIn);
    }

    public interface MessageListener {
        MidiMessage onMessage(MidiMessage message, long timeStamp) throws Exception;
    }

    private static final String CORE_MIDI_PREFIX = "CoreMIDI4J - ";
    public static final String MIDI_IN_DEVICES_PREF_KEY = "midiInDevices";
    public static final String MIDI_OUT_DEVICES_PREF_KEY = "midiOutDevices";

    private Function<Integer, MidiDevice> systemInputDeviceProvider;
    private Function<Integer, MidiDevice> systemOutputDeviceProvider;
    private final Function<SynthFactory, Function<Integer, MidiDevice>> synthInputDeviceProvider;
    private final Function<SynthFactory, Function<Integer, MidiDevice>> synthOutputDeviceProvider;

    private final List<MidiDevicesChangeListener> deviceChangeListeners = new ArrayList<>();

    public MidiDeviceManager(List<MidiDevice> initialInDevices, List<MidiDevice> initialOutDevices, Function<SynthFactory, Function<Integer, MidiDevice>> synthInputDeviceProvider, Function<SynthFactory, Function<Integer, MidiDevice>> synthOutputDeviceProvider) {
        this.systemInputDeviceProvider = createSystemDeviceProvider(initialInDevices);
        this.systemOutputDeviceProvider = createSystemDeviceProvider(initialOutDevices);
        this.synthInputDeviceProvider = synthInputDeviceProvider;
        this.synthOutputDeviceProvider = synthOutputDeviceProvider;
    }

    public static MidiDeviceManager create(Function<SynthFactory, Function<Integer, MidiDevice>> synthInputDeviceProvider, Function<SynthFactory, Function<Integer, MidiDevice>> synthOutputDeviceProvider) {
        return new MidiDeviceManager(getInitialDevices(true), getInitialDevices(false), synthInputDeviceProvider, synthOutputDeviceProvider);
    }

    private static Function<Integer, MidiDevice> createSystemDeviceProvider(List<MidiDevice> devices) {
        return (alt) -> createMultiplexDeviceSupplier(devices).get();
    }

    public static List<MidiDevice> getInitialDevices(boolean midiIn) {
        Preferences preferences = Preferences.userNodeForPackage(MidiDeviceManager.class);
        String midiDevicesKey = midiIn ? MIDI_IN_DEVICES_PREF_KEY : MIDI_OUT_DEVICES_PREF_KEY;
        String midiDevicesPref = preferences.get(midiDevicesKey, null);

        if (midiDevicesPref == null) {
            return Collections.emptyList();
        } else {
            List<String> midiDeviceNames = parseDeviceNames(midiDevicesPref);
            List<MidiDevice> midiDevices = new ArrayList<>();
            for (String midiDeviceName : midiDeviceNames) {
                MidiDevice device = findDevice(midiDeviceName, midiIn, () -> null);
                if (device != null) {
                    midiDevices.add(device);
                }
            }
            return midiDevices;
        }
    }

    public static MidiDevice findDevice(String deviceName, boolean midiIn) {
        return findDevice(deviceName, midiIn, () -> NullMidiPort.INSTANCE);
    }

    public static MidiDevice findDevice(String deviceName, boolean midiIn, Supplier<MidiDevice> defaultDeviceSupplier) {
        return getAllDevices().stream().
                filter(device -> isDevice(deviceName, device, midiIn)).
                findFirst().
                orElseGet(defaultDeviceSupplier);
    }

    private static boolean isDevice(String deviceName, MidiDevice device, boolean midiIn) {
        return isOfDirection(device, midiIn) && device.getDeviceInfo().getName().contains(deviceName);
    }

    public static boolean isInput(MidiDevice device) {
        return device.getMaxTransmitters() != 0;
    }

    public static boolean isOutput(MidiDevice device) {
        return device.getMaxReceivers() != 0;
    }

    public static List<MidiDevice> getAllDevices() {
        List<MidiDevice> midiDevices = new ArrayList<>();
        for (MidiDevice.Info deviceInfo : CoreMidiDeviceProvider.getMidiDeviceInfo()) {
            MidiDevice device = MidiDeviceWrapper.create(deviceInfo);
            midiDevices.add(device);
        }
        return midiDevices;
    }

    public static boolean isOfDirection(MidiDevice device, boolean midiIn) {
        return midiIn ? isInput(device) : isOutput(device);
    }

    public static List<MidiDevice> getAvailableDevices(boolean midiIn) {
        return getAllDevices().stream().filter(device -> isOfDirection(device, midiIn)).collect(Collectors.toList());
    }

    public static int getOutputVariant(int keyModifiers) {
        if ((keyModifiers & (InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK)) == (InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK)) {
            return 3;
        } else if ((keyModifiers & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK) {
            return 1;
        } else if ((keyModifiers & InputEvent.ALT_DOWN_MASK) == InputEvent.ALT_DOWN_MASK) {
            return 2;
        } else {
            return 0;
        }
    }

    public void setSystemDeviceProvider(List<MidiDevice> devices, boolean midiIn) {
        fireMidiDeviceChange(devices, midiIn, true);
        try {
            Supplier<MidiDevice> multiplexDeviceSupplier = MidiDeviceManager.createMultiplexDeviceSupplier(devices);
            setSystemDeviceProvider((alt) -> multiplexDeviceSupplier.get(), midiIn);

            Preferences preferences = Preferences.userNodeForPackage(MidiDeviceManager.class);
            String midiDevicesKey = midiIn ? MIDI_IN_DEVICES_PREF_KEY : MIDI_OUT_DEVICES_PREF_KEY;
            preferences.put(midiDevicesKey, getConcatDeviceNames(devices));
        } finally {
            fireMidiDeviceChange(devices, midiIn, false);
        }
    }

    public void setSystemDeviceProvider(Function<Integer, MidiDevice> newSystemDeviceProvider, boolean midiIn) {
        if (midiIn) {
            this.systemInputDeviceProvider = newSystemDeviceProvider;
        } else {
            this.systemOutputDeviceProvider = newSystemDeviceProvider;
        }
    }

    public MidiDevice getInputDevice() {
        return systemInputDeviceProvider.apply(0);
    }

    public MidiDevice getInputDevice(int inputVariant) {
        return systemInputDeviceProvider.apply(inputVariant);
    }

    public MidiDevice getInputDevice(SynthFactory synthFactory, int inputVariant) {
        return synthInputDeviceProvider.apply(synthFactory).apply(inputVariant);
    }

    public MidiDevice getOutputDevice() {
        return systemOutputDeviceProvider.apply(0);
    }

    public MidiDevice getOutputDevice(int outputVariant) {
        return systemOutputDeviceProvider.apply(outputVariant);
    }

    public MidiDevice getOutputDevice(SynthFactory synthFactory, int outputVariant) {
        return synthOutputDeviceProvider.apply(synthFactory).apply(outputVariant);
    }

    public static void broadcast(Consumer<MidiDevice> action) {
        for (MidiDevice.Info deviceInfo : CoreMidiDeviceProvider.getMidiDeviceInfo()) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(deviceInfo);
                if (device.getMaxReceivers() != 0) {
                    device.open();
                    new Thread(() -> action.accept(device)).start();
                }
            } catch (MidiUnavailableException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getDeviceName(MidiDevice midiDevice) {
        String name = midiDevice.getDeviceInfo().getName();
        if (name.startsWith(CORE_MIDI_PREFIX)) {
            return name.substring(CORE_MIDI_PREFIX.length());
        } else {
            return name;
        }
    }

    public static List<String> parseDeviceNames(String concatDeviceNames) {
        String[] split = concatDeviceNames.split(",");
        if (split.length == 1) {
            return Collections.singletonList(split[0].trim());
        } else {
            List<String> names = new ArrayList<>();
            for (String s : split) {
                String name = s.trim();
                names.add(name);
            }
            return names;
        }
    }

    public static String getConcatDeviceNames(List<MidiDevice> devices) {
        StringBuilder sb = new StringBuilder();
        for (MidiDevice selectedDevice : devices) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(MidiDeviceManager.getDeviceName(selectedDevice));
        }
        return sb.toString();
    }

    public static Supplier<MidiDevice> createMultiplexDeviceSupplier(List<MidiDevice> deviceSuppliers) {
        if (deviceSuppliers.isEmpty()) {
            return () -> NullMidiPort.INSTANCE;
        } else {
            return createMultiplexDeviceSupplier(deviceSuppliers, 0);
        }
    }

    private static Supplier<MidiDevice> createMultiplexDeviceSupplier(List<MidiDevice> deviceSuppliers, int index) {
        MidiDevice midiDevice = deviceSuppliers.get(index);
        if (index == deviceSuppliers.size() - 1) {
            return () -> midiDevice;
        } else {
            return new DuplexMidiPortSupplier(() -> midiDevice, createMultiplexDeviceSupplier(deviceSuppliers, index + 1));
        }
    }

    public void addMidiDeviceChangeListener(MidiDevicesChangeListener listener) {
        deviceChangeListeners.add(listener);
    }

    public void removeMidiDeviceChangeListener(MidiDevicesChangeListener listener) {
        deviceChangeListeners.remove(listener);
    }

    private void fireMidiDeviceChange(List<MidiDevice> devices, boolean midiIn, boolean before) {
        for (MidiDevicesChangeListener deviceChangeListener : deviceChangeListeners) {
            if (before) {
                deviceChangeListener.beforeMidiDevicesChange(devices, midiIn);
            } else {
                deviceChangeListener.afterMidiDevicesChange(devices, midiIn);
            }
        }
    }

    public static class DuplexMidiPortSupplier implements Supplier<MidiDevice> {

        private MidiDevice curDev1;
        private MidiDevice curDev2;
        private MidiDevice duplex;
        private Supplier<MidiDevice> deviceSupplier1;
        private Supplier<MidiDevice> deviceSupplier2;

        public DuplexMidiPortSupplier(Supplier<MidiDevice> deviceSupplier1, Supplier<MidiDevice> deviceSupplier2) {
            this.deviceSupplier1 = deviceSupplier1;
            this.deviceSupplier2 = deviceSupplier2;
        }

        @Override
        public MidiDevice get() {

            MidiDevice dev1 = deviceSupplier1.get();
            MidiDevice dev2 = deviceSupplier2.get();

            if (dev1 != curDev1 || dev2 != curDev2) {
                curDev1 = dev1;
                curDev2 = dev2;
                duplex = new DuplexMidiPort(curDev1, curDev2);
            }

            return duplex;
        }
    }

    public static class DuplexDeviceProvider implements Function<Integer, MidiDevice> {

        private final Supplier<MidiDevice> deviceSupplier1;
        private final Supplier<MidiDevice> deviceSupplier2;
        private final Supplier<MidiDevice> duplexSupplier;

        public DuplexDeviceProvider(Supplier<MidiDevice> deviceSupplier1, Supplier<MidiDevice> deviceSupplier2) {
            this.deviceSupplier1 = deviceSupplier1;
            this.deviceSupplier2 = deviceSupplier2;
            this.duplexSupplier = new DuplexMidiPortSupplier(deviceSupplier1, deviceSupplier2);
        }

        @Override
        public MidiDevice apply(Integer outputVariant) {
            if (outputVariant == 0) {
                return deviceSupplier1.get();
            }
            if (outputVariant == 1) {
                return deviceSupplier2.get();
            }
            if (outputVariant == 3) {
                return duplexSupplier.get();
            }
            return deviceSupplier1.get();
        }

    }

    public static class MidiDeviceWrapper implements MidiDevice {
        private final MidiDevice midiDevice;

        volatile int refCnt;

        private final List<MessageListener> inMessageListeners = new Vector<>();
        private final List<MessageListener> outMessageListeners = new Vector<>();

        final List<Receiver> receivers = Collections.synchronizedList(new Vector<>());
        final List<Transmitter> transmitters = Collections.synchronizedList(new Vector<>());

        static final ConcurrentHashMap<MidiDevice.Info, MidiDeviceWrapper> cache = new ConcurrentHashMap<>();

        private MidiDeviceWrapper(MidiDevice.Info midiDeviceInfo) {
            try {
                this.midiDevice = MidiSystem.getMidiDevice(midiDeviceInfo);
            } catch (MidiUnavailableException e) {
                throw new RuntimeException(e);
            }
        }

        public static synchronized MidiDevice create(MidiDevice.Info info) {
            return info == null ? null : cache.computeIfAbsent(info, MidiDeviceWrapper::new);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MidiDeviceWrapper that = (MidiDeviceWrapper) o;
            return Objects.equals(midiDevice, that.midiDevice);
        }

        @Override
        public int hashCode() {
            return Objects.hash(midiDevice);
        }

        @Override
        public String toString() {
            return getDeviceName(midiDevice);
        }

        @Override
        public Info getDeviceInfo() {
            return midiDevice.getDeviceInfo();
        }

        @Override
        public synchronized void open() throws MidiUnavailableException {
            System.out.println("MIDI open " + refCnt + " (" + this + ")");
            if (refCnt == 0) {
                midiDevice.open();
            }
            refCnt++;
        }

        @Override
        public synchronized void close() {
            if (refCnt == 0) {
                System.err.println("Warning: Unbalanced reference counting (" + this + ")");
            } else {
                refCnt--;
                if (refCnt == 0) {
                    System.out.println("MIDI closing the delegate " + refCnt + " (" + this + ")");
                    midiDevice.close();
                }
            }
            System.out.println("MIDI close " + refCnt + " (" + this + ")");
        }

        @Override
        public synchronized boolean isOpen() {
            return refCnt > 0 || midiDevice.isOpen();
        }

        @Override
        public long getMicrosecondPosition() {
            return midiDevice.getMicrosecondPosition();
        }

        @Override
        public int getMaxReceivers() {
            return midiDevice.getMaxReceivers();
        }

        @Override
        public int getMaxTransmitters() {
            return midiDevice.getMaxTransmitters();
        }

        @Override
        public Receiver getReceiver() throws MidiUnavailableException {
            return new MidiDeviceReceiver() {

                {
                    receivers.add(this);
                }

                final Receiver delegate = midiDevice.getReceiver();

                @Override
                public MidiDevice getMidiDevice() {
                    return MidiDeviceWrapper.this;
                }

                @Override
                public void send(MidiMessage message, long timeStamp) {
                    fireOnActivity(false, message, timeStamp);
                    delegate.send(message, timeStamp);
                }

                @Override
                public void close() {
                    delegate.close();
                    receivers.remove(this);
                }
            };
        }

        @Override
        public List<Receiver> getReceivers() {
            return receivers;
        }

        @Override
        public Transmitter getTransmitter() throws MidiUnavailableException {
            return new MidiDeviceTransmitter() {

                {
                    transmitters.add(this);
                }

                final Transmitter delegate = midiDevice.getTransmitter();

                @Override
                public MidiDevice getMidiDevice() {
                    return MidiDeviceWrapper.this;
                }

                @Override
                public void setReceiver(Receiver receiver) {
                    delegate.setReceiver(new Receiver() {
                        @Override
                        public void send(MidiMessage message, long timeStamp) {
                            fireOnActivity(true, message, timeStamp);
                            receiver.send(message, timeStamp);
                        }

                        @Override
                        public void close() {
                            receiver.close();
                        }
                    });
                }

                @Override
                public Receiver getReceiver() {
                    return delegate.getReceiver();
                }

                @Override
                public void close() {
                    delegate.close();
                    transmitters.remove(this);
                }
            };
        }

        @Override
        public List<Transmitter> getTransmitters() {
            return midiDevice.getTransmitters();
        }

        public void addActivityListener(MessageListener listener, boolean midiIn) {
            if (midiIn) {
                inMessageListeners.add(listener);
            } else {
                outMessageListeners.add(listener);
            }
        }

        void fireOnActivity(boolean midiIn, MidiMessage message, long timeStamp) {
            for (MessageListener listener : (midiIn ? inMessageListeners : outMessageListeners)) {
                try {
                    listener.onMessage(message, timeStamp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void removeActivityListener(MidiActivityPanel.MidiDeviceSlot midiDeviceSlot, boolean midiIn) {
            List<MessageListener> messageListeners = midiIn ? inMessageListeners : outMessageListeners;
            messageListeners.remove(midiDeviceSlot);
        }
    }
}
