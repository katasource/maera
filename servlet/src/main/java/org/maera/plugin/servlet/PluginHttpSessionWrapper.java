package org.maera.plugin.servlet;

import org.maera.plugin.servlet.util.ClassLoaderStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;

/**
 * Wraps a HttpSession for consumption by OSGi plugins in order to workaround Weblogic problems caused by setting
 * different Context ClassLoaders.
 * See https://studio.atlassian.com/browse/PLUG-515
 *
 * @since 2.3.9
 */
public class PluginHttpSessionWrapper implements HttpSession {
    private HttpSession delegate;
    private static final Logger log = LoggerFactory.getLogger(PluginHttpSessionWrapper.class);

    public PluginHttpSessionWrapper(final HttpSession session) {
        this.delegate = session;
    }

    public Object getAttribute(final String name) {
        // Trick WLS by putting the WebAppClassLoader back into this thread's ContextClassLoader for the duration of the
        //  getAttribute() call. See PLUG-515.
        ClassLoader classLoader = ClassLoaderStack.pop();
        try {
            if (log.isDebugEnabled()) {
                log.debug("getAttribute('" + name + "') Popping ClassLoader: " + classLoader + " .New ContextClassLoader: " + Thread.currentThread().getContextClassLoader());
            }
            return delegate.getAttribute(name);
        }
        finally {
            // Reset to the Plugins ClassLoader and let OSGi continue to do its ClassLoader voodoo.
            ClassLoaderStack.push(classLoader);
        }
    }

    public void setAttribute(final String name, final Object value) {
        // Trick WLS by putting the WebAppClassLoader back into this thread's ContextClassLoader for the duration of the
        // method call. See PLUG-515.
        ClassLoader classLoader = ClassLoaderStack.pop();
        try {
            delegate.setAttribute(name, value);
        }
        finally {
            // Reset to the Plugins ClassLoader and let OSGi continue to do its ClassLoader voodoo.
            ClassLoaderStack.push(classLoader);
        }
    }

    public Object getValue(final String name) {
        // Trick WLS by putting the WebAppClassLoader back into this thread's ContextClassLoader for the duration of the
        // method call. See PLUG-515.
        ClassLoader classLoader = ClassLoaderStack.pop();
        try {
            //noinspection deprecation
            return delegate.getValue(name);
        }
        finally {
            // Reset to the Plugins ClassLoader and let OSGi continue to do its ClassLoader voodoo.
            ClassLoaderStack.push(classLoader);
        }
    }

    public void putValue(final String name, final Object value) {
        // Trick WLS by putting the WebAppClassLoader back into this thread's ContextClassLoader for the duration of the
        // method call. See PLUG-515.
        ClassLoader classLoader = ClassLoaderStack.pop();
        try {
            //noinspection deprecation
            delegate.putValue(name, value);
        }
        finally {
            // Reset to the Plugins ClassLoader and let OSGi continue to do its ClassLoader voodoo.
            ClassLoaderStack.push(classLoader);
        }
    }

    public long getCreationTime() {
        return delegate.getCreationTime();
    }

    public String getId() {
        return delegate.getId();
    }

    public long getLastAccessedTime() {
        return delegate.getLastAccessedTime();
    }

    public ServletContext getServletContext() {
        return delegate.getServletContext();
    }

    public void setMaxInactiveInterval(final int interval) {
        delegate.setMaxInactiveInterval(interval);
    }

    public int getMaxInactiveInterval() {
        return delegate.getMaxInactiveInterval();
    }

    @SuppressWarnings({"deprecation"})
    public javax.servlet.http.HttpSessionContext getSessionContext() {
        return delegate.getSessionContext();
    }

    public Enumeration getAttributeNames() {
        return delegate.getAttributeNames();
    }

    @SuppressWarnings({"deprecation"})
    public String[] getValueNames() {
        return delegate.getValueNames();
    }

    public void removeAttribute(final String name) {
        delegate.removeAttribute(name);
    }

    @SuppressWarnings({"deprecation"})
    public void removeValue(final String name) {
        delegate.removeValue(name);
    }

    public void invalidate() {
        delegate.invalidate();
    }

    public boolean isNew() {
        return delegate.isNew();
    }
}
