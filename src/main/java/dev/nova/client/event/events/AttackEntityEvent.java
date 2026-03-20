package dev.nova.client.event.events;
import dev.nova.client.event.CancellableEvent;
import net.minecraft.entity.Entity;
public final class AttackEntityEvent extends CancellableEvent {
    public final Entity target;
    public AttackEntityEvent(Entity target) { this.target = target; }
}
