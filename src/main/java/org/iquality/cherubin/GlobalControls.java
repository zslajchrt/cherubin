package org.iquality.cherubin;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class GlobalControls {

    public static void makeToolBar(JToolBar toolBar) {
        toolBar.add(new JButton(new AbstractAction("All Notes Off") {
            @Override
            public void actionPerformed(ActionEvent e) {
                SoundSender.sendAllSoundsOff();
            }
        }));
    }

}