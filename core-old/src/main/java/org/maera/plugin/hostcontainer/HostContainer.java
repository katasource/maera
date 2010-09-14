package org.maera.plugin.hostcontainer;

/**
 * Interface into the host application's dependency injection system.  The implementation will be expected to be able
 * to instantiate modules and possibly {@link org.maera.plugin.ModuleDescriptor} instances via constructor
 * injection, matching the constructor with the largest number of arguments first.
 *
 * @since 2.2.0
 */
public interface HostContainer {
    /**
     * Constructs an instance of a class, matching the constructor with the largest number of arguments first, and
     * autowires as appropriate.  Actual method of autowiring may vary between implementations, though all should
     * support constructor injection.
     *
     * @param moduleClass The class
     * @return An instance of the passed class
     * @throws IllegalArgumentException If unable to instantiate the class
     */
    <T> T create(Class<T> moduleClass) throws IllegalArgumentException;
}
