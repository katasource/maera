package org.maera.plugin.predicate;

import org.maera.plugin.ModuleDescriptor;

/**
 * Interface used to match plugin modules according to implementation specific rules.
 *
 * @since 0.17
 * @deprecated since 2.2 unused
 */
@Deprecated
public interface ModulePredicate {
    /**
     * <p>Will match a plugin module according to implementation rules.<p>
     * <p>This method must not change the state of the module nor its plugin .</p>
     *
     * @param moduleDescriptor the {@link ModuleDescriptor} to test against.
     * @return <code>true</code> if the module matches the predicate, <code>false</code> otherwise.
     */
    boolean matches(final ModuleDescriptor moduleDescriptor);
}
