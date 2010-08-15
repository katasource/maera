package org.maera.plugin.osgi.factory;

public class ConcurrentStateEngine {
    private final Object[] states;
    private volatile int index = 0;

    public ConcurrentStateEngine(Object... states) {
        this.states = states;
    }

    public void tryNextState(Object expected, Object next) {
        System.out.println("trying to move from " + expected + " to " + next + " but in " + states[index]);
        int old = indexOf(expected);
        int tries = 100;
        while (old != index && tries-- > 0) {
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        if (tries <= 0) {
            throw new IllegalStateException("Timeout waiting for state");
        }
        state(next);
    }

    public void state(Object next) {
        index = indexOf(next);
        System.out.println("Moved to state " + next + " (" + index + ")");
    }

    private int indexOf(Object state) {
        for (int x = 0; x < states.length; x++) {
            if (states[x].equals(state)) {
                return x;
            }
        }
        throw new IllegalStateException("Cannot find state " + state);
    }
}
