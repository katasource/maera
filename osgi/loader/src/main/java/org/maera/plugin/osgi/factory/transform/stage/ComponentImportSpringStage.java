package org.maera.plugin.osgi.factory.transform.stage;

import org.dom4j.Document;
import org.dom4j.Element;
import org.maera.plugin.osgi.factory.transform.PluginTransformationException;
import org.maera.plugin.osgi.factory.transform.TransformContext;
import org.maera.plugin.osgi.factory.transform.TransformStage;
import org.maera.plugin.osgi.factory.transform.model.ComponentImport;
import org.maera.plugin.util.PluginUtils;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transforms component imports into a Spring XML file
 *
 * @since 2.2.0
 */
public class ComponentImportSpringStage implements TransformStage {
    /**
     * Path of generated Spring XML file
     */
    private static final String SPRING_XML = "META-INF/spring/atlassian-plugins-component-imports.xml";

    Logger log = LoggerFactory.getLogger(ComponentImportSpringStage.class);

    public void execute(TransformContext context) throws PluginTransformationException {
        if (SpringHelper.shouldGenerateFile(context, SPRING_XML)) {
            Document springDoc = SpringHelper.createSpringDocument();
            Element root = springDoc.getRootElement();

            ServiceReference[] serviceReferences = context.getOsgiContainerManager().getRegisteredServices();
            for (ComponentImport comp : context.getComponentImports().values()) {
                if (!PluginUtils.doesModuleElementApplyToApplication(comp.getSource(), context.getApplicationKeys())) {
                    continue;
                }
                Element osgiReference = root.addElement("osgi:reference");
                osgiReference.addAttribute("id", comp.getKey());

                if (comp.getFilter() != null) {
                    osgiReference.addAttribute("filter", comp.getFilter());
                }

                Element interfaces = osgiReference.addElement("osgi:interfaces");
                for (String infName : comp.getInterfaces()) {
                    validateInterface(infName, context.getPluginFile().getName(), serviceReferences);
                    context.getExtraImports().add(infName.substring(0, infName.lastIndexOf('.')));
                    Element e = interfaces.addElement("beans:value");
                    e.setText(infName);
                }
            }
            if (root.elements().size() > 0) {
                context.setShouldRequireSpring(true);
                context.getFileOverrides().put(SPRING_XML, SpringHelper.documentToBytes(springDoc));
            }
        }
    }

    private void validateInterface(String interfaceName, String pluginName, ServiceReference[] serviceReferences) {
        if (log.isDebugEnabled()) {
            boolean found = false;
            outer:
            for (ServiceReference ref : serviceReferences) {

                for (String clazz : (String[]) ref.getProperty(Constants.OBJECTCLASS)) {
                    if (interfaceName.equals(clazz)) {
                        found = true;
                        break outer;
                    }
                }
            }
            if (!found) {
                log.debug("Couldn't confirm that '" + interfaceName + "' (used as a <component-import> in the plugin with name '" + pluginName +
                        "') is a public component in the product's OSGi exports. If this is an interface you expect to be" +
                        " provided from the product, double check the spelling of '" + interfaceName + "'; if this class" +
                        " is supposed to come from another plugin, you can probably ignore this warning.");
            }
        }

    }
}