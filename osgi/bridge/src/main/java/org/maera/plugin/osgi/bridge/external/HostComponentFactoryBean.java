package org.maera.plugin.osgi.bridge.external;

import org.apache.commons.lang.Validate;
import org.maera.plugin.PluginException;
import org.osgi.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.context.BundleContextAware;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.osgi.framework.ServiceEvent.REGISTERED;

/**
 * Simple factory bean to resolve host components.  Since we know host components won't change during the bundle's
 * lifetime, we can use a direct reference instead of the fancy proxy stuff from Spring DM.
 *
 * @since 2.2.0
 */
public class HostComponentFactoryBean implements FactoryBean, BundleContextAware, InitializingBean {
    private BundleContext bundleContext;
    private String filter;
    private Object service;
    private Class<?>[] interfaces;

    public Object getObject() throws Exception {
        return findService();
    }

    public Class getObjectType() {
        return (findService() != null ? findService().getClass() : null);
    }

    public boolean isSingleton() {
        return true;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * Sets the OSGi service filter.
     *
     * @param filter OSGi filter describing the importing OSGi service
     */
    public void setFilter(String filter) {
        this.filter = filter;
    }

    public void setInterfaces(Class<?>[] interfaces) {
        this.interfaces = interfaces;
    }

    /**
     * Finds a service, if the bundle context is available.
     *
     * @return The service, null if not found or the bundle context isn't available yet
     * @throws org.maera.plugin.PluginException
     *          If either 0 or more than 1 service reference is found
     */
    private Object findService() throws PluginException {
        return service;
    }

    /**
     * Wraps the service in a dynamic proxy that ensures the service reference is still valid
     *
     * @return A proxy that wraps the service
     */
    private Object createHostComponentProxy() {
        // we use the bundleContext's classloader since it was loaded from the main webapp
        return Proxy.newProxyInstance(bundleContext.getClass().getClassLoader(), interfaces, new DynamicServiceInvocationHandler(
                bundleContext, filter));
    }

    public void afterPropertiesSet() throws Exception {
        Validate.notNull(bundleContext);
        Validate.notNull(interfaces);
        service = createHostComponentProxy();
    }

    /**
     * InvocationHandler for a dynamic proxy that ensures all methods are executed with the
     * object class's class loader as the context class loader.
     */
    static class DynamicServiceInvocationHandler implements InvocationHandler {
        private static final Logger log = LoggerFactory.getLogger(DynamicServiceInvocationHandler.class);
        private volatile Object service;


        DynamicServiceInvocationHandler(final BundleContext bundleContext, final String filter) {
            try {
                ServiceReference[] refs = bundleContext.getServiceReferences(null, filter);
                if (refs != null && refs.length > 0) {
                    service = bundleContext.getService(refs[0]);
                }

                bundleContext.addServiceListener(new ServiceListener() {
                    public void serviceChanged(ServiceEvent serviceEvent) {
                        // We ignore unregistered services as we want to continue to use the old one during a transition
                        if (REGISTERED == serviceEvent.getType()) {
                            if (log.isDebugEnabled()) {
                                log.debug("Updating the host component matching filter: " + filter);
                            }
                            service = bundleContext.getService(serviceEvent.getServiceReference());
                        }
                    }
                }, filter);
            }
            catch (InvalidSyntaxException e) {
                throw new IllegalArgumentException("Invalid filter string: " + filter, e);
            }
        }

        public Object invoke(final Object o, final Method method, final Object[] objects) throws Throwable {
            try {
                return method.invoke(service, objects);
            }
            catch (final InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
    }
}
