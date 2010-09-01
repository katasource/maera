package org.maera.plugin.osgi.hostcomponents.impl;

import org.maera.plugin.hostcontainer.HostContainer;
import org.maera.plugin.osgi.hostcomponents.ComponentRegistrar;
import org.maera.plugin.osgi.hostcomponents.ContextClassLoaderStrategy;
import org.maera.plugin.osgi.hostcomponents.HostComponentRegistration;
import org.maera.plugin.osgi.hostcomponents.InstanceBuilder;
import org.maera.plugin.osgi.hostcomponents.PropertyBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default component registrar that also can write registered host components into the OSGi service registry.
 *
 * @since 0.1
 */
public class DefaultComponentRegistrar implements ComponentRegistrar {
    
    private static final Logger log = LoggerFactory.getLogger(DefaultComponentRegistrar.class);

    private final List<HostComponentRegistration> registry = new CopyOnWriteArrayList<HostComponentRegistration>();

    public InstanceBuilder register(final Class<?>... mainInterfaces) {
        final Registration reg = new Registration(mainInterfaces);
        registry.add(reg);
        return new DefaultInstanceBuilder(reg);
    }

    public List<ServiceRegistration> writeRegistry(final BundleContext ctx) {
        final ArrayList<ServiceRegistration> services = new ArrayList<ServiceRegistration>();

        for (final HostComponentRegistration reg : new ArrayList<HostComponentRegistration>(registry)) {
            if (Arrays.asList(reg.getMainInterfaceClasses()).contains(HostContainer.class)) {
                log.warn("Cannot register a HostContainer as a host component, skipping");
                registry.remove(reg);
                continue;
            }

            final String[] names = reg.getMainInterfaces();

            reg.getProperties().put(HOST_COMPONENT_FLAG, Boolean.TRUE.toString());

            // If no bean name specified, generate one that will be consistent across restarts
            final String beanName = reg.getProperties().get(PropertyBuilder.BEAN_NAME);
            if (beanName == null) {
                String genKey = String.valueOf(Arrays.asList(reg.getMainInterfaces()).hashCode());
                reg.getProperties().put(PropertyBuilder.BEAN_NAME, "hostComponent-" + genKey);
            }

            if (log.isDebugEnabled()) {
                log.debug("Registering: " + Arrays.asList(names) + " instance " + reg.getInstance() + "with properties: " + reg.getProperties());
            }

            if (names.length == 0) {
                log.warn("Host component " + beanName + " of instance " + reg.getInstance() + " has no interfaces");
            }

            Object service = reg.getInstance();
            if (!ContextClassLoaderStrategy.USE_PLUGIN.name().equals(reg.getProperties().get(PropertyBuilder.CONTEXT_CLASS_LOADER_STRATEGY))) {
                service = wrapService(reg.getMainInterfaceClasses(), reg.getInstance());
            }

            final ServiceRegistration sreg = ctx.registerService(names, service, reg.getProperties());
            if (sreg != null) {
                services.add(sreg);
            }
        }
        return Collections.unmodifiableList(services);
    }

    public List<HostComponentRegistration> getRegistry() {
        return Collections.unmodifiableList(registry);
    }

    /**
     * Wraps the service in a dynamic proxy that ensures all methods are executed with the object class's class loader
     * as the context class loader
     *
     * @param interfaces The interfaces to proxy
     * @param service    The instance to proxy
     * @return A proxy that wraps the service
     */
    protected Object wrapService(final Class<?>[] interfaces, final Object service) {
        return Proxy.newProxyInstance(getClass().getClassLoader(), interfaces, new ContextClassLoaderSettingInvocationHandler(service));
    }

    /**
     * InvocationHandler for a dynamic proxy that ensures all methods are executed with the
     * object class's class loader as the context class loader.
     */
    private static class ContextClassLoaderSettingInvocationHandler implements InvocationHandler {
        private final Object service;

        ContextClassLoaderSettingInvocationHandler(final Object service) {
            this.service = service;
        }

        public Object invoke(final Object o, final Method method, final Object[] objects) throws Throwable {
            final Thread thread = Thread.currentThread();
            final ClassLoader ccl = thread.getContextClassLoader();
            try {
                thread.setContextClassLoader(service.getClass().getClassLoader());
                return method.invoke(service, objects);
            }
            catch (final InvocationTargetException e) {
                throw e.getTargetException();
            }
            finally {
                thread.setContextClassLoader(ccl);
            }
        }
    }
}
