package org.maera.plugin.spring;

import org.maera.plugin.osgi.hostcomponents.ContextClassLoaderStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for Spring beans which are made available to OSGi plugin components
 * <p/>
 * If a Class is specified, then the bean is exposed only as that class -- otherwise it is exposed as all interfaces it implements.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AvailableToPlugins {
    /**
     * @return The interface the bean is exposed as
     */
    Class value() default Void.class;

    /**
     * @return The context class loader strategy to use when determine which CCL should be set when host component methods are invoked
     */
    ContextClassLoaderStrategy contextClassLoaderStrategy() default ContextClassLoaderStrategy.USE_HOST;
}
