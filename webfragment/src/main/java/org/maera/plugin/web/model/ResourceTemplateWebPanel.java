package org.maera.plugin.web.model;

import com.google.common.base.Preconditions;
import org.maera.plugin.PluginAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.Map;

/**
 * This class is used for web panel declaration that do not have a custom
 * <code>class</code> attribute in their descriptor, but do have a
 * <code>location</code> attribute in their resource child element, which
 * points to a template file on the (plugin's) classpath.
 *
 * @see org.maera.plugin.web.descriptors.DefaultWebPanelModuleDescriptor
 * @since 2.5.0
 */
public class ResourceTemplateWebPanel extends AbstractWebPanel {
    private static final Logger logger = LoggerFactory.getLogger(ResourceTemplateWebPanel.class.getName());
    private String resourceFilename;

    public ResourceTemplateWebPanel(PluginAccessor pluginAccessor) {
        super(pluginAccessor);
    }

    /**
     * Specifies the name of the template file that is to be rendered.
     * This file will be loaded from the (plugin's) classpath.
     *
     * @param resourceFilename the name of the template file that is to be rendered.
     *                         May not be null.
     */
    public void setResourceFilename(String resourceFilename) {
        this.resourceFilename = Preconditions.checkNotNull(resourceFilename, "resourceFilename");
    }

    public String getHtml(Map<String, Object> context) {
        try {
            final StringWriter sink = new StringWriter();
            getRenderer().render(resourceFilename, plugin, context, sink);
            return sink.toString();
        }
        catch (Exception e) {
            final String message = String.format("Error rendering WebPanel (%s): %s", resourceFilename, e.getMessage());
            logger.warn(message, e);
            return message;
        }
    }
}
