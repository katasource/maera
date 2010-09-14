package org.maera.plugin;

import com.atlassian.util.concurrent.Assertions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import org.dom4j.Element;
import org.maera.plugin.elements.ResourceDescriptor;
import org.maera.plugin.elements.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Iterables.filter;
import static org.maera.plugin.util.Assertions.notNull;

/**
 * An aggregate of all resource descriptors within the given plugin module or
 * plugin.
 *
 * @see org.maera.plugin.impl.AbstractPlugin#resources
 * @see org.maera.plugin.descriptors.AbstractModuleDescriptor#resources
 */
public class Resources implements Resourced {
    public static final Resources EMPTY_RESOURCES = new Resources(Collections.<ResourceDescriptor>emptyList());

    private final List<ResourceDescriptor> resourceDescriptors;

    /**
     * Parses the resource descriptors from the provided plugin XML element and
     * creates a Resources object containing them.
     * <p/>
     * If the module or plugin contains no resource elements, an empty Resources
     * object will be returned. This method will not return null.
     *
     * @param element the plugin or plugin module XML fragment which should not
     *                be null
     * @return a Resources object representing the resources in the plugin or
     *         plugin module
     * @throws PluginParseException     if there are two resources with the same
     *                                  name and type in this element, or another parse error occurs
     * @throws IllegalArgumentException if the provided element is null
     */
    public static Resources fromXml(final Element element) throws PluginParseException, IllegalArgumentException {
        if (element == null) {
            throw new IllegalArgumentException("Cannot parse resources from null XML element");
        }

        @SuppressWarnings("unchecked")
        final List<Element> elements = element.elements("resource");

        final List<ResourceDescriptor> templates = new ArrayList<ResourceDescriptor>(elements.size());

        for (final Element e : elements) {
            final ResourceDescriptor resourceDescriptor = new ResourceDescriptor(e);

            if (templates.contains(resourceDescriptor)) {
                throw new PluginParseException("Duplicate resource with type '" + resourceDescriptor.getType() + "' and name '" + resourceDescriptor.getName() + "' found");
            }

            templates.add(resourceDescriptor);
        }
        return new Resources(templates);
    }

    /**
     * Create a resource object with the given resource descriptors. The
     * provided list must not be null.
     *
     * @param resourceDescriptors the descriptors which are part of this
     *                            resources object
     * @throws IllegalArgumentException if the resourceDescriptors list is null
     */
    public Resources(final Iterable<ResourceDescriptor> resourceDescriptors) throws IllegalArgumentException {
        Assertions.notNull("Resources cannot be created with a null resources list. Pass empty list instead", resourceDescriptors);
        this.resourceDescriptors = ImmutableList.<ResourceDescriptor>builder().addAll(resourceDescriptors).build();
    }

    public List<ResourceDescriptor> getResourceDescriptors() {
        return resourceDescriptors;
    }

    /**
     * * @deprecated since 2.5.0 use {@link #getResourceDescriptors()} and
     * filter as required
     */
    @Deprecated
    public List<ResourceDescriptor> getResourceDescriptors(final String type) {
        return ImmutableList.<ResourceDescriptor>builder().addAll(filter(resourceDescriptors, new TypeFilter(type))).build();
    }

    public ResourceLocation getResourceLocation(final String type, final String name) {
        for (final ResourceDescriptor resourceDescriptor : resourceDescriptors) {
            if (resourceDescriptor.doesTypeAndNameMatch(type, name)) {
                return resourceDescriptor.getResourceLocationForName(name);
            }
        }
        return null;
    }

    public ResourceDescriptor getResourceDescriptor(final String type, final String name) {
        for (final ResourceDescriptor resourceDescriptor : resourceDescriptors) {
            if (resourceDescriptor.getType().equalsIgnoreCase(type) && resourceDescriptor.getName().equalsIgnoreCase(name)) {
                return resourceDescriptor;
            }
        }
        return null;
    }

    public static class TypeFilter implements Predicate<ResourceDescriptor> {
        private final String type;

        public TypeFilter(final String type) {
            this.type = notNull("type", type);
        }

        public boolean apply(final ResourceDescriptor input) {
            // TODO Auto-generated method stub
            return type.equals(input.getType());
        }
    }
}
