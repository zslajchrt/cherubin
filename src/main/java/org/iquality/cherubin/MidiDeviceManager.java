package org.iquality.cherubin;

import javax.sound.midi.MidiDevice;
import java.util.function.Function;
import java.util.function.Supplier;

public class MidiDeviceManager {

    private final Function<Integer, MidiDevice> systemInputDeviceProvider;
    private final Function<Integer, MidiDevice> systemOutputDeviceProvider;
    private final Function<SynthFactory, Function<Integer, MidiDevice>> synthInputDeviceProvider;
    private final Function<SynthFactory, Function<Integer, MidiDevice>> synthOutputDeviceProvider;

    public MidiDeviceManager(Function<Integer, MidiDevice> systemInputDeviceProvider, Function<Integer, MidiDevice> systemOutputDeviceProvider, Function<SynthFactory, Function<Integer, MidiDevice>> synthInputDeviceProvider, Function<SynthFactory, Function<Integer, MidiDevice>> synthOutputDeviceProvider) {
        this.systemInputDeviceProvider = systemInputDeviceProvider;
        this.systemOutputDeviceProvider = systemOutputDeviceProvider;
        this.synthInputDeviceProvider = synthInputDeviceProvider;
        this.synthOutputDeviceProvider = synthOutputDeviceProvider;
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

    public static class DuplexDeviceProvider implements Function<Integer, MidiDevice> {

        private final Supplier<MidiDevice> deviceSupplier1;
        private final Supplier<MidiDevice> deviceSupplier2;
        private final Supplier<MidiDevice> duplexSupplier;

        public DuplexDeviceProvider(Supplier<MidiDevice> deviceSupplier1, Supplier<MidiDevice> deviceSupplier2) {
            this.deviceSupplier1 = deviceSupplier1;
            this.deviceSupplier2 = deviceSupplier2;
            this.duplexSupplier = new DuplexMidiPortSupplier();
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

        class DuplexMidiPortSupplier implements Supplier<MidiDevice> {

            private MidiDevice curDev1;
            private MidiDevice curDev2;
            private MidiDevice duplex;

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
    }
}
