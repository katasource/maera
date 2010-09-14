package org.maera.plugin.hostcontainer;

/**
 * Simple host container that instantiates classes directly.
 *
 * @since 2.2.0
 */
public class DefaultHostContainer implements HostContainer {
    /**
     * Creates the object by instantiating the default constructor
     *
     * @param moduleClass The class to create
     * @return The instance
     * @throws IllegalArgumentException If the constructor couldn't be called successfully
     */
    public <T> T create(Class<T> moduleClass) throws IllegalArgumentException {
        try {
            return moduleClass.newInstance();
        }
        catch (InstantiationException e) {
            throw new IllegalArgumentException("Unable to instantiate constructor", e);
        }
        catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Unable to access constructor", e);
        }
    }
}
