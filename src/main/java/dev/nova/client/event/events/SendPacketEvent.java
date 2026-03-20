package dev.nova.client.event.events;
import dev.nova.client.event.CancellableEvent;
import net.minecraft.network.packet.Packet;
public final class SendPacketEvent extends CancellableEvent {
    public final Packet<?> packet;
    public SendPacketEvent(Packet<?> packet) { this.packet = packet; }
}
