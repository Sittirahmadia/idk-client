package dev.nova.client.event.events;
import dev.nova.client.event.Event;
public final class KeyEvent extends Event {
    public final int key, action;
    public KeyEvent(int key, int action) { this.key = key; this.action = action; }
}
