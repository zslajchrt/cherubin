package org.iquality.cherubin;

import javax.sound.midi.MidiDevice;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AppFrame extends JFrame {

    private final AppModel appModel;

    public AppFrame(AppModel appModel, SoundDbModel soundDbModel, int width, int height) throws HeadlessException {
        super("Cherubin - Midi Librarian");
        this.appModel = appModel;

        this.getContentPane().setPreferredSize(new Dimension(width, height));

        SequenceTable sequenceTable = new SequenceTable(new SequenceTableModel(appModel));
        SoundDbTable soundDbTable = new SoundDbTable(new SoundDbTableModel(soundDbModel));
        VirtualBlofeldPanel blofeldPanel = new VirtualBlofeldPanel(new VirtualBlofeldModel(soundDbModel));

        List<AppExtension> exts = new ArrayList<>();
        exts.add(sequenceTable);
        exts.add(soundDbTable);
        exts.add(blofeldPanel);

        JTabbedPane tabbedPane = new JTabbedPane();
        for (AppExtension ext : exts) {
            ext.activate();
            JPanel tabPanel = new JPanel(new BorderLayout());
            JPanel toolBar = new JPanel();
            ext.getToolbarComponents().forEach(toolBar::add);
            tabPanel.add(toolBar, BorderLayout.PAGE_START);
            tabPanel.add(new JScrollPane((Component) ext), BorderLayout.CENTER);

            tabbedPane.add(tabPanel, ext.getExtensionName());
        }

        add(tabbedPane, BorderLayout.CENTER);

        sequenceTable.activate();
        soundDbTable.activate();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exts.forEach(AppExtension::deactivate);
                appModel.close();
            }
        });

        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    static JButton makeButton(String command, String tooltip, String text, Consumer<ActionEvent> action) {
        JButton button = new JButton();
        button.setActionCommand(command);
        button.setToolTipText(tooltip);
        button.setText(text);

        button.addActionListener(new LambdaAction(action));

        return button;
    }

    public static void main(String[] args) {
        MidiDevice proxyRightIn = MidiPortCommunicator.findDevice("VirtualMIDICable1", true);
        //MidiDevice proxyRightIn = MidiPortCommunicator.findDevice("Blofeld", true);
        MidiDevice proxyRightOut = MidiPortCommunicator.findDevice("Blofeld", false);
//        MidiDevice proxyRightIn = new NullMidiPort();
//        MidiDevice proxyRightOut = new NullMidiPort();
        AppModel appModel = new AppModel(proxyRightIn, proxyRightOut);
        SoundDbModel soundDbModel = new SoundDbModel(appModel,"jdbc:h2:/Users/zslajchrt/Music/Waldorf/Blofeld/Cherubin/allsounds;IFEXISTS=TRUE", "zbynek", "Ovation1");
        AppFrame main = new AppFrame(appModel, soundDbModel, 800, 600);
        main.setVisible(true);
    }
}
