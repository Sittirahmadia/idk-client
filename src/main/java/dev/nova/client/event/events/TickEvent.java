package dev.nova.client.event.events;
import dev.nova.client.event.Event;
public final class TickEvent extends Event {
    public enum Phase { PRE, POST }
    public final Phase phase;
    public TickEvent(Phase phase) { this.phase = phase; }
}
