package org.maera.plugin.web.descriptors;

/**
 * A web-section plugin adds extra sections to a particular location.
 */
public interface WebSectionModuleDescriptor extends WebFragmentModuleDescriptor {
    String getLocation();
}
