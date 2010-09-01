package org.maera.plugin.osgi.factory.transform.stage;

import org.dom4j.Attribute;
import org.dom4j.DocumentHelper;
import org.dom4j.XPath;
import org.maera.plugin.osgi.factory.transform.PluginTransformationException;
import org.maera.plugin.osgi.factory.transform.TransformContext;
import org.maera.plugin.osgi.factory.transform.TransformStage;
import org.maera.plugin.osgi.hostcomponents.HostComponentRegistration;
import org.maera.plugin.util.ClassLoaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scans the plugin descriptor for any "class" attribute, and ensures that it will be imported, if appropriate.
 *
 * @since 0.1
 */
public class ScanDescriptorForHostClassesStage implements TransformStage {
    
    private static final Logger log = LoggerFactory.getLogger(ScanDescriptorForHostClassesStage.class);

    @SuppressWarnings("unchecked")
    public void execute(TransformContext context) throws PluginTransformationException {
        XPath xpath = DocumentHelper.createXPath("//@class");
        List<Attribute> attributes = xpath.selectNodes(context.getDescriptorDocument());
        for (Attribute attr : attributes) {
            String className = attr.getValue();

            scanForHostComponents(context, className);

            int dotpos = className.lastIndexOf(".");
            if (dotpos > -1) {
                String pkg = className.substring(0, dotpos);
                String pkgPath = pkg.replace('.', '/') + '/';

                // Only add an import if the system exports it and the plugin isn't using the package
                if (context.getSystemExports().isExported(pkg)) {
                    if (context.getPluginArtifact().doesResourceExist(pkgPath)) {
                        log.warn("The plugin '" + context.getPluginArtifact().toString() + "' uses a package '" +
                                pkg + "' that is also exported by the application.  It is highly recommended that the " +
                                "plugin use its own packages.");
                    } else {
                        context.getExtraImports().add(pkg);
                    }
                }
            }
        }
    }

    private void scanForHostComponents(TransformContext context, String className) {
        // Class name can be prefixed with 'bean:' to reference a spring bean, in this case don't attempt to load it. 
        if (className != null && className.indexOf(":") != -1) {
            return;
        }

        Map<Class<?>, HostComponentRegistration> hostComponentInterfaces = new HashMap<Class<?>, HostComponentRegistration>();
        for (HostComponentRegistration registration : context.getHostComponentRegistrations()) {
            for (Class<?> cls : registration.getMainInterfaceClasses()) {
                hostComponentInterfaces.put(cls, registration);
            }
        }

        Class cls;
        try {
            cls = ClassLoaderUtils.loadClass(className, getClass());
        }
        catch (ClassNotFoundException e) {
            // not a host class, ignore
            return;
        }

        // Check constructor arguments for host component interfaces
        for (Constructor ctor : cls.getConstructors()) {
            for (Class<?> ctorParam : ctor.getParameterTypes()) {
                if (hostComponentInterfaces.containsKey(ctorParam)) {
                    context.addRequiredHostComponent(hostComponentInterfaces.get(ctorParam));
                }
            }
        }

        // Check setters for host component interface arguments
        for (Method method : cls.getMethods()) {
            if (method.getName().startsWith("set") && method.getParameterTypes().length == 1) {
                if (hostComponentInterfaces.containsKey(method.getParameterTypes()[0])) {
                    context.addRequiredHostComponent(hostComponentInterfaces.get(method.getParameterTypes()[0]));
                }
            }
        }
    }
}
