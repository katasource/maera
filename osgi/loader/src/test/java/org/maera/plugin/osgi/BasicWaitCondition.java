package org.maera.plugin.osgi;

import org.maera.plugin.util.WaitUntil;

public abstract class BasicWaitCondition implements WaitUntil.WaitCondition {
    public abstract boolean isFinished();

    public String getWaitMessage() {
        return "";
    }
}
