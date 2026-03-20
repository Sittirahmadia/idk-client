package dev.nova.client.mixin;

import dev.nova.client.NovaClient;
import dev.nova.client.event.events.PlayerMoveEvent;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {

    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void onSendMovement(CallbackInfo ci) {
        if (NovaClient.INSTANCE == null) return;
        PlayerMoveEvent event = NovaClient.INSTANCE.eventBus.post(new PlayerMoveEvent());
        if (event.isCancelled()) ci.cancel();
    }
}
