package org.iquality.cherubin;

import java.awt.*;
import java.util.List;

public interface AppExtension {

    List<Component> getToolbarComponents();

    void activate();

    void deactivate();

    String getExtensionName();
}
