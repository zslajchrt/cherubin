package org.iquality.cherubin;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.swing.*;
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

public class AppFrame extends JFrame {
    static {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
    }

    private final AppModel appModel;
    private final List<AppExtension> appExtensions;
    private final JTabbedPane tabbedPane;

    public AppFrame(AppModel appModel, SoundDbModel soundDbModel, VirtualBlofeldModel virtualBlofeldModel, int width, int height) throws HeadlessException {
        super("Cherubin - Midi Librarian");
        this.appModel = appModel;

        this.getContentPane().setPreferredSize(new Dimension(width, height));

        SequencePanel sequencePanel = new SequencePanel(new SequenceModel(appModel));
        SoundDbTable soundDbTable = new SoundDbTable(new SoundDbTableModel(soundDbModel));
        VirtualBlofeldPanel blofeldPanel = new VirtualBlofeldPanel(virtualBlofeldModel);

        appExtensions = new ArrayList<>();
        appExtensions.add(sequencePanel);
        appExtensions.add(soundDbTable);
        appExtensions.add(blofeldPanel);

        tabbedPane = new JTabbedPane();
        for (AppExtension ext : appExtensions) {
            ext.initialize();
            JPanel tabPanel = new JPanel(new BorderLayout());
            JPanel toolBar = new JPanel();
            ext.getToolbarComponents().forEach(toolBar::add);
            tabPanel.add(toolBar, BorderLayout.PAGE_START);
            tabPanel.add(new JScrollPane((Component) ext), BorderLayout.CENTER);

            tabbedPane.add(tabPanel, ext.getExtensionName());
        }
        tabbedPane.addChangeListener(e -> notifySelected());

        add(tabbedPane, BorderLayout.CENTER);

        add(new RoutingPanel(appModel), BorderLayout.PAGE_END);

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

    private void notifySelected() {
        SwingUtilities.invokeLater(() -> appExtensions.get(tabbedPane.getSelectedIndex()).onSelected());
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
        MidiDevice leftIn = MidiPortCommunicator.findDevice("VirtualMIDICable1", true);
        MidiDevice leftOut = MidiPortCommunicator.findDevice("VirtualMIDICable2", false);
        MidiDevice rightIn = MidiPortCommunicator.findDevice("Blofeld", true);
        MidiDevice rightOut = MidiPortCommunicator.findDevice("Blofeld", false);
        AppModel appModel = new AppModel(leftIn, leftOut, rightIn, rightOut);

        MidiProxy midiProxy = new MidiProxy(appModel, new MidiProxy.MidiProxyListener() {
            @Override
            public MidiMessage onMessage(MidiMessage message, long timeStamp, MidiProxy.Direction direction) {
                return message;
            }
        });

        Connection con = DriverManager.getConnection("jdbc:h2:/Users/zslajchrt/Music/Waldorf/Blofeld/Cherubin/allsounds;IFEXISTS=TRUE", "zbynek", "Ovation1");
        SoundDbModel soundDbModel = new SoundDbModel(appModel, con);
        VirtualBlofeldModel virtualBlofeldModel = new VirtualBlofeldModel(con, appModel);
        AppFrame main = new AppFrame(appModel, soundDbModel, virtualBlofeldModel, 800, 600);
        main.setVisible(true);
    }
}
