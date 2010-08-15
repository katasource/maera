package org.maera.plugin.manager;

import junit.framework.TestCase;
import org.maera.plugin.manager.StateTracker.State;

import static org.maera.plugin.manager.StateTracker.State.*;

public class TestStateTracker extends TestCase {
    public void testStandardTransitions() throws Exception {
        new StateTracker().setState(STARTING).setState(STARTED).setState(SHUTTING_DOWN).setState(SHUTDOWN).setState(STARTING);
    }

    public void testIllegalNotStartedTransitions() throws Exception {
        assertIllegalState(new StateTracker(), NOT_STARTED, STARTED, SHUTDOWN);
    }

    public void testIllegalStartingTransitions() throws Exception {
        assertIllegalState(new StateTracker().setState(STARTING), NOT_STARTED, STARTING, SHUTTING_DOWN, SHUTDOWN);
    }

    public void testIllegalStartedTransitions() throws Exception {
        assertIllegalState(new StateTracker().setState(STARTING).setState(STARTED), STARTED, NOT_STARTED, STARTING, SHUTDOWN);
    }

    public void testIllegalWarmRestartingTransitions() throws Exception {
        assertIllegalState(new StateTracker().setState(STARTING).setState(STARTED).setState(WARM_RESTARTING), NOT_STARTED, STARTING, SHUTDOWN, SHUTTING_DOWN);
    }

    public void testIllegalWarmRestartTransitions() throws Exception {
        assertIllegalState(new StateTracker().setState(STARTING).setState(STARTED).setState(WARM_RESTARTING).setState(STARTED), STARTED, NOT_STARTED, STARTING, SHUTDOWN);
    }

    public void testIllegalShuttingDownTransitions() throws Exception {
        assertIllegalState(new StateTracker().setState(STARTING).setState(STARTED).setState(SHUTTING_DOWN), NOT_STARTED, STARTING, STARTED,
                SHUTTING_DOWN);
    }

    public void testIllegalShutdownTransitions() throws Exception {
        assertIllegalState(new StateTracker().setState(STARTING).setState(STARTED).setState(SHUTTING_DOWN).setState(SHUTDOWN), NOT_STARTED, STARTED,
                SHUTTING_DOWN, SHUTDOWN);
    }

    void assertIllegalState(final StateTracker tracker, final State... states) {
        for (final State state : states) {
            try {
                final State oldState = tracker.get();
                tracker.setState(state);
                fail(oldState + " should not be allowed to transition to: " + state);
            }
            catch (final IllegalStateException expected) {
            }
        }
    }
}
