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
    private final JPanel statusBar;
    private final JPanel extStatusBar;

    public AppFrame(AppModel appModel, SoundDbModel soundDbModel, SynthModel synthModel, int width, int height) throws HeadlessException {
        super("Cherubin - Midi Librarian");
        this.appModel = appModel;

        this.getContentPane().setPreferredSize(new Dimension(width, height));

        SequencePanel sequencePanel = new SequencePanel(new SequenceModel(appModel.getMidiServices(), appModel.getMidiDeviceManager()));
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
            tabbedPane.add(tabPanel, ext.getExtensionName());
        }
        tabbedPane.addChangeListener(e -> notifySelected());

        add(tabbedPane, BorderLayout.CENTER);

        statusBar = makeStatusBar();
        extStatusBar = new JPanel(new BorderLayout());
        statusBar.add(extStatusBar);
        statusBar.add(Box.createHorizontalGlue());
        statusBar.add(new GlobalStatusBar(appModel));
        add(statusBar, BorderLayout.PAGE_END);

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

    /**
     * Update the status bar by the context extension's components.
     */
    private void updateStatusBar() {
        extStatusBar.removeAll();
        AppExtension appExtension = appExtensions.get(tabbedPane.getSelectedIndex());
        appExtension.getStatusBarComponents().forEach(extStatusBar::add);
        extStatusBar.revalidate();
        extStatusBar.repaint();
    }

    public AppModel getAppModel() {
        return appModel;
    }

    private JPanel makeStatusBar() {
        JPanel statusBar = new JPanel();
        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.X_AXIS));
        TitledBorder titled = BorderFactory.createTitledBorder("Status");
        statusBar.setBorder(titled);
        return statusBar;
    }

    private void notifySelected() {
        appExtensions.get(currentlySelectedExt).onDeselected();
        currentlySelectedExt = tabbedPane.getSelectedIndex();
        appExtensions.get(currentlySelectedExt).onSelected();
        updateStatusBar();
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu editMenu = new JMenu("Edit");
        initClipboardMenu(editMenu);
        menuBar.add(editMenu);

        JMenu midiMenu = new JMenu("MIDI");
        initMidiMenu(midiMenu);
        menuBar.add(midiMenu);

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

    private void initMidiMenu(JMenu midiMenu) {
        AbstractAction action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new MidiDevicesDialog(AppFrame.this, appModel.getMidiDeviceManager());
            }
        };
        //action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(key, InputEvent.CTRL_DOWN_MASK));
        action.putValue(AbstractAction.NAME, "Devices...");
        midiMenu.add(new JMenuItem(action));

        action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MidiProxy proxy = appModel.getProxy();
                new MidiThruDialog(AppFrame.this, proxy.getLinks(), proxy::setLinks);
            }
        };
        //action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(key, InputEvent.CTRL_DOWN_MASK));
        action.putValue(AbstractAction.NAME, "Thru Config...");
        midiMenu.add(new JMenuItem(action));

        action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                appModel.getProxy().setThru(!appModel.getProxy().isThru());
                ((JCheckBoxMenuItem) e.getSource()).setState(appModel.getProxy().isThru());
            }
        };
        //action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(key, InputEvent.CTRL_DOWN_MASK));
        action.putValue(AbstractAction.NAME, "Thru");
        JCheckBoxMenuItem thruItem = new JCheckBoxMenuItem(action);
        thruItem.setState(appModel.getProxy().isThru());
        midiMenu.add(thruItem);

        action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                appModel.getExecutor().execute(MidiServices::sendAllSoundsOff);
            }
        };
        //action.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(key, InputEvent.CTRL_DOWN_MASK));
        action.putValue(AbstractAction.NAME, "All Notes Off");
        midiMenu.add(new JMenuItem(action));

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

        MidiDevice blofeldIn = MidiDeviceManager.findDevice("Blofeld", true);
        MidiDevice blofeldOut = MidiDeviceManager.findDevice("Blofeld", false);

        MidiDevice bassStationIn = MidiDeviceManager.findDevice("Bass Station", true);
        MidiDevice bassStationOut = MidiDeviceManager.findDevice("Bass Station", false);

        MidiDevice peakIn = MidiDeviceManager.findDevice("Peak", true);
        MidiDevice peakOut = MidiDeviceManager.findDevice("Peak", false);

        MidiDevice loopIn = MidiDeviceManager.findDevice("VirtualMIDICable1", true);
        MidiDevice loopOut = MidiDeviceManager.findDevice("VirtualMIDICable2", false);
        MidiDeviceManager.DuplexDeviceProvider blofeldProviderOut = new MidiDeviceManager.DuplexDeviceProvider(() -> blofeldOut, () -> loopOut);
        MidiDeviceManager.DuplexDeviceProvider blofeldProviderIn = new MidiDeviceManager.DuplexDeviceProvider(() -> blofeldIn, () -> loopIn);

        Function<SynthFactory, Function<Integer, MidiDevice>> synthMidiInputProvider = (synthFactory) -> {
            if (synthFactory == BlofeldFactory.INSTANCE) {
                return blofeldProviderIn;
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

        MidiDeviceManager midiDeviceManager = MidiDeviceManager.initialize(synthMidiInputProvider, synthMidiOutputProvider);
        MidiProxy proxy = new MidiProxy();
        AppModel appModel = new AppModel(midiDeviceManager, proxy, new MidiServices());

        Connection con = DriverManager.getConnection("jdbc:h2:/Users/zslajchrt/Music/Waldorf/Blofeld/Cherubin/allsounds;IFEXISTS=TRUE", "zbynek", "Ovation1");
        SoundDbModel soundDbModel = new SoundDbModel(appModel, con);
        SynthModel synthModel = new SynthModel(soundDbModel);
        AppFrame main = new AppFrame(appModel, soundDbModel, synthModel, 1200, 800);
        main.setVisible(true);
    }
}
