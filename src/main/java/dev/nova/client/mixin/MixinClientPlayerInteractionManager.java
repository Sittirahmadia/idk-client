package dev.nova.client.mixin;

import dev.nova.client.NovaClient;
import dev.nova.client.module.modules.misc.NoBreakDelay;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {

    @Inject(method = "getBlockBreakingCooldown", at = @At("HEAD"), cancellable = true)
    private void onGetCooldown(CallbackInfoReturnable<Integer> cir) {
        if (NovaClient.INSTANCE == null) return;
        NoBreakDelay mod = NovaClient.INSTANCE.moduleManager.get(NoBreakDelay.class);
        if (mod != null && mod.isEnabled()) cir.setReturnValue(0);
    }
}
