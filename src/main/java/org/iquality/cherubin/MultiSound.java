package org.iquality.cherubin;

import java.util.List;

public interface MultiSound extends Sound {

    List<SoundSlotRef> getSlotRefs();
}
