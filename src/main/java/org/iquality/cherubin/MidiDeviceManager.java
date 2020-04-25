package org.iquality.cherubin;

import uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class MidiDeviceManager {

    private static final int SOUND_DUMP_DELAY = 100;
    private static final String CORE_MIDI_PREFIX = "CoreMIDI4J - ";
    public static final String MIDI_IN_DEVICES_PREF_KEY = "midiInDevices";
    public static final String MIDI_OUT_DEVICES_PREF_KEY = "midiOutDevices";

    private Function<Integer, MidiDevice> systemInputDeviceProvider;
    private Function<Integer, MidiDevice> systemOutputDeviceProvider;
    private final Function<SynthFactory, Function<Integer, MidiDevice>> synthInputDeviceProvider;
    private final Function<SynthFactory, Function<Integer, MidiDevice>> synthOutputDeviceProvider;

    public MidiDeviceManager(Function<Integer, MidiDevice> systemInputDeviceProvider, Function<Integer, MidiDevice> systemOutputDeviceProvider, Function<SynthFactory, Function<Integer, MidiDevice>> synthInputDeviceProvider, Function<SynthFactory, Function<Integer, MidiDevice>> synthOutputDeviceProvider) {
        this.systemInputDeviceProvider = systemInputDeviceProvider;
        this.systemOutputDeviceProvider = systemOutputDeviceProvider;
        this.synthInputDeviceProvider = synthInputDeviceProvider;
        this.synthOutputDeviceProvider = synthOutputDeviceProvider;
    }

    public static MidiDeviceManager create(Function<SynthFactory, Function<Integer, MidiDevice>> synthInputDeviceProvider, Function<SynthFactory, Function<Integer, MidiDevice>> synthOutputDeviceProvider) {
        return new MidiDeviceManager(createSystemDeviceProvider(true), createSystemDeviceProvider(false), synthInputDeviceProvider, synthOutputDeviceProvider);
    }

    private static Function<Integer, MidiDevice> createSystemDeviceProvider(boolean midiIn) {
        List<MidiDevice> initialDevices = getInitialDevices(midiIn);
        return (alt) -> createMultiplexDeviceSupplier(initialDevices).get();
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
        try {
            List<MidiDevice> midiDevices = new ArrayList<>();
            for (MidiDevice.Info deviceInfo : CoreMidiDeviceProvider.getMidiDeviceInfo()) {
                MidiDevice device = MidiSystem.getMidiDevice(deviceInfo);
                midiDevices.add(device);
            }
            return midiDevices;
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<MidiDevice> getAvailableInputDevices() {
        return getAvailableDevices(true);
    }

    public static List<MidiDevice> getAvailableOutputDevices() {
        return getAvailableDevices(false);
    }

    public static boolean isOfDirection(MidiDevice device, boolean midiIn) {
        return midiIn ? isInput(device) : isOutput(device);
    }

    public static List<MidiDevice> getAvailableDevices(boolean midiIn) {
        return getAllDevices().stream().filter(device -> isOfDirection(device, midiIn)).collect(Collectors.toList());
    }

    public MidiDevice getInputDevice() {
        return systemInputDeviceProvider.apply(0);
    }

    public MidiDevice getInputDevice(int inputVariant) {
        return systemInputDeviceProvider.apply(inputVariant);
    }

    public void setSystemDeviceProvider(List<MidiDevice> devices, boolean midiIn) {
        Supplier<MidiDevice> multiplexDeviceSupplier = MidiDeviceManager.createMultiplexDeviceSupplier(devices);
        setSystemDeviceProvider((alt) -> multiplexDeviceSupplier.get(), midiIn);

        Preferences preferences = Preferences.userNodeForPackage(MidiDeviceManager.class);
        String midiDevicesKey = midiIn ? MIDI_IN_DEVICES_PREF_KEY : MIDI_OUT_DEVICES_PREF_KEY;
        preferences.put(midiDevicesKey, getConcatDeviceNames(devices));
    }

    public void setSystemDeviceProvider(Function<Integer, MidiDevice> newSystemDeviceProvider, boolean midiIn) {
        if (midiIn) {
            this.systemInputDeviceProvider = newSystemDeviceProvider;
        } else {
            this.systemOutputDeviceProvider = newSystemDeviceProvider;
        }
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
                    device.isOpen();
                    new Thread(() -> action.accept(device)).start();
                }
            } catch (MidiUnavailableException e) {
                e.printStackTrace();
            }
        }
    }

    public static void delay(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException ignored) {
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

    public static void delay() {
        delay(SOUND_DUMP_DELAY);
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

    public static class MidiDeviceWrapper {
        private final MidiDevice midiDevice;

        public MidiDeviceWrapper(MidiDevice midiDevice) {
            this.midiDevice = midiDevice;
        }

        public MidiDevice getMidiDevice() {
            return midiDevice;
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
    }

}
