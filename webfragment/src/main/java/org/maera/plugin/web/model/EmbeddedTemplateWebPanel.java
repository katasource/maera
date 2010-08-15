package org.maera.plugin.web.model;

import org.maera.plugin.PluginAccessor;
import org.maera.plugin.web.renderer.RendererException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * This class is used for web panel declaration that do not have a custom
 * <code>class</code> attribute in their descriptor, nor a <code>location</code>
 * attribute in their resource child element.
 * <p/>
 * This class reads the web panel's content from the resource element's
 * content.
 *
 * @see org.maera.plugin.web.descriptors.DefaultWebPanelModuleDescriptor
 * @since 2.5.0
 */
public class EmbeddedTemplateWebPanel extends AbstractWebPanel {
    private String templateBody;
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedTemplateWebPanel.class.getName());

    public EmbeddedTemplateWebPanel(PluginAccessor pluginAccessor) {
        super(pluginAccessor);
    }

    /**
     * @param templateBody the body of the web panel (may contain any content type such as velocity or just static
     *                     HTML) that was inlined in <code>maera-plugin.xml</code>
     */
    public void setTemplateBody(String templateBody) {
        this.templateBody = templateBody;
    }

    public String getHtml(Map<String, Object> context) {
        try {
            return getRenderer().renderFragment(templateBody, plugin, context);
        }
        catch (RendererException e) {
            final String message = String.format("Error rendering WebPanel: %s\n" +
                    "Template contents: %s", e.getMessage(), templateBody);
            logger.warn(message, e);
            return message;
        }
    }
}
