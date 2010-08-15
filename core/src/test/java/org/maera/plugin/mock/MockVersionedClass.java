package org.maera.plugin.mock;

/**
 * This is a class that just contains a version property, used to test that classes bundled into plugins will override
 * the same classes from the container (so, for example, a plugin can use a newer version of a library that ships
 * with Confluence).
 */
public class MockVersionedClass {
    public int getVersion() {
        return 1;
    }
}
