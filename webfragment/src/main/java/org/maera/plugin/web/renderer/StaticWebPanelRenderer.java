package org.maera.plugin.web.renderer;

import org.apache.commons.io.IOUtils;
import org.maera.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Map;

/**
 * Static {@link WebPanelRenderer}, just returns the supplied text.
 */
public class StaticWebPanelRenderer implements WebPanelRenderer {
    public static final StaticWebPanelRenderer RENDERER = new StaticWebPanelRenderer();
    public static final String RESOURCE_TYPE = "static";

    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    public void render(String templateName, Plugin plugin, Map<String, Object> context, Writer writer) throws RendererException, IOException {
        InputStream in = null;
        try {
            in = loadTemplate(plugin, templateName);
            IOUtils.copy(in, writer);
        }
        finally {
            IOUtils.closeQuietly(in);
        }
    }

    public String renderFragment(String fragment, Plugin plugin, Map<String, Object> context) throws RendererException {
        return fragment;
    }

    private InputStream loadTemplate(Plugin plugin, String templateName) throws IOException {
        InputStream in = plugin.getClassLoader().getResourceAsStream(templateName);
        if (in == null) {
            // template not found in the plugin, try the host application:
            if ((in = getClass().getResourceAsStream(templateName)) == null) {
                throw new RendererException(String.format("Static web panel template %s not found.", templateName));
            }
        }
        return in;
    }
}
