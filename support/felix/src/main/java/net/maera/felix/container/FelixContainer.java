package net.maera.felix.container;

import net.maera.felix.logging.Slf4jLogger;
import net.maera.io.Resource;
import net.maera.osgi.container.impl.DefaultContainer;
import net.maera.osgi.container.impl.DefaultHostActivator;
import net.maera.osgi.container.impl.HostActivator;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.FelixConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

/**
 * @since 0.1
 */
public class FelixContainer extends DefaultContainer {

    private static final transient Logger log = LoggerFactory.getLogger(FelixContainer.class);
    private static final String STARTUP_THREAD_NAME = "Felix:Startup";

    private File cacheDirectory;

    public FelixContainer() {
        super();
        cacheDirectory = new File("felix-cache");
        setFrameworkStartThreadFactory(new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable r) {
                final Thread thread = new Thread(r, STARTUP_THREAD_NAME);
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    @SuppressWarnings({"unchecked"})
    @Override
    protected Map<String, Object> prepareFrameworkConfig(Map<String, Object> configMap) {
        Map<String, Object> map = super.prepareFrameworkConfig(configMap);

        // Explicitly specify the directory to use for caching bundles.
        map.put(BundleCache.CACHE_ROOTDIR_PROP, getCacheDirectory().getAbsolutePath());

        Slf4jLogger felixLogger = new Slf4jLogger(log);
        map.put(FelixConstants.LOG_LOGGER_PROP, felixLogger);
        map.put(FelixConstants.LOG_LEVEL_PROP, String.valueOf(felixLogger.getLogLevel()));

        map.put(FelixConstants.IMPLICIT_BOOT_DELEGATION_PROP, Boolean.FALSE.toString());
        map.put(FelixConstants.FRAMEWORK_BUNDLE_PARENT, FelixConstants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK);

        HostActivator hostActivator = getHostActivator();
        if (hostActivator == null) {
            hostActivator = new DefaultHostActivator();
            setHostActivator(hostActivator);
        }

        List activators = Arrays.asList(hostActivator);
        configMap.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, activators);

        return map;
    }

    public File getCacheDirectory() {
        return cacheDirectory;
    }

    public void setCacheDirectory(File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }
}
