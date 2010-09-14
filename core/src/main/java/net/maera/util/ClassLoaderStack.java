package net.maera.util;

import java.util.LinkedList;
import java.util.List;

/**
 * This utility provides a thread local stack of {@link ClassLoader}s.
 * The current "top" of the stack is the thread's current context class loader.
 * This can be used when implementing delegating plugin {@link java.util.logging.Filter}s or {@code Servlet}s
 * that need to set the {@link ClassLoader} to the {@code PluginClassLoader} the filter or servlet is declared in.
 *
 * @since 0.1
 */
public class ClassLoaderStack {

    private static final ThreadLocal<List<ClassLoader>> classLoaderStack = new ThreadLocal<List<ClassLoader>>() {
        protected List<ClassLoader> initialValue() {
            return new LinkedList<ClassLoader>();
        }
    };

    /**
     * Makes the given classLoader the new ContextClassLoader for this thread, and pushes the current ContextClassLoader
     * onto a ThreadLocal stack so that we can do a {@link #pop} operation later to return to that ContextClassLoader.
     * <p/>
     * Passing null is allowed and will act as a no-op. This means that you can safely {@link #pop} a ClassLoader and
     * {@code push} it back in and it will work safely whether the stack was empty at time of {@link #pop} or not.
     *
     * @param loader The new ClassLoader to set as ContextClassLoader.
     */
    public static void push(ClassLoader loader) {
        if (loader == null) {
            return;
        }

        classLoaderStack.get().add(0, Thread.currentThread().getContextClassLoader());
        Thread.currentThread().setContextClassLoader(loader);
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
        if (classLoaderStack.get().isEmpty()) {
            return null;
        }
        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoaderStack.get().remove(0));
        return currentClassLoader;
    }

    /**
     * Executes the provided {@link Runnable} implementation in the specified {@link ClassLoader}.
     * <p/>
     * This implementation will {@link #push} the new ClassLoader on the stack, call the specified {@code Runnable}'s
     * {@link Runnable#run run} method, and then finally {@link #pop pop} the ClassLoader off the stack,
     * guaranteeing that the stack returns to the previous state before this method was called.
     *
     * @param newClassLoader The {@link ClassLoader} to run the specified {@link Runnable} in.
     * @param runnable       The implementation to be run in the specified {@link ClassLoader}
     */
    public static void runInContext(ClassLoader newClassLoader, Runnable runnable) {
        push(newClassLoader);
        try {
            runnable.run();
        }
        finally {
            pop();
        }
    }
}