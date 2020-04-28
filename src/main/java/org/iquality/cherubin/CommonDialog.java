package org.iquality.cherubin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

public abstract class CommonDialog extends JDialog {

    private static final KeyStroke escapeStroke =
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    public static final String dispatchWindowClosingActionMapKey =
            "org.iquality.cherubin:WINDOW_CLOSING";

    protected CommonDialog(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
    }

    protected void installEscapeCloseOperation() {
        Action dispatchClosing = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                CommonDialog.this.dispatchEvent(new WindowEvent(
                        CommonDialog.this, WindowEvent.WINDOW_CLOSING
                ));
            }
        };
        JRootPane root = CommonDialog.this.getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                escapeStroke, dispatchWindowClosingActionMapKey
        );
        root.getActionMap().put(dispatchWindowClosingActionMapKey, dispatchClosing
        );
    }


}