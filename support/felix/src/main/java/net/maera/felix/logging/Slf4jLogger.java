package net.maera.felix.logging;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.resolver.ResourceNotFoundException;
import org.osgi.framework.BundleException;

import java.util.Arrays;
import java.util.List;

/**
 * Directs Felix's logging to SLF4J.
 *
 * @since 0.1
 */
public class Slf4jLogger extends Logger {

    private final org.slf4j.Logger log;

    private static final List<String> messagesToIgnore = Arrays.asList(
            "BeanInfo",
            "sun.beans.editors.",
            "add an import for 'org.springframework.osgi.service.",
            "Class 'org.springframework.util.Assert'",
            "Class '[Lorg.springframework.osgi.service",
            "org.springframework.core.InfrastructureProxy",
            "org.springframework.aop.SpringProxy",
            "org.springframework.aop.IntroductionInfo",
            "Class 'org.apache.commons.logging.impl.Log4JLogger'",
            "org.springframework.util.Assert",
            "org.springframework.osgi.service.importer.ServiceReferenceProxy",
            "org.springframework.osgi.service.importer.ImportedOsgiServiceProxy",
            "org.springframework.osgi.service.importer.support.ImportContextClassLoaderEditor",
            "[Lorg.springframework.osgi.service.importer.OsgiServiceLifecycleListener;Editor"
    );

    public Slf4jLogger(org.slf4j.Logger log) {
        this.log = log;
        setLogLevel(
                log.isDebugEnabled() ? Logger.LOG_DEBUG :
                        log.isInfoEnabled() ? Logger.LOG_INFO :
                                log.isWarnEnabled() ? Logger.LOG_WARNING :
                                        Logger.LOG_ERROR);
    }

    protected void doLog(org.osgi.framework.ServiceReference serviceReference, int level, java.lang.String message, java.lang.Throwable throwable) {
        if (serviceReference != null)
            message = "Service " + serviceReference + ": " + message;

        switch (level) {
            case LOG_DEBUG:
                if (throwable != null) {
                    log.debug(message, throwable);
                } else {
                    log.debug(message);
                }
                break;
            case LOG_INFO:
                if (throwable != null) {
                    log.info(message, throwable);
                } else {
                    log.info(message);
                }
                //logInfoUnlessLame(message);
                break;
            case LOG_WARNING:
                if (throwable != null) {
                    log.warn(message, throwable);
                } else {
                    log.warn(message);
                }
                // Handles special class loader errors from felix that have quite useful information
                /*
                if (throwable != null) {
                    if (throwable instanceof ClassNotFoundException && isClassNotFoundsWeCareAbout(throwable)) {
                        log.debug("Class not found in bundle: " + message);
                    } else if (throwable instanceof ResourceNotFoundException) {
                        log.trace("Resource not found in bundle: " + message);
                    }
                } else {
                    logInfoUnlessLame(message);
                }*/

                break;
            case LOG_ERROR:
                if (throwable != null) {
                    log.error(message, throwable);
                } else {
                    log.error(message);
                }
                /*if (throwable != null) {
                    if ((throwable instanceof BundleException) &&
                            (((BundleException) throwable).getNestedException() != null)) {
                        throwable = ((BundleException) throwable).getNestedException();
                    }
                    log.error(message, throwable);
                } else
                    log.error(message);*/
                break;
            default:
                String msg = "UNKNOWN LOG LEVEL [" + level + "]: " + message;
                if (throwable != null) {
                    log.warn(msg, throwable);
                } else {
                    log.warn(msg);
                }
        }
    }

    protected void logInfoUnlessLame(String message) {
        if (message != null) {
            // I'm really, really sick of these stupid messages
            for (String dumbBit : messagesToIgnore)
                if (message.contains(dumbBit))
                    return;
        }
        log.info(message);
    }

    public boolean isClassNotFoundsWeCareAbout(Throwable t) {
        if (t instanceof ClassNotFoundException) {
            String className = t.getMessage();
            if (className.contains("***") && t.getCause() instanceof ClassNotFoundException) {
                className = t.getCause().getMessage();
            }
            if (!className.startsWith("org.springframework") && !className.endsWith("BeanInfo") && !className.endsWith("Editor")) {
                return true;
            }
        }
        return false;
    }

}
