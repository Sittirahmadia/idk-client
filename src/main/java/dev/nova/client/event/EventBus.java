package dev.nova.client.event;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nova EventBus — annotation-driven, subscriber-per-class map.
 * Listeners annotated with @EventHandler are registered automatically.
 */
public final class EventBus {

    private final Map<Class<?>, List<ListenerEntry>> listeners = new ConcurrentHashMap<>();

    /** Register all @EventHandler methods in the given object. */
    public void subscribe(Object obj) {
        for (Method m : obj.getClass().getMethods()) {
            if (!m.isAnnotationPresent(EventHandler.class)) continue;
            if (m.getParameterCount() != 1) continue;
            Class<?> eventType = m.getParameterTypes()[0];
            listeners.computeIfAbsent(eventType, k -> new ArrayList<>())
                     .add(new ListenerEntry(obj, m));
        }
    }

    /** Remove all listeners registered from the given object. */
    public void unsubscribe(Object obj) {
        for (List<ListenerEntry> list : listeners.values())
            list.removeIf(e -> e.owner == obj);
    }

    /** Fire an event to all subscribers for its type. */
    public <T extends Event> T post(T event) {
        List<ListenerEntry> list = listeners.get(event.getClass());
        if (list == null) return event;
        for (ListenerEntry e : list) {
            try { e.method.invoke(e.owner, event); }
            catch (Exception ex) { ex.printStackTrace(); }
            if (event instanceof CancellableEvent ce && ce.isCancelled()) break;
        }
        return event;
    }

    private record ListenerEntry(Object owner, Method method) {}
}
