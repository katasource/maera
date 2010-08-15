package org.maera.plugin.servlet.download.plugin;

import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.event.PluginEventListener;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.event.events.PluginModuleDisabledEvent;
import org.maera.plugin.event.events.PluginModuleEnabledEvent;
import org.maera.plugin.servlet.DownloadException;
import org.maera.plugin.servlet.DownloadStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A download strategy which maintains a list of {@link DownloadStrategyModuleDescriptor}s
 * and delegates to them in order.
 *
 * @see DownloadStrategyModuleDescriptor
 * @see DownloadStrategy
 * @since 2.2.0
 */
public class PluggableDownloadStrategy implements DownloadStrategy {
    private static final Logger log = LoggerFactory.getLogger(PluggableDownloadStrategy.class);
    private final Map<String, DownloadStrategy> strategies = new ConcurrentHashMap<String, DownloadStrategy>();

    public PluggableDownloadStrategy(final PluginEventManager pluginEventManager) {
        pluginEventManager.register(this);
    }

    public boolean matches(final String urlPath) {
        for (final DownloadStrategy strategy : strategies.values()) {
            if (strategy.matches(urlPath)) {
                if (log.isDebugEnabled()) {
                    log.debug("Matched plugin download strategy: " + strategy.getClass().getName());
                }
                return true;
            }
        }
        return false;
    }

    public void serveFile(final HttpServletRequest request, final HttpServletResponse response) throws DownloadException {
        for (final DownloadStrategy strategy : strategies.values()) {
            if (strategy.matches(request.getRequestURI().toLowerCase())) {
                strategy.serveFile(request, response);
                return;
            }
        }
        throw new DownloadException(
                "Found plugin download strategy during matching but not when trying to serve. Enable debug logging for more information.");
    }

    public void register(final String key, final DownloadStrategy strategy) {
        if (strategies.containsKey(key)) {
            log.warn("Replacing existing download strategy with module key: " + key);
        }
        strategies.put(key, strategy);
    }

    public void unregister(final String key) {
        strategies.remove(key);
    }

    @PluginEventListener
    public void pluginModuleEnabled(final PluginModuleEnabledEvent event) {
        final ModuleDescriptor<?> module = event.getModule();
        if (!(module instanceof DownloadStrategyModuleDescriptor)) {
            return;
        }

        register(module.getCompleteKey(), (DownloadStrategy) module.getModule());
    }

    @PluginEventListener
    public void pluginModuleDisabled(final PluginModuleDisabledEvent event) {
        final ModuleDescriptor<?> module = event.getModule();
        if (!(module instanceof DownloadStrategyModuleDescriptor)) {
            return;
        }

        unregister(module.getCompleteKey());
    }
}
