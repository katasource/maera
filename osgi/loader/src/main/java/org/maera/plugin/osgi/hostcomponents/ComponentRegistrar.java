package org.maera.plugin.osgi.hostcomponents;

/**
 * A registrar for capturing host components and their configuration
 */
public interface ComponentRegistrar {
    /**
     * The flag to mark host components from other OSGi services
     */
    static final String HOST_COMPONENT_FLAG = "plugins-host";

    /**
     * Starts the single host component registration by declaring it as implementing one or more interfaces
     *
     * @param mainInterfaces The list of interfaces this host component implements
     * @return The instance builder that will tie these interfaces with a host component instance
     */
    InstanceBuilder register(Class<?>... mainInterfaces);
}
