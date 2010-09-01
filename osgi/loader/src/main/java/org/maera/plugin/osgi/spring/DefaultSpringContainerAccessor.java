package org.maera.plugin.osgi.spring;

import org.apache.commons.lang.Validate;
import org.maera.plugin.AutowireCapablePlugin;
import org.maera.plugin.PluginException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

/**
 * Manages spring context access, including autowiring.
 *
 * @since 0.1
 */
public class DefaultSpringContainerAccessor implements SpringContainerAccessor {

    private final Object nativeBeanFactory;
    private final Method nativeCreateBeanMethod;
    private final Method nativeAutowireBeanMethod;
    private final Method nativeGetBeanMethod;
    private final Method nativeGetBeansOfTypeMethod;

    /**
     * The autowire strategy to use when creating and wiring a bean
     */
    private enum AutowireStrategy {
        AUTOWIRE_NO,
        /**
         * Performs setter-based injection by name
         */
        AUTOWIRE_BY_NAME,

        /**
         * Performs setter-based injection by type
         */
        AUTOWIRE_BY_TYPE,

        /**
         * Performs construction-based injection by type
         */
        AUTOWIRE_BY_CONSTRUCTOR,

        /**
         * Autodetects appropriate injection by first seeing if any no-arg constructors exist.  If not, performs constructor
         * injection, and if so, autowires by type then name
         */
        AUTOWIRE_AUTODETECT
    }

    public DefaultSpringContainerAccessor(final Object applicationContext) {
        Object beanFactory = null;
        try {
            final Method m = applicationContext.getClass().getMethod("getAutowireCapableBeanFactory");
            beanFactory = m.invoke(applicationContext);
        }
        catch (final NoSuchMethodException e) {
            // Should never happen
            throw new PluginException("Cannot find createBean method on registered bean factory: " + beanFactory, e);
        }
        catch (final IllegalAccessException e) {
            // Should never happen
            throw new PluginException("Cannot access createBean method", e);
        }
        catch (final InvocationTargetException e) {
            handleSpringMethodInvocationError(e);
        }

        nativeBeanFactory = beanFactory;
        try {
            nativeCreateBeanMethod = beanFactory.getClass().getMethod("createBean", Class.class, int.class, boolean.class);
            nativeAutowireBeanMethod = beanFactory.getClass().getMethod("autowireBeanProperties", Object.class, int.class, boolean.class);
            nativeGetBeanMethod = beanFactory.getClass().getMethod("getBean", String.class);
            nativeGetBeansOfTypeMethod = beanFactory.getClass().getMethod("getBeansOfType", Class.class);

            Validate.noNullElements(new Object[]{nativeGetBeansOfTypeMethod, nativeAutowireBeanMethod, nativeCreateBeanMethod, nativeGetBeanMethod});
        }
        catch (final NoSuchMethodException e) {
            // Should never happen
            throw new PluginException("Cannot find one or more methods on registered bean factory: " + nativeBeanFactory, e);
        }
    }

    private void handleSpringMethodInvocationError(final InvocationTargetException e) {
        if (e.getCause() instanceof Error) {
            throw (Error) e.getCause();
        } else if (e.getCause() instanceof RuntimeException) {
            throw (RuntimeException) e.getCause();
        } else {
            // Should never happen as Spring methods only throw runtime exceptions
            throw new PluginException("Unable to invoke createBean", e.getCause());
        }
    }

    public <T> T createBean(final Class<T> clazz) {
        try {
            return clazz.cast(nativeCreateBeanMethod.invoke(nativeBeanFactory, clazz, AutowireStrategy.AUTOWIRE_AUTODETECT.ordinal(), false));
        }
        catch (final IllegalAccessException e) {
            // Should never happen
            throw new PluginException("Unable to access createBean method", e);
        }
        catch (final InvocationTargetException e) {
            handleSpringMethodInvocationError(e);
            return null;
        }
    }

    public <T> Collection<T> getBeansOfType(Class<T> interfaceClass) {
        try {
            Map<String, T> beans = (Map<String, T>) nativeGetBeansOfTypeMethod.invoke(nativeBeanFactory, interfaceClass);
            return beans.values();
        }
        catch (final IllegalAccessException e) {
            // Should never happen
            throw new PluginException("Unable to access getBeansOfType method", e);
        }
        catch (final InvocationTargetException e) {
            handleSpringMethodInvocationError(e);
            return null;
        }
    }

    public void autowireBean(final Object instance, AutowireCapablePlugin.AutowireStrategy autowireStrategy) {
        try {
            nativeAutowireBeanMethod.invoke(nativeBeanFactory, instance, autowireStrategy.ordinal(), false);
        }
        catch (final IllegalAccessException e) {
            // Should never happen
            throw new PluginException("Unable to access createBean method", e);
        }
        catch (final InvocationTargetException e) {
            handleSpringMethodInvocationError(e);
        }
    }

    public Object getBean(String id) {
        try {
            return nativeGetBeanMethod.invoke(nativeBeanFactory, id);
        }
        catch (final IllegalAccessException e) {
            // Should never happen
            throw new PluginException("Unable to access getBean method", e);
        }
        catch (final InvocationTargetException e) {
            handleSpringMethodInvocationError(e);
            return null;
        }
    }
}