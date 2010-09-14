package org.maera.plugin.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

///CLOVER:OFF

/**
 * This class is extremely useful for loading resources and classes in a fault tolerant manner
 * that works across different applications servers.
 * <p/>
 * It has come out of many months of frustrating use of multiple application servers at Atlassian,
 * please don't change things unless you're sure they're not going to break in one server or another!
 *
 * @author $Author: mcannon $
 * @version $Revision: 1.1 $
 */
public class ClassLoaderUtils {
    /**
     * Load a class with a given name.
     * <p/>
     * It will try to load the class in the following order:
     * <ul>
     * <li>From {@link Thread#getContextClassLoader() Thread.currentThread().getContextClassLoader()}
     * <li>Using the basic {@link Class#forName(java.lang.String) }
     * <li>From {@link Class#getClassLoader() ClassLoaderUtil.class.getClassLoader()}
     * <li>From the {@link Class#getClassLoader() callingClass.getClassLoader() }
     * </ul>
     *
     * @param className    The name of the class to load
     * @param callingClass The Class object of the calling object
     * @throws ClassNotFoundException If the class cannot be found anywhere.
     */
    public static <T> Class<T> loadClass(final String className, final Class<?> callingClass) throws ClassNotFoundException {
        try {
            return coerce(Thread.currentThread().getContextClassLoader().loadClass(className));
        }
        catch (final ClassNotFoundException e) {
            try {
                return coerce(Class.forName(className));
            }
            catch (final ClassNotFoundException ex) {
                try {
                    return coerce(ClassLoaderUtils.class.getClassLoader().loadClass(className));
                }
                catch (final ClassNotFoundException exc) {
                    if ((callingClass != null) && (callingClass.getClassLoader() != null)) {
                        return coerce(callingClass.getClassLoader().loadClass(className));
                    } else {
                        throw exc;
                    }
                }
            }
        }
    }

    private static <T> Class<T> coerce(final Class<?> klass) {
        @SuppressWarnings("unchecked")
        final Class<T> result = (Class<T>) klass;
        return result;
    }

    /**
     * Load a given resource.
     * <p/>
     * This method will try to load the resource using the following methods (in order):
     * <ul>
     * <li>From {@link Thread#getContextClassLoader() Thread.currentThread().getContextClassLoader()}
     * <li>From {@link Class#getClassLoader() ClassLoaderUtil.class.getClassLoader()}
     * <li>From the {@link Class#getClassLoader() callingClass.getClassLoader() }
     * </ul>
     *
     * @param resourceName The name of the resource to load
     * @param callingClass The Class object of the calling object
     */
    public static URL getResource(final String resourceName, final Class<?> callingClass) {
        URL url = null;

        url = Thread.currentThread().getContextClassLoader().getResource(resourceName);

        if (url == null) {
            url = ClassLoaderUtils.class.getClassLoader().getResource(resourceName);
        }

        if (url == null) {
            url = callingClass.getClassLoader().getResource(resourceName);
        }
        return url;
    }

    /**
     * returns all found resources as java.net.URLs.
     * <p/>
     * This method will try to load the resource using the following methods (in order):
     * <ul>
     * <li>From {@link Thread#getContextClassLoader() Thread.currentThread().getContextClassLoader()}
     * <li>From {@link Class#getClassLoader() ClassLoaderUtil.class.getClassLoader()}
     * <li>From the {@link Class#getClassLoader() callingClass.getClassLoader() }
     * </ul>
     *
     * @param resourceName The name of the resource to load
     * @param callingClass The Class object of the calling object
     */
    public static Enumeration<URL> getResources(final String resourceName, final Class<?> callingClass) throws IOException {
        Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(resourceName);
        if (urls == null) {
            urls = ClassLoaderUtils.class.getClassLoader().getResources(resourceName);
            if (urls == null) {
                urls = callingClass.getClassLoader().getResources(resourceName);
            }
        }

        return urls;
    }

    /**
     * This is a convenience method to load a resource as a stream.
     * <p/>
     * The algorithm used to find the resource is given in getResource()
     *
     * @param resourceName The name of the resource to load
     * @param callingClass The Class object of the calling object
     */
    public static InputStream getResourceAsStream(final String resourceName, final Class<?> callingClass) {
        final URL url = getResource(resourceName, callingClass);
        try {
            return url != null ? url.openStream() : null;
        }
        catch (final IOException e) {
            return null;
        }
    }

    /**
     * Prints the current classloader hierarchy - useful for debugging.
     */
    public static void printClassLoader() {
        System.out.println("ClassLoaderUtils.printClassLoader");
        printClassLoader(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Prints the classloader hierarchy from a given classloader - useful for debugging.
     */
    public static void printClassLoader(final ClassLoader cl) {
        System.out.println("ClassLoaderUtils.printClassLoader(cl = " + cl + ")");
        if (cl != null) {
            printClassLoader(cl.getParent());
        }
    }
}
