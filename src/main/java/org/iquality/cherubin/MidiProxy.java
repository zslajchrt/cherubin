package org.iquality.cherubin;

import javax.sound.midi.*;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class MidiProxy {

    public interface StatusListener {
        void onStatusChanged(boolean thru);
    }

    public interface ConfigurationChangeListener {
        void beforeProxyConfigurationChange();
        void afterProxyConfigurationChange();
    }

    private final List<ProxyLinkMediator> linkMediators = new ArrayList<>();
    private final List<StatusListener> statusListeners = new ArrayList<>();
    private final List<ConfigurationChangeListener> configChangeListeners = new ArrayList<>();

    private volatile boolean thru = true;

    public MidiProxy() {
        List<ProxyLink> proxyLinks = loadLinks();
        startLinks(proxyLinks);
    }

    public List<ProxyLink> getLinks() {
        return linkMediators.stream().map(ProxyLinkMediator::getLink).collect(Collectors.toList());
    }

    public void setLinks(List<ProxyLink> links) {
        fireConfigurationChange(true);
        try {
            storeLinks(links);
            startLinks(links);
        } finally {
            fireConfigurationChange(false);
        }
    }

    private void startLinks(List<ProxyLink> links) {
        linkMediators.forEach(ProxyLinkMediator::close);
        linkMediators.clear();
        links.forEach(link -> linkMediators.add(new ProxyLinkMediator(link)));
    }

    private void storeLinks(List<ProxyLink> links) {
        try {
            Preferences preferences = Preferences.userNodeForPackage(MidiProxy.class);
            preferences.clear();
            int i = 0;
            for (ProxyLink link : links) {
                preferences.put("in" + i, MidiDeviceManager.getDeviceName(link.inDevice));
                preferences.put("out" + i, MidiDeviceManager.getDeviceName(link.outDevice));
                i++;
            }
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ProxyLink> loadLinks() {
        Preferences preferences = Preferences.userNodeForPackage(MidiProxy.class);
        int i = 0;
        MidiDevice inDevice;
        MidiDevice outDevice;
        List<ProxyLink> links = new ArrayList<>();
        do {
            inDevice = null;
            outDevice = null;
            String inDeviceName = preferences.get("in" + i, null);
            String outDeviceName = preferences.get("out" + i, null);
            if (inDeviceName != null) {
                inDevice = MidiDeviceManager.findDevice(inDeviceName, true, () -> null);
            }
            if (outDeviceName != null) {
                outDevice = MidiDeviceManager.findDevice(outDeviceName, false, () -> null);
            }
            if (inDevice != null && outDevice != null) {
                ProxyLink proxyLink = new ProxyLink(inDevice, outDevice, null);
                links.add(proxyLink);
            }
            i++;
        } while (inDevice != null && outDevice != null);

        return links;
    }

    public void setThru(boolean flag) {
        this.thru = flag;
        fireStatusChanged(thru);
    }

    public boolean isThru() {
        return thru;
    }

    public void addStatusListener(StatusListener listener) {
        statusListeners.add(listener);
    }

    public void removeStatusListener(StatusListener listener) {
        statusListeners.remove(listener);
    }

    private void fireStatusChanged(boolean thru) {
        statusListeners.forEach(listener -> listener.onStatusChanged(thru));
    }

    public void addConfigurationChangeListener(ConfigurationChangeListener listener) {
        configChangeListeners.add(listener);
    }

    public void removeConfigurationChangeListener(ConfigurationChangeListener listener) {
        configChangeListeners.remove(listener);
    }

    private void fireConfigurationChange(boolean before) {
        for (ConfigurationChangeListener configChangeListener : configChangeListeners) {
            if (before) {
                configChangeListener.beforeProxyConfigurationChange();
            } else {
                configChangeListener.afterProxyConfigurationChange();
            }
        }
    }

    public Set<MidiDevice> getLinkedDevices(boolean midiIn) {
        HashSet<MidiDevice> involved = new HashSet<>();
        for (ProxyLinkMediator linkMediator : linkMediators) {
            if (midiIn) {
                involved.add(linkMediator.in);
            } else {
                involved.add(linkMediator.out);
            }
        }
        return involved;
    }

    class CapturingReceiver implements Receiver {

        private final Receiver receiver;
        private final MidiDeviceManager.MessageListener listener;

        public CapturingReceiver(Receiver receiver, MidiDeviceManager.MessageListener listener) {
            this.receiver = receiver;
            this.listener = listener;
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (!isThru()) {
                return;
            }

            MidiMessage alteredMessage = null;
            try {
                alteredMessage = listener.onMessage(message, timeStamp);
            } catch (Exception e) {
                e.printStackTrace();
                alteredMessage = message;
            }
            receiver.send(alteredMessage == null ? message : alteredMessage, timeStamp);
        }

        @Override
        public void close() {
            receiver.close();
        }
    }

    public static class ProxyLink {

        private final MidiDevice inDevice;
        private final MidiDevice outDevice;
        public final MidiDeviceManager.MessageListener listener;

        public ProxyLink(MidiDevice inDevice, MidiDevice outDevice, MidiDeviceManager.MessageListener listener) {
            this.inDevice = inDevice;
            this.outDevice = outDevice;
            this.listener = listener;
        }

        public MidiDevice getInDevice() {
            return inDevice;
        }

        public MidiDevice getOutDevice() {
            return outDevice;
        }

        @Override
        public String toString() {
            return MidiDeviceManager.getDeviceName(inDevice) + " -> " + MidiDeviceManager.getDeviceName(outDevice);
        }
    }

    class ProxyLinkMediator {
        private final ProxyLink link;
        private final MidiDevice in;
        private final MidiDevice out;
        private final Transmitter transmitter;
        private final Receiver receiver;

        public ProxyLinkMediator(ProxyLink link) {
            this.link = link;

            try {
                this.in = open(link.inDevice);
                this.out = open(link.outDevice);

                this.transmitter = in.getTransmitter();
                this.receiver = out.getReceiver();

                if (link.listener != null) {
                    transmitter.setReceiver(new CapturingReceiver(receiver, link.listener));
                } else {
                    transmitter.setReceiver(new CapturingReceiver(receiver, (message, timeStamp) -> message));
                }
            } catch (MidiUnavailableException e) {
                throw new RuntimeException(e);
            }
        }

        public ProxyLink getLink() {
            return link;
        }

        private MidiDevice open(MidiDevice device) throws MidiUnavailableException {
            device.open();
            return device;
        }

        public void close() {
            try (Transmitter t = this.transmitter; Receiver r = receiver; MidiDevice i = in; MidiDevice o = out) {
            }
        }
    }
}
