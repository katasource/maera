package org.maera.plugin.servlet.util;

/**
 * This utility provides a thread local stack of {@link ClassLoader}s.
 * The current "top" of the stack is the thread's current context class loader.
 * This can be used when implementing delegating plugin {@link java.util.logging.Filter}s or {@link javax.servlet.Servlet}s
 * that need to set the {@link ClassLoader} to the {@link org.maera.plugin.classloader.PluginClassLoader} the filter
 * or servlet is declared in.
 *
 * @since 2.1.0
 * @deprecated Moved to maera-plugins-core: org.maera.plugin.util.ClassLoaderStack. This impl delegates to the new impl.
 */
@Deprecated
public class ClassLoaderStack {
    /**
     * Makes the given classLoader the new ContextClassLoader for this thread, and pushes the current ContextClassLoader
     * onto a ThreadLocal stack so that we can do a {@link #pop} operation later to return to that ContextClassLoader.
     * <p/>
     * <p/>
     * Passing null is allowed and will act as a no-op. This means that you can safely {@link #pop} a ClassLoader and {@link #push} it back in
     * and it will work safely whether the stack was empty at time of {@link #pop} or not.
     *
     * @param loader The new ClassLoader to set as ContextClassLoader.
     */
    public static void push(ClassLoader loader) {
        org.maera.plugin.util.ClassLoaderStack.push(loader);
    }

    /**
     * Pops the current ContextClassLoader off the stack, setting the new ContextClassLoader to the previous one on the stack.
     * <ul>
     * <li>If the stack is not empty, then the current ClassLoader is replaced by the previous one on the stack, and then returned.</li>
     * <li>If the stack is empty, then null is returned and the current ContextClassLoader is not changed.</li>
     * </ul>
     *
     * @return the previous ContextClassLoader that was just replaced, or null if the stack is empty.
     */
    public static ClassLoader pop() {
        return org.maera.plugin.util.ClassLoaderStack.pop();
    }
}
