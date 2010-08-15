package org.maera.plugin.servlet.descriptors;

import org.apache.commons.lang.Validate;
import org.maera.plugin.StateAware;
import org.maera.plugin.module.ModuleFactory;
import org.maera.plugin.servlet.ServletModuleManager;

import javax.servlet.http.HttpServlet;

/**
 * A module descriptor that allows plugin developers to define servlets. Developers can define what urls the
 * servlet should be serve by defining one or more &lt;url-pattern&gt; elements.
 */
public class ServletModuleDescriptor extends BaseServletModuleDescriptor<HttpServlet> implements StateAware {
    private final ServletModuleManager servletModuleManager;

    /**
     * Creates a descriptor that uses a module factory to create instances
     *
     * @param moduleFactory
     * @since 2.5.0
     */
    public ServletModuleDescriptor(final ModuleFactory moduleFactory, final ServletModuleManager servletModuleManager) {
        super(moduleFactory);
        Validate.notNull(servletModuleManager);
        this.servletModuleManager = servletModuleManager;
    }

    @Override
    public void enabled() {
        super.enabled();
        servletModuleManager.addServletModule(this);
    }

    @Override
    public void disabled() {
        servletModuleManager.removeServletModule(this);
        super.disabled();
    }

    @Override
    public HttpServlet getModule() {
        return moduleFactory.createModule(moduleClassName, this);
    }

    /**
     * @deprecated Since 2.0.0, use {@link #getModule}
     */
    @Deprecated
    public HttpServlet getServlet() {
        return getModule();
    }
}
