package org.maera.plugin.osgi.factory.transform.stage;

import org.dom4j.Document;
import org.dom4j.Element;
import org.maera.plugin.osgi.factory.transform.PluginTransformationException;
import org.maera.plugin.osgi.factory.transform.TransformContext;
import org.maera.plugin.osgi.factory.transform.TransformStage;
import org.maera.plugin.util.PluginUtils;
import org.maera.plugin.util.validation.ValidationPattern;

import java.util.ArrayList;
import java.util.List;

import static org.maera.plugin.util.validation.ValidationPattern.createPattern;
import static org.maera.plugin.util.validation.ValidationPattern.test;

/**
 * Transforms component tags in the plugin descriptor into the appropriate spring XML configuration file
 *
 * @since 0.1
 */
public class ComponentSpringStage implements TransformStage {

    /**
     * Path of generated Spring XML file
     */
    private static final String SPRING_XML = "META-INF/spring/maera-plugins-components.xml";

    public void execute(TransformContext context) throws PluginTransformationException {
        if (SpringHelper.shouldGenerateFile(context, SPRING_XML)) {
            Document springDoc = SpringHelper.createSpringDocument();
            Element root = springDoc.getRootElement();
            List<Element> elements = context.getDescriptorDocument().getRootElement().elements("component");

            ValidationPattern validation = createPattern().
                    rule(
                            test("@key").withError("The key is required"),
                            test("@class").withError("The class is required"),
                            test("not(@public) or interface or @interface").withError("Interfaces must be declared for public components"),
                            test("not(service-properties) or count(service-properties/entry[@key and @value]) > 0")
                                    .withError("The service-properties element must contain at least one entry element with key and value attributes"));

            for (Element component : elements) {
                if (!PluginUtils.doesModuleElementApplyToApplication(component, context.getApplicationKeys())) {
                    continue;
                }
                validation.evaluate(component);

                Element bean = root.addElement("beans:bean");
                bean.addAttribute("id", component.attributeValue("key"));
                bean.addAttribute("alias", component.attributeValue("alias"));
                bean.addAttribute("class", component.attributeValue("class"));
                bean.addAttribute("autowire", "default");
                if ("true".equalsIgnoreCase(component.attributeValue("public"))) {
                    Element osgiService = root.addElement("osgi:service");
                    osgiService.addAttribute("id", component.attributeValue("key") + "_osgiService");
                    osgiService.addAttribute("ref", component.attributeValue("key"));

                    List<String> interfaceNames = new ArrayList<String>();
                    List<Element> compInterfaces = component.elements("interface");
                    for (Element inf : compInterfaces) {
                        interfaceNames.add(inf.getTextTrim());
                    }
                    if (component.attributeValue("interface") != null) {
                        interfaceNames.add(component.attributeValue("interface"));
                    }

                    Element interfaces = osgiService.addElement("osgi:interfaces");
                    for (String name : interfaceNames) {
                        ensureExported(name, context);
                        Element e = interfaces.addElement("beans:value");
                        e.setText(name);
                    }

                    Element svcprops = component.element("service-properties");
                    if (svcprops != null) {
                        Element targetSvcprops = osgiService.addElement("osgi:service-properties");
                        for (Element prop : new ArrayList<Element>(svcprops.elements("entry"))) {
                            Element e = targetSvcprops.addElement("beans:entry");
                            e.addAttribute("key", prop.attributeValue("key"));
                            e.addAttribute("value", prop.attributeValue("value"));
                        }
                    }
                }
            }
            if (root.elements().size() > 0) {
                context.setShouldRequireSpring(true);
                context.getFileOverrides().put(SPRING_XML, SpringHelper.documentToBytes(springDoc));
            }
        }
    }

    void ensureExported(String className, TransformContext context) {
        String pkg = className.substring(0, className.lastIndexOf('.'));
        if (!context.getExtraExports().contains(pkg)) {
            String fileName = className.replace('.', '/') + ".class";

            if (context.getPluginArtifact().doesResourceExist(fileName)) {
                context.getExtraExports().add(pkg);
            }
        }
    }

}
