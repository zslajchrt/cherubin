package org.iquality.cherubin;

import org.iquality.cherubin.bassStation2.BS2Factory;
import org.iquality.cherubin.blofeld.BlofeldFactory;
import org.iquality.cherubin.peak.PeakFactory;

import javax.sound.midi.MidiDevice;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.awt.BorderLayout.PAGE_START;

public class AppFrame extends JFrame {
    static {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
    }

    private final AppModel appModel;
    private final List<AppExtension> appExtensions;
    private final JTabbedPane tabbedPane;

    private int currentlySelectedExt;

    public AppFrame(AppModel appModel, SoundDbModel soundDbModel, SynthModel synthModel, int width, int height) throws HeadlessException {
        super("Cherubin - Midi Librarian");
        this.appModel = appModel;

        this.getContentPane().setPreferredSize(new Dimension(width, height));

        JToolBar toolBar = new JToolBar("Global Controls");
        GlobalControls.makeToolBar(toolBar, this);
        add(toolBar, PAGE_START);

        SequencePanel sequencePanel = new SequencePanel(new SequenceModel(appModel));
        SoundDbPanel soundDbTable = new SoundDbPanel(soundDbModel);
        SynthPanel blofeldPanel = new SynthPanel(synthModel);

        appExtensions = new ArrayList<>();
        appExtensions.add(sequencePanel);
        appExtensions.add(soundDbTable);
        appExtensions.add(blofeldPanel);

        tabbedPane = new JTabbedPane();
        for (AppExtension ext : appExtensions) {
            ext.initialize();
            JPanel tabPanel = new JPanel(new BorderLayout());
            JPanel extToolBar = new JPanel();
            ext.getToolBarComponents().forEach(extToolBar::add);
            tabPanel.add(extToolBar, PAGE_START);
            tabPanel.add(ext.getMainPanel(), BorderLayout.CENTER);

            JPanel statusBar = makeStatusBar();
            ext.getStatusBarComponents().forEach(statusBar::add);
            tabPanel.add(statusBar, BorderLayout.PAGE_END);

            tabbedPane.add(tabPanel, ext.getExtensionName());
        }
        tabbedPane.addChangeListener(e -> notifySelected());

        add(tabbedPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(new RoutingPanel(appModel), PAGE_START);
        add(bottomPanel, BorderLayout.PAGE_END);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                appExtensions.forEach(AppExtension::close);
                appModel.close();
            }
        });

        initMenuBar();

        appExtensions.forEach(AppExtension::initialize);

        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        notifySelected();
    }

    public AppModel getAppModel() {
        return appModel;
    }

    private JPanel makeStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        TitledBorder titled = BorderFactory.createTitledBorder("Status");
        statusBar.setBorder(titled);
        final JLabel status = new JLabel();
        statusBar.add(status);
        return statusBar;
    }

    private void notifySelected() {
        SwingUtilities.invokeLater(() -> {
            appExtensions.get(currentlySelectedExt).onDeselected();
            currentlySelectedExt = tabbedPane.getSelectedIndex();
            appExtensions.get(currentlySelectedExt).onSelected();
        });
    }


    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu editMenu = new JMenu("Edit");
        initClipboardMenu(editMenu);

        menuBar.add(editMenu);

        setJMenuBar(menuBar);
    }

    private class AppExtensionAction extends AbstractAction implements ChangeListener, PropertyChangeListener {

        private final Function<AppExtension, Action> extActFn;
        private Action delegate;

        public AppExtensionAction(Function<AppExtension, Action> extActFn) {
            this.extActFn = extActFn;
            tabbedPane.addChangeListener(this);
            delegate = extActFn.apply(appExtensions.get(tabbedPane.getSelectedIndex()));
            if (delegate != null) {
                setEnabled(delegate.isEnabled());
                delegate.addPropertyChangeListener(this);
            } else {
                setEnabled(false);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            assert delegate != null;
            delegate.actionPerformed(e);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (delegate != null) {
                delegate.removePropertyChangeListener(this);
            }
            delegate = extActFn.apply(getCurrentAppExtension());
            if (delegate != null) {
                setEnabled(delegate.isEnabled());
                delegate.addPropertyChangeListener(this);
            } else {
                setEnabled(false);
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("enabled")) {
                setEnabled((Boolean) evt.getNewValue());
            }
        }
    }

    private void initClipboardMenu(JMenu editMenu) {
        addClipboardMenuAction(editMenu, new AppExtensionAction(AppExtension::getCutAction), KeyEvent.VK_X, "Cut");
        addClipboardMenuAction(editMenu, new AppExtensionAction(AppExtension::getCopyAction), KeyEvent.VK_C, "Copy");
        addClipboardMenuAction(editMenu, new AppExtensionAction(AppExtension::getPasteAction), KeyEvent.VK_V, "Paste");
    }

    private AppExtension getCurrentAppExtension() {
        return appExtensions.get(tabbedPane.getSelectedIndex());
    }

    private void addClipboardMenuAction(JMenu menu, Action action, int key, String text) {
        action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(key, InputEvent.CTRL_DOWN_MASK));
        action.putValue(AbstractAction.NAME, text);
        menu.add(new JMenuItem(action));
    }

    static JButton makeButton(String command, String tooltip, String text, Consumer<ActionEvent> action) {
        JButton button = new JButton();
        button.setActionCommand(command);
        button.setToolTipText(tooltip);
        button.setText(text);

        button.addActionListener(new LambdaAction(action));

        return button;
    }

    public static void main(String[] args) throws Exception {

        //Supplier<MidiDevice> leftIn = () -> MidiPortCommunicator.findDevice("Avid 003 Rack Port 1", true);
        //Supplier<MidiDevice> leftIn = () -> MidiPortCommunicator.findDevice("VirtualMIDICable1", true);
        //Supplier<MidiDevice> leftIn = () -> MidiPortCommunicator.findDevice("Bass Station", true);
        //Supplier<MidiDevice> leftOut = () -> MidiPortCommunicator.findDevice("VirtualMIDICable2", false);
        //Supplier<MidiDevice> rightIn = () -> MidiPortCommunicator.findDevice("Blofeld", true);
        //Supplier<MidiDevice> rightOut = () -> MidiPortCommunicator.findDevice("Blofeld", false);
        //Supplier<MidiDevice> rightIn = NullMidiPort::new;
        //Supplier<MidiDevice> rightOut = NullMidiPort::new;
        //Supplier<MidiDevice> rightOut = () -> MidiPortCommunicator.findDevice("Bass Station", false);

        MidiDevice systemIn = MidiDeviceManager.findDevice("Avid 003 Rack Port 1", true);
        MidiDevice systemOut = MidiDeviceManager.findDevice("VirtualMIDICable2", false);
        //MidiDevice systemOut = MidiPortCommunicator.findDevice("Peak", false);

        MidiDevice blofeldIn = MidiDeviceManager.findDevice("Blofeld", true);
        MidiDevice blofeldOut = MidiDeviceManager.findDevice("Blofeld", false);

        MidiDevice bassStationIn = MidiDeviceManager.findDevice("Bass Station", true);
        MidiDevice bassStationOut = MidiDeviceManager.findDevice("Bass Station", false);

        MidiDevice peakIn = MidiDeviceManager.findDevice("Peak", true);
        MidiDevice peakOut = MidiDeviceManager.findDevice("Peak", false);

//        Function<Integer, MidiDevice> systemMidiInputProvider = (inputVariant) -> systemIn;
//        Function<Integer, MidiDevice> systemMidiOutputProvider = (outputVariant) -> systemOut;

        MidiDeviceManager.DuplexDeviceProvider blofeldProviderOut = new MidiDeviceManager.DuplexDeviceProvider(() -> blofeldOut, () -> systemOut);

        Function<SynthFactory, Function<Integer, MidiDevice>> synthMidiInputProvider = (synthFactory) -> {
            if (synthFactory == BlofeldFactory.INSTANCE) {
                return (inputVariant) -> blofeldIn;
            } else if (synthFactory == BS2Factory.INSTANCE) {
                return (inputVariant) -> bassStationIn;
            } else if (synthFactory == PeakFactory.INSTANCE) {
                return (inputVariant) -> peakIn;
            } else {
                return (inputVariant) -> NullMidiPort.INSTANCE;
            }
        };

        Function<SynthFactory, Function<Integer, MidiDevice>> synthMidiOutputProvider = (synthFactory) -> {
            if (synthFactory == BlofeldFactory.INSTANCE) {
                return blofeldProviderOut;
            } else if (synthFactory == BS2Factory.INSTANCE) {
                return (outputVariant) -> bassStationOut;
            } else if (synthFactory == PeakFactory.INSTANCE) {
                return (outputVariant) -> peakOut;
            } else {
                return (outputVariant) -> NullMidiPort.INSTANCE;
            }
        };

        //MidiDeviceManager midiDeviceManager = new MidiDeviceManager(systemMidiInputProvider, systemMidiOutputProvider, synthMidiInputProvider, synthMidiOutputProvider);
        MidiDeviceManager midiDeviceManager = MidiDeviceManager.create(synthMidiInputProvider, synthMidiOutputProvider);
        AppModel appModel = new AppModel(midiDeviceManager);

//        MidiProxy midiProxy = new MidiProxy(appModel, new MidiProxy.MidiProxyListener() {
//            @Override
//            public MidiMessage onMessage(MidiMessage message, long timeStamp, MidiProxy.Direction direction) {
//                return message;
//            }
//        });
//
//        MidiDevice midiDevice = leftIn.get();
//        midiDevice.open();
//        midiDevice.getTransmitter().setReceiver(new Receiver() {
//            @Override
//            public void send(MidiMessage message, long timeStamp) {
//                if (message instanceof SysexMessage) {
//                    File f = new File("blofeld-init-multi.syx");
//                    try (FileOutputStream fileOutputStream = new FileOutputStream(f)) {
//                        fileOutputStream.write(message.getMessage());
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//
//            @Override
//            public void close() {
//
//            }
//        });
//
//        System.in.read();
//        midiDevice.open();

        Connection con = DriverManager.getConnection("jdbc:h2:/Users/zslajchrt/Music/Waldorf/Blofeld/Cherubin/allsounds;IFEXISTS=TRUE", "zbynek", "Ovation1");
        SoundDbModel soundDbModel = new SoundDbModel(appModel, con);
        SynthModel synthModel = new SynthModel(soundDbModel);
        AppFrame main = new AppFrame(appModel, soundDbModel, synthModel, 1200, 800);
        main.setVisible(true);
    }
}
