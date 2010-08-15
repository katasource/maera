package org.maera.plugin;

import org.maera.plugin.elements.ResourceDescriptor;
import org.maera.plugin.elements.ResourceLocation;

import java.util.List;

public interface Resourced {
    /**
     * Get a {@link List} of all the {@link ResourceDescriptor descriptors}
     * .
     * <p/>
     * Note: since 2.2 this list must be immutable, previously modifying this
     * list could modify the underlying list.
     *
     * @return all resource descriptors this object supports.
     */
    List<ResourceDescriptor> getResourceDescriptors();

    /**
     * Get a list of all {@link ResourceDescriptor descriptors} of a particular
     * type.
     * <p/>
     * Note: since 2.2 this list must be immutable, previously this list was
     * modifiable but modifications would not be reflected in the underlying
     * list.
     *
     * @return all resource descriptors this object supports.
     * @deprecated since 2.5.0 use {@link #getResourceDescriptors()} and filter
     *             as required
     */
    @Deprecated
    List<ResourceDescriptor> getResourceDescriptors(String type);

    /**
     * Get a {@link ResourceDescriptor} of a particular type and name.
     *
     * @return the specified resource descriptor if found, null otherwise.
     */
    ResourceDescriptor getResourceDescriptor(String type, String name);

    /**
     * Get a {@link ResourceLocation} of a particular type and name.
     *
     * @return the specified resource location if found, null otherwise.
     */
    ResourceLocation getResourceLocation(String type, String name);
}
