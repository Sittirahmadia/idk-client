package dev.nova.client.mixin;

import dev.nova.client.NovaClient;
import dev.nova.client.event.events.RenderHudEvent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class MixinInGameHud {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext ctx, RenderTickCounter counter, CallbackInfo ci) {
        if (NovaClient.INSTANCE == null) return;
        NovaClient.INSTANCE.eventBus.post(new RenderHudEvent(ctx, counter.getTickDelta(false)));
    }
}
