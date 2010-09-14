package org.maera.plugin.event.impl;

import com.atlassian.event.spi.ListenerHandler;
import com.atlassian.event.spi.ListenerInvoker;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class is used internally by the {@link DefaultPluginEventManager} to adapt {@link ListenerMethodSelector}s to
 * the {@code ListenerHandler} interface used in Atlassian Event.
 *
 * @since 2.5.0
 */
final class MethodSelectorListenerHandler implements ListenerHandler {
    private final ListenerMethodSelector listenerMethodSelector;

    public MethodSelectorListenerHandler(ListenerMethodSelector listenerMethodSelector) {
        this.listenerMethodSelector = listenerMethodSelector;
    }

    public List<? extends ListenerInvoker> getInvokers(final Object listener) {
        final List<Method> validMethods = getValidMethods(checkNotNull(listener));

        return Lists.transform(validMethods, new Function<Method, ListenerInvoker>() {
            public ListenerInvoker apply(final Method method) {
                return new ListenerInvoker() {
                    public Set<Class<?>> getSupportedEventTypes() {
                        return Sets.newHashSet(method.getParameterTypes());
                    }

                    public void invoke(Object event) {
                        try {
                            method.invoke(listener, event);
                        }
                        catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                        catch (InvocationTargetException e) {
                            if (e.getCause() == null) {
                                throw new RuntimeException(e);
                            } else if (e.getCause().getMessage() == null) {
                                throw new RuntimeException(e.getCause());
                            } else {
                                throw new RuntimeException(e.getCause().getMessage(), e);
                            }
                        }
                    }

                    public boolean supportAsynchronousEvents() {
                        return true;
                    }
                };
            }
        });
    }

    private List<Method> getValidMethods(Object listener) {
        final List<Method> listenerMethods = Lists.newArrayList();
        for (Method method : listener.getClass().getMethods()) {
            if (isValidMethod(method)) {
                listenerMethods.add(method);
            }
        }
        return listenerMethods;
    }

    private boolean isValidMethod(Method method) {
        if (listenerMethodSelector.isListenerMethod(method)) {
            if (hasOneAndOnlyOneParameter(method)) {
                return true;
            } else {
                throw new RuntimeException("Method <" + method + "> of class <" + method.getDeclaringClass() + "> " +
                        "is being registered as a listener but has 0 or more than 1 parameters! " +
                        "Listener methods MUST have 1 and only 1 parameter.");
            }
        }
        return false;
    }

    private boolean hasOneAndOnlyOneParameter(Method method) {
        return method.getParameterTypes().length == 1;
    }
}
