package dev.nova.client.mixin;

import dev.nova.client.NovaClient;
import dev.nova.client.event.events.ReceivePacketEvent;
import dev.nova.client.event.events.SendPacketEvent;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    @Inject(
        method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;Z)V",
        at = @At("HEAD"), cancellable = true
    )
    private void onSend(Packet<?> packet, PacketCallbacks callbacks, boolean flush, CallbackInfo ci) {
        if (NovaClient.INSTANCE == null) return;
        SendPacketEvent event = NovaClient.INSTANCE.eventBus.post(new SendPacketEvent(packet));
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
    private void onReceive(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        if (NovaClient.INSTANCE == null) return;
        ReceivePacketEvent event = NovaClient.INSTANCE.eventBus.post(new ReceivePacketEvent(packet));
        if (event.isCancelled()) ci.cancel();
    }
}
