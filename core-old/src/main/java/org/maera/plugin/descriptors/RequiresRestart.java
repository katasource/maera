package org.maera.plugin.descriptors;

import java.lang.annotation.*;

/**
 * Marks {@link org.maera.plugin.ModuleDescriptor} implementations that require a restart of the application to
 * start the plugin when installed at runtime.  If this annotation is not present, it is assumed that the module descriptor
 * supports runtime installation.
 *
 * @since 2.1
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RequiresRestart {
}
