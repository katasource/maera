package net.maera.osgi.container.impl;

import net.maera.MaeraException;
import net.maera.io.Resource;
import net.maera.lifecycle.Destroyable;
import net.maera.lifecycle.Initializable;
import net.maera.osgi.container.Container;
import net.maera.osgi.container.ContainerException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;

/**
 * @since 0.1
 * @author Les Hazlewood
 */
public class DefaultContainer implements Container, Initializable, Destroyable {

    private static final transient Logger log = LoggerFactory.getLogger(DefaultContainer.class);

    private static final String OSGI_FRAMEWORK_FACTORY_BOOTSTRAP_RESOURCE_PATH =
            "META-INF/services/org.osgi.framework.launch.FrameworkFactory";

    private FrameworkFactory frameworkFactory;
    private Framework framework;
    private boolean frameworkCreatedImplicitly;
    private Map<?, ?> frameworkConfig;
    private long stopWaitMillis;

    private volatile boolean running;

    public DefaultContainer() {
        this.running = false;
        this.stopWaitMillis = 5000; //5 seconds
        this.frameworkCreatedImplicitly = false;
    }

    public FrameworkFactory getFrameworkFactory() {
        return frameworkFactory;
    }

    public void setFrameworkFactory(FrameworkFactory frameworkFactory) {
        this.frameworkFactory = frameworkFactory;
    }

    public Framework getFramework() {
        return framework;
    }

    public void setFramework(Framework framework) {
        this.framework = framework;
    }

    public Map<?, ?> getFrameworkConfig() {
        return frameworkConfig;
    }

    public void setFrameworkConfig(Map<?, ?> frameworkConfig) {
        this.frameworkConfig = frameworkConfig;
    }

    public long getStopWaitMillis() {
        return stopWaitMillis;
    }

    public void setStopWaitMillis(long stopWaitMillis) {
        this.stopWaitMillis = stopWaitMillis;
    }

    private static FrameworkFactory createFrameworkFactory(String className) throws MaeraException {
        try {
            Class clazz = Class.forName(className);
            return (FrameworkFactory) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            throw new MaeraException("Unable to find FrameworkFactory class [" +
                    className + "]. Ensure an OSGi 4.2 (or greater) compatible framework " +
                    "is on the classpath.  These frameworks define this class in a file '" +
                    OSGI_FRAMEWORK_FACTORY_BOOTSTRAP_RESOURCE_PATH + "'.", e);
        } catch (IllegalAccessException e) {
            String msg = "Fatal error: Unable to access frameworkFactoryClass [" +
                    className + "].  The plugin system cannot initialize.";
            throw new MaeraException(msg, e);
        } catch (InstantiationException e) {
            String msg = "Fatal error: Unable to instantiate frameworkFactoryClass [" + className + "].  Ensure " +
                    "an OSGi 4.2 (or greater) compatible framework is on the classpath.";
            throw new MaeraException(msg, e);
        }
    }

    @Override
    public void init() {
        if (this.framework == null) {
            if (this.frameworkFactory == null) {
                this.frameworkFactory = getDefaultFrameworkFactory();
            }
            if (this.frameworkFactory == null) {
                throw new IllegalStateException("FrameworkFactory instance cannot be null.");
            }
            this.framework = this.frameworkFactory.newFramework(this.frameworkConfig);
            this.frameworkCreatedImplicitly = true;
        }

        try {
            this.framework.init();
            this.framework.start();
        } catch (BundleException e) {
            String msg = "Unable to start OSGi framework.";
            throw new MaeraException(msg, e);
        }
    }

    @Override
    public void start() throws ContainerException {
        if (running) return;
        try {
            this.framework.start();
            this.running = true;
        } catch( BundleException e) {
            throw new ContainerException("Unable to start the OSGi framework.", e);
        }
    }

    @Override
    public void stop() throws InterruptedException, ContainerException {
        if (!running) return;
        try {
            this.framework.stop();
            long waitMillis = this.stopWaitMillis;
            if (waitMillis > 0) {
                this.framework.waitForStop(waitMillis);
            }
        } catch (BundleException e) {
            String msg = "Unable to cleanly stop the OSGi Container.";
            throw new ContainerException(msg, e);
        } finally {
            this.running = false;
        }
    }

    @Override
    public Bundle installBundle(Resource resource) throws ContainerException {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public void destroy() {
        try {
            stop();
        } catch (InterruptedException e) {
            //_always_ preserve the interrupt status if you don't re-throw the InterruptedException
            //(Java Concurrency in Practice, Section 7.1.2):
            Thread.currentThread().interrupt();
            throw new ContainerException("Encountered InterruptedException while waiting for the OSGi framework " +
                    "to stop.", e);
        } finally {
            if (this.frameworkCreatedImplicitly) {
                this.framework = null;
            }
        }
    }

    private static FrameworkFactory getDefaultFrameworkFactory() {
        URL url = DefaultContainer.class.getClassLoader().getResource(OSGI_FRAMEWORK_FACTORY_BOOTSTRAP_RESOURCE_PATH);

        if (url == null) {
            //TODO - use a better exception class:
            throw new MaeraException("Unable to acquire OSGi FrameworkFactory class from the classpath under " +
                    "the path '" + OSGI_FRAMEWORK_FACTORY_BOOTSTRAP_RESOURCE_PATH + "'.  This is required of all " +
                    "OSGi 4.2 compliant frameworks.  Ensure you are using an up-to-date OSGi 4.2 (or later) " +
                    "framework implementation.");
        }

        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(url.openStream()));
            for (String s = br.readLine(); s != null; s = br.readLine()) {
                s = s.trim();
                // Try to load first non-empty, non-commented line.
                if ((s.length() > 0) && (s.charAt(0) != '#')) {
                    return createFrameworkFactory(s);
                }
            }
        } catch (IOException e) {
            String msg = "Fatal error: Unable to access resource '" +
                    OSGI_FRAMEWORK_FACTORY_BOOTSTRAP_RESOURCE_PATH + "'.  This " +
                    "is required to discover the OSGi FrameworkFactory class to use at runtime.";
            throw new MaeraException(msg);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    log.warn("Unable to close OSGi FrameworkFactory class name config file '" +
                            OSGI_FRAMEWORK_FACTORY_BOOTSTRAP_RESOURCE_PATH + "'.", e);
                }
            }
        }

        //if we've made it to this point, the file wasn't read correctly and the framework wasn't instantiated
        throw new IllegalStateException("No FrameworkFactory instantiated!");
    }




}
