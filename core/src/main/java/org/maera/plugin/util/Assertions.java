package org.maera.plugin.util;

/**
 * Design by contract assertions.
 */
public class Assertions {
    public static <T> T notNull(final String name, final T notNull) throws IllegalArgumentException {
        if (notNull == null) {
            throw new NullArgumentException(name);
        }
        return notNull;
    }

    public static void isTrue(final String name, final boolean check) throws IllegalArgumentException {
        if (!check) {
            throw new IllegalArgumentException(name);
        }
    }

    private Assertions() {
    }

    static class NullArgumentException extends IllegalArgumentException {
        private static final long serialVersionUID = 6178592463723624585L;

        NullArgumentException(final String name) {
            super(name + " should not be null!");
        }
    }
}
