package org.maera.plugin.classloader;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

/**
 * A class loader that delegates to another class loader.
 */
public class DelegationClassLoader extends ClassLoader {
    private static final Logger log = LoggerFactory.getLogger(DelegationClassLoader.class);

    private ClassLoader delegateClassLoader = DelegationClassLoader.class.getClassLoader();

    public void setDelegateClassLoader(ClassLoader delegateClassLoader) {
        Validate.notNull(delegateClassLoader, "Can't set the delegation target to null");
        if (log.isDebugEnabled()) {
            log.debug("Update class loader delegation from [" + this.delegateClassLoader +
                    "] to [" + delegateClassLoader + "]");
        }
        this.delegateClassLoader = delegateClassLoader;
    }

    public Class loadClass(String name) throws ClassNotFoundException {
        return delegateClassLoader.loadClass(name);
    }

    public URL getResource(String name) {
        return delegateClassLoader.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return delegateClassLoader.getResources(name);
    }

    public InputStream getResourceAsStream(String name) {
        return delegateClassLoader.getResourceAsStream(name);
    }

    public synchronized void setDefaultAssertionStatus(boolean enabled) {
        delegateClassLoader.setDefaultAssertionStatus(enabled);
    }

    public synchronized void setPackageAssertionStatus(String packageName, boolean enabled) {
        delegateClassLoader.setPackageAssertionStatus(packageName, enabled);
    }

    public synchronized void setClassAssertionStatus(String className, boolean enabled) {
        delegateClassLoader.setClassAssertionStatus(className, enabled);
    }

    public synchronized void clearAssertionStatus() {
        delegateClassLoader.clearAssertionStatus();
    }
}
