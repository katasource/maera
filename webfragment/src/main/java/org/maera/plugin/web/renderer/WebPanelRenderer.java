package org.maera.plugin.web.renderer;

import org.maera.plugin.Plugin;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * This interface allows the plugin system to be extended by adding new
 * renderers for new markup formats. Currently the atlassian-template-renderer
 * project provides a velocity implementation.
 *
 * @see {@link org.maera.plugin.web.descriptors.WebPanelRendererModuleDescriptor#getModule()}
 * @since 2.5.0
 */
public interface WebPanelRenderer {
    /**
     * @return the name of the resource type supported by this renderer. {@code <resource>} elements defined in plugin
     *         descriptors to be rendered by this renderer should specify this String as their {@code type} attribute.
     */
    String getResourceType();

    /**
     * Renders the template to the writer.
     *
     * @param templateName file name of the template to render
     * @param plugin       the context plugin. Used, for example, to resolve templates and other resources from the classpath
     *                     via {@link Plugin#getClassLoader()}
     * @param context      Map of objects to make available in the template rendering process
     * @param writer       where to write the rendered template
     * @throws RendererException   thrown if there is an internal exception when rendering the template
     * @throws java.io.IOException thrown if there is a problem reading the template file or writing to the writer
     */
    void render(String templateName, Plugin plugin, Map<String, Object> context, Writer writer)
            throws RendererException, IOException;

    /**
     * Renders the {@code fragment} using the given context and adding {@code I18nResolver} and {@code
     * WebResourceManager}.
     *
     * @param fragment template fragment to render
     * @param plugin   the context plugin. Used, for example, to resolve templates and other resources from the classpath
     *                 via {@link Plugin#getClassLoader()}
     * @param context  Map of objects to make available in the template rendering process
     * @return rendered template
     * @throws RendererException thrown if there is an internal exception when rendering the template
     */
    String renderFragment(String fragment, Plugin plugin, Map<String, Object> context) throws RendererException;
}
