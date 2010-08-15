package org.maera.plugin.osgi.factory.transform.stage;

import org.dom4j.Element;
import org.maera.plugin.osgi.factory.transform.PluginTransformationException;
import org.maera.plugin.osgi.factory.transform.TransformContext;
import org.maera.plugin.osgi.factory.transform.TransformStage;

import java.util.List;

/**
 * Adds bundle instruction overrides from the plugin descriptor to be later used in the manifest generation process.
 *
 * @since 2.2.0
 */
public class AddBundleOverridesStage implements TransformStage {
    public void execute(TransformContext context) throws PluginTransformationException {
        Element pluginInfo = context.getDescriptorDocument().getRootElement().element("plugin-info");
        if (pluginInfo != null) {
            Element instructionRoot = pluginInfo.element("bundle-instructions");
            if (instructionRoot != null) {
                List<Element> instructionsElement = instructionRoot.elements();
                for (Element instructionElement : instructionsElement) {
                    String name = instructionElement.getName();
                    String value = instructionElement.getTextTrim();
                    context.getBndInstructions().put(name, value);
                }
            }
        }
    }
}
