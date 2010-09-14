package org.maera.plugin.event.listeners;

import org.maera.plugin.event.events.PluginDisabledEvent;
import org.maera.plugin.event.events.PluginEnabledEvent;
import org.maera.plugin.event.events.PluginModuleDisabledEvent;
import org.maera.plugin.event.events.PluginModuleEnabledEvent;

import java.util.*;

public class RecordingListener {
    private final List<Object> events = new ArrayList<Object>();
    private final Set<Class> eventClasses = new HashSet<Class>();

    public RecordingListener(Class... eventClasses) {
        this.eventClasses.addAll(Arrays.asList(eventClasses));
    }

    public void channel(final Object event) {
        if (event != null && eventClasses.contains(event.getClass()))
            events.add(event);
    }

    /**
     * @return the events received by this listener in the order they were received
     */
    public List<Object> getEvents() {
        return events;
    }

    /**
     * @return the classes of each event received by this listener in the order they were received
     */
    public List<Class> getEventClasses() {
        List<Class> result = new ArrayList<Class>(events.size());
        for (Object event : events) {
            if (event != null) result.add(event.getClass());
        }
        return result;
    }

    public void reset() {
        events.clear();
    }

    public List<String> getEventPluginOrModuleKeys() {
        List<String> result = new ArrayList<String>(events.size());
        for (Object event : events) {
            if (event instanceof PluginEnabledEvent)
                result.add(((PluginEnabledEvent) event).getPlugin().getKey());
            else if (event instanceof PluginDisabledEvent)
                result.add(((PluginDisabledEvent) event).getPlugin().getKey());
            else if (event instanceof PluginModuleEnabledEvent)
                result.add(((PluginModuleEnabledEvent) event).getModule().getCompleteKey());
            else if (event instanceof PluginModuleDisabledEvent)
                result.add(((PluginModuleDisabledEvent) event).getModule().getCompleteKey());
            else
                result.add(null);
        }
        return result;
    }
}
