package dev.nova.client.mixin;

import dev.nova.client.NovaClient;
import dev.nova.client.event.events.TickEvent;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(CallbackInfo ci) {
        if (NovaClient.INSTANCE == null) return;
        NovaClient.INSTANCE.eventBus.post(new TickEvent(TickEvent.Phase.PRE));
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickTail(CallbackInfo ci) {
        if (NovaClient.INSTANCE == null) return;
        NovaClient.INSTANCE.eventBus.post(new TickEvent(TickEvent.Phase.POST));
    }
}
