package dev.nova.client.event.events;
import dev.nova.client.event.CancellableEvent;
import net.minecraft.network.packet.Packet;
public final class ReceivePacketEvent extends CancellableEvent {
    public final Packet<?> packet;
    public ReceivePacketEvent(Packet<?> packet) { this.packet = packet; }
}
