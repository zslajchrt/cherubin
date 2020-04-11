package org.iquality.cherubin;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.util.List;

public interface AppExtension {

    Component getMainPanel();

    List<Component> getToolBarComponents();

    List<Component> getStatusBarComponents();

    void initialize();

    void close();

    default void onSelected() {
    }

    default void onDeselected() {
    }

    String getExtensionName();

    default Action getCutAction() {
        return null;
    }

    default Action getCopyAction() {
        return null;
    }

    default Action getPasteAction() {
        return null;
    }

    default Clipboard getSystemClipboard() {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        return defaultToolkit.getSystemClipboard();
    }
}
