package dev.nova.client.mixin;

import dev.nova.client.NovaClient;
import dev.nova.client.event.events.RenderWorldEvent;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void onRenderWorld(RenderTickCounter counter, CallbackInfo ci) {
        if (NovaClient.INSTANCE == null) return;
        NovaClient.INSTANCE.eventBus.post(new RenderWorldEvent(counter.getTickDelta(false)));
    }
}
