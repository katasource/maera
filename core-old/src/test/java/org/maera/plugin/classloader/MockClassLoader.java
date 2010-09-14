package org.maera.plugin.classloader;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MockClassLoader extends AbstractClassLoader {
    private Map<String, Class> registeredClasses = new HashMap<String, Class>();

    protected URL findResource(final String name) {
        throw new UnsupportedOperationException();
    }

    protected Class findClass(final String className) throws ClassNotFoundException {
        Class clazz = registeredClasses.get(className);
        if (clazz == null) {
            throw new ClassNotFoundException("Class '" + className + "' not found.");
        }
        return clazz;
    }

    public void register(final String className, final Class clazz) {
        registeredClasses.put(className, clazz);
    }
}
