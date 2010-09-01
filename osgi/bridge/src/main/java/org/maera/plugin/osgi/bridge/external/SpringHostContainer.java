package org.maera.plugin.osgi.bridge.external;

import org.maera.plugin.hostcontainer.HostContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Host container implementation that uses the bundle's application context
 *
 * @since 0.1
 */
public class SpringHostContainer implements HostContainer, ApplicationContextAware {

    private ApplicationContext applicationContext;

    public <T> T create(Class<T> moduleClass) throws IllegalArgumentException {
        if (applicationContext == null) {
            throw new IllegalStateException("Application context missing");
        }
        return (T) applicationContext.getAutowireCapableBeanFactory().createBean(moduleClass, AutowireCapableBeanFactory.AUTOWIRE_AUTODETECT, false);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
