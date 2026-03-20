package dev.nova.client.event.events;
import dev.nova.client.event.Event;
import net.minecraft.client.gui.DrawContext;
public final class RenderHudEvent extends Event {
    public final DrawContext context;
    public final float delta;
    public RenderHudEvent(DrawContext context, float delta) { this.context = context; this.delta = delta; }
}
