package org.iquality.cherubin;

import com.jhe.hexed.JHexEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class HexViewFrame extends JFrame {

    public HexViewFrame(Window parent, byte[] data) {
        super("Hex View");
        getContentPane().add(new JHexEditor(data));
        //win.addWindowListener(this);
        getRootPane().registerKeyboardAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setLocation(parent.getX() + 50 , parent.getY() + 50);
        pack();
    }
}
