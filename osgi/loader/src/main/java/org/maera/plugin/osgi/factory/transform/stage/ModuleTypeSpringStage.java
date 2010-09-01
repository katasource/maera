package org.maera.plugin.osgi.factory.transform.stage;

import org.dom4j.Document;
import org.dom4j.Element;
import org.maera.plugin.osgi.factory.transform.PluginTransformationException;
import org.maera.plugin.osgi.factory.transform.TransformContext;
import org.maera.plugin.osgi.factory.transform.TransformStage;
import org.maera.plugin.util.PluginUtils;
import org.maera.plugin.util.validation.ValidationPattern;

import java.util.List;

import static org.maera.plugin.util.validation.ValidationPattern.createPattern;
import static org.maera.plugin.util.validation.ValidationPattern.test;

/**
 * Transforms module-type elements into the appropriate Spring XML configuration file
 *
 * @since 0.1
 */
public class ModuleTypeSpringStage implements TransformStage {
    
    /**
     * The name of the generated Spring XML file for this stage
     */
    private static final String SPRING_XML = "META-INF/spring/maera-plugins-module-types.xml";
    static final String HOST_CONTAINER = "springHostContainer";
    static final String SPRING_HOST_CONTAINER = "org.maera.plugin.osgi.bridge.external.SpringHostContainer";

    public void execute(TransformContext context) throws PluginTransformationException {
        if (SpringHelper.shouldGenerateFile(context, SPRING_XML)) {
            Document doc = SpringHelper.createSpringDocument();
            Element root = doc.getRootElement();
            List<Element> elements = context.getDescriptorDocument().getRootElement().elements("module-type");
            if (elements.size() > 0) {
                context.getExtraImports().add("org.maera.plugin.osgi.external");
                context.getExtraImports().add("org.maera.plugin.osgi.bridge.external");
                context.getExtraImports().add("org.maera.plugin");
                Element hostContainerBean = root.addElement("beans:bean");
                hostContainerBean.addAttribute("id", HOST_CONTAINER);
                hostContainerBean.addAttribute("class", SPRING_HOST_CONTAINER);

                ValidationPattern validation = createPattern().
                        rule(
                                test("@key").withError("The key is required"),
                                test("@class").withError("The class is required"));
                for (Element e : elements) {
                    if (!PluginUtils.doesModuleElementApplyToApplication(e, context.getApplicationKeys())) {
                        continue;
                    }
                    validation.evaluate(e);
                    Element bean = root.addElement("beans:bean");
                    bean.addAttribute("id", getBeanId(e));
                    bean.addAttribute("class", "org.maera.plugin.osgi.external.SingleModuleDescriptorFactory");

                    Element arg = bean.addElement("beans:constructor-arg");
                    arg.addAttribute("index", "0");
                    arg.addAttribute("ref", HOST_CONTAINER);
                    Element arg2 = bean.addElement("beans:constructor-arg");
                    arg2.addAttribute("index", "1");
                    Element value2 = arg2.addElement("beans:value");
                    value2.setText(e.attributeValue("key"));
                    Element arg3 = bean.addElement("beans:constructor-arg");
                    arg3.addAttribute("index", "2");
                    Element value3 = arg3.addElement("beans:value");
                    value3.setText(e.attributeValue("class"));

                    Element osgiService = root.addElement("osgi:service");
                    osgiService.addAttribute("id", getBeanId(e) + "_osgiService");
                    osgiService.addAttribute("ref", getBeanId(e));
                    osgiService.addAttribute("auto-export", "interfaces");
                }
            }

            if (root.elements().size() > 0) {
                context.setShouldRequireSpring(true);
                context.getFileOverrides().put(SPRING_XML, SpringHelper.documentToBytes(doc));
            }
        }
    }

    private String getBeanId(Element e) {
        return "moduleType-" + e.attributeValue("key");
    }
}
