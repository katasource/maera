package net.maera.osgi.container.impl;

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
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Container implementation that provides thread-safe lifecycle support (init, start, stop, destroy).  An internal
 * {@link Lock} instance is used to ensure that multiple threads do not collide during lifecycle operations.  That is,
 * only one thread may interact with an instance at any time during
 * {@link #init}, {@link #start}, {@link #stop}, {@link #destroy} invocations.  Instances of this class will be
 * thread-safe during those calls.
 * <p/>
 * Subclasses can implement any lifecycle logic as required by overriding any of the respective template
 * {@link #onInit}, {@link #onStart}, {@link #onStop}, {@link #onDestroy} methods as necessary.
 * <p/>
 * Finally, thread-safety is only guaranteed during the aforementioned lifecycle method invocations.  Subclasses
 * must ensure thread-safety on their own for any methods not invoked during the lifecycle operations.
 *
 * @author Les Hazlewood
 * @since 0.1
 */
public class LifecycleContainer implements Container, Initializable, Destroyable {

    private static final transient Logger log = LoggerFactory.getLogger(LifecycleContainer.class);

    private static final String OSGI_FRAMEWORK_FACTORY_BOOTSTRAP_RESOURCE_PATH =
            "META-INF/services/org.osgi.framework.launch.FrameworkFactory";

    private static final String STARTUP_THREAD_NAME = "OSGi:Startup";
    private static final long STARTUP_JOIN_WAIT_MILLIS = 10 * 60 * 1000;

    private FrameworkFactory frameworkFactory;
    private Framework framework;
    private boolean frameworkCreatedImplicitly;
    private Map<?, ?> frameworkConfig;
    private long stopWaitMillis;

    private volatile boolean initialized;
    private volatile boolean running;
    private final Lock lifecycleLock;

    private final ThreadFactory frameworkStartThreadFactory;

    public LifecycleContainer() {
        this(STARTUP_THREAD_NAME);
    }

    public LifecycleContainer(final String startupThreadName) {
        this.initialized = false;
        this.running = false;
        this.stopWaitMillis = 5000; //5 seconds
        this.frameworkCreatedImplicitly = false;

        this.lifecycleLock = new ReentrantLock();

        this.frameworkStartThreadFactory = new ThreadFactory() {
            public Thread newThread(final Runnable r) {
                final Thread thread = new Thread(r, startupThreadName);
                thread.setDaemon(true);
                return thread;
            }
        };
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

    protected boolean isInitialized() {
        return this.initialized;
    }

    protected boolean isRunning() {
        return this.running;
    }

    private static FrameworkFactory createFrameworkFactory(String className) throws ContainerException {
        try {
            Class clazz = Class.forName(className);
            return (FrameworkFactory) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            throw new ContainerException("Unable to find FrameworkFactory class [" +
                    className + "]. Ensure an OSGi 4.2 (or greater) compatible framework " +
                    "is on the classpath.  These frameworks define this class in a file '" +
                    OSGI_FRAMEWORK_FACTORY_BOOTSTRAP_RESOURCE_PATH + "'.", e);
        } catch (IllegalAccessException e) {
            String msg = "Fatal error: Unable to access frameworkFactoryClass [" +
                    className + "].  The plugin system cannot initialize.";
            throw new ContainerException(msg, e);
        } catch (InstantiationException e) {
            String msg = "Fatal error: Unable to instantiate frameworkFactoryClass [" + className + "].  Ensure " +
                    "an OSGi 4.2 (or greater) compatible framework is on the classpath.";
            throw new ContainerException(msg, e);
        }
    }

    @Override
    public void init() {
        this.lifecycleLock.lock();
        try {
            lockedInit();
        } finally {
            this.lifecycleLock.unlock();
        }
    }

    protected void lockedInit() throws ContainerException {
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
            onInit();
            this.initialized = true;
        } catch (ContainerException ce) {
            //propagate:
            throw ce;
        } catch (Throwable t) {
            throw new ContainerException("Unable to successfully initialize.", t);
        }
    }

    /**
     * Template method for subclass custom initialization logic, if necessary.
     *
     * @throws Exception if there is any error during initialization
     */
    protected void onInit() throws Exception {
    }

    @Override
    public void start() throws ContainerException {
        this.lifecycleLock.lock();
        try {
            lockedStart();
        } finally {
            this.lifecycleLock.unlock();
        }
    }

    protected void lockedStart() throws ContainerException {
        if (running) return;
        if (!initialized) {
            init();
        }
        try {
            start(this.framework);
            onStart();
            this.running = true;
        } catch (InterruptedException e) {
            //_always_ preserve the interrupt status if you don't re-throw the InterruptedException
            //(Java Concurrency in Practice, Section 7.1.2):
            Thread.currentThread().interrupt();
            throw new ContainerException("Encountered InterruptedException while starting the OSGi framework.  " +
                    "Thread interrupt status has been preserved.", e);
        } catch (ContainerException ce) {
            //propagate without wrapping:
            throw ce;
        } catch (Throwable t) {
            //wrap and propagate:
            throw new ContainerException("Unable to start.", t);
        }
    }

    private void start(final Framework framework) throws InterruptedException {
        // Start the framework in a different thread that explicitly sets the daemon status.
        final Runnable start = new Runnable() {
            public void run() {
                try {
                    Thread.currentThread().setContextClassLoader(null);
                    framework.start();
                } catch (final BundleException e) {
                    String msg = "Unable to start the OSGi framework instance of type " +
                            framework.getClass();
                    throw new ContainerException(msg, e);
                }
            }
        };
        final Thread t = frameworkStartThreadFactory.newThread(start);
        t.start();

        t.join(STARTUP_JOIN_WAIT_MILLIS); //should this be configurable?
    }

    protected void onStart() throws Exception {
    }

    @Override
    public void stop() throws InterruptedException, ContainerException {
        stop(this.stopWaitMillis);
    }

    public void stop(long waitMillis) throws InterruptedException, ContainerException {
        this.lifecycleLock.lock();
        try {
            lockedStop(waitMillis);
        } finally {
            this.lifecycleLock.unlock();
        }
    }

    protected void lockedStop(long waitMillis) throws InterruptedException, ContainerException {
        if (!running || this.framework == null) return;
        try {
            onStop();
            this.framework.stop();
            if (waitMillis > 0) {
                this.framework.waitForStop(waitMillis);
            }
            this.running = false;
        } catch (InterruptedException ie) {
            //propagate without wrapping:
            throw ie;
        } catch (ContainerException ce) {
            //propagate without wrapping:
            throw ce;
        } catch (Throwable t) {
            //wrap and propagate:
            throw new ContainerException("Unable to cleanly stop the OSGi framework.");
        }
    }

    protected void onStop() throws Exception {
    }

    @Override
    public Bundle installBundle(Resource resource) throws ContainerException {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public void destroy() {
        this.lifecycleLock.lock();
        try {
            lockedDestroy();
        } finally {
            this.lifecycleLock.unlock();
        }
    }

    protected void lockedDestroy() throws ContainerException {
        if (this.framework == null) { //allow for idempotent destroy calls
            return;
        }
        try {
            stop();
        } catch (InterruptedException e) {
            //_always_ preserve the interrupt status if you don't re-throw the InterruptedException
            //(Java Concurrency in Practice, Section 7.1.2):
            Thread.currentThread().interrupt();
            throw new ContainerException("Encountered InterruptedException while waiting for the OSGi framework " +
                    "to stop.  Thread interrupt status has been preserved.", e);
        } catch (ContainerException ce) {
            //propagate without wrapping:
            throw ce;
        } catch (Throwable t) {
            //wrap and propagate:
            throw new ContainerException("Unable to cleanly stop the OSGi framework.");
        } finally {
            if (this.frameworkCreatedImplicitly) {
                this.framework = null;
            }
        }
    }

    protected void onDestroy() throws Exception {
    }

    private static FrameworkFactory getDefaultFrameworkFactory() {
        URL url = LifecycleContainer.class.getClassLoader().getResource(OSGI_FRAMEWORK_FACTORY_BOOTSTRAP_RESOURCE_PATH);

        if (url == null) {
            //TODO - use a better exception class:
            throw new ContainerException("Unable to acquire OSGi FrameworkFactory class from the classpath under " +
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
            throw new ContainerException(msg);
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
