package org.maera.plugin.event.impl;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.event.config.EventThreadPoolConfiguration;
import com.atlassian.event.config.ListenerHandlersConfiguration;
import com.atlassian.event.internal.AsynchronousAbleEventDispatcher;
import com.atlassian.event.internal.EventExecutorFactoryImpl;
import com.atlassian.event.internal.EventPublisherImpl;
import com.atlassian.event.internal.EventThreadPoolConfigurationImpl;
import com.atlassian.event.spi.EventDispatcher;
import com.atlassian.event.spi.EventExecutorFactory;
import com.atlassian.event.spi.ListenerHandler;
import org.maera.plugin.event.NotificationException;
import org.maera.plugin.event.PluginEventManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple, synchronous event manager that uses one or more method selectors to determine event listeners.  The default
 * method selectors are {@link MethodNameListenerMethodSelector} and {@link AnnotationListenerMethodSelector}.
 */
public class DefaultPluginEventManager implements PluginEventManager {
    private final EventPublisher publisher;

    /**
     * Constructor that looks for an arbitrary selectors
     *
     * @param selectors List of selectors that determine which are listener methods
     */
    public DefaultPluginEventManager(final ListenerMethodSelector[] selectors) {
        ListenerHandlersConfiguration configuration = new ListenerHandlersConfiguration() {
            public List<ListenerHandler> getListenerHandlers() {
                List<ListenerHandler> handlers = new ArrayList<ListenerHandler>(selectors.length);
                for (ListenerMethodSelector selector : selectors) {
                    handlers.add(new MethodSelectorListenerHandler(selector));
                }
                return handlers;
            }
        };

        EventThreadPoolConfiguration threadPoolConfiguration = new EventThreadPoolConfigurationImpl();
        EventExecutorFactory factory = new EventExecutorFactoryImpl(threadPoolConfiguration);
        EventDispatcher dispatcher = new AsynchronousAbleEventDispatcher(factory);
        publisher = new EventPublisherImpl(dispatcher, configuration);
    }

    public DefaultPluginEventManager() {
        this(new ListenerMethodSelector[]{new MethodNameListenerMethodSelector(), new AnnotationListenerMethodSelector(), new AnnotationListenerMethodSelector(EventListener.class)});
    }

    /**
     * Default constructor that delegates all event publication to an {@code EventPublisher}
     */
    public DefaultPluginEventManager(EventPublisher publisher) {
        this.publisher = publisher;
    }

    public void register(Object listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        publisher.register(listener);
    }

    public void unregister(Object listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        publisher.unregister(listener);
    }

    public void broadcast(Object event) throws NotificationException {
        try {
            publisher.publish(event);
        }
        catch (RuntimeException e) {
            throw new NotificationException(e);
        }
    }
}
