package dev.nova.client.event.events;

import dev.nova.client.event.Event;
import net.minecraft.client.render.RenderTickCounter;

public final class RenderWorldEvent extends Event {
    public final float delta;
    public RenderWorldEvent(float delta) { this.delta = delta; }
}
