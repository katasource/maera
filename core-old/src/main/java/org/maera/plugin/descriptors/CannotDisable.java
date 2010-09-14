package org.maera.plugin.descriptors;

import java.lang.annotation.*;


/**
 * Marks {@link org.maera.plugin.ModuleDescriptor} implementations that cannot be disabled.
 * If this annotation is not present, it is assumed that the module descriptor
 * supports disablement.
 *
 * @since 2.5.0
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CannotDisable {
}
