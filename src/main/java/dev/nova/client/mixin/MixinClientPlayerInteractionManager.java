package dev.nova.client.mixin;

import dev.nova.client.NovaClient;
import dev.nova.client.module.modules.misc.NoBreakDelay;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientPlayerInteractionManager.class, priority = 999)
public class MixinClientPlayerInteractionManager {

    @Inject(method = "getBlockBreakingCooldown()I", at = @At("HEAD"), cancellable = true, require = 0)
    private void onGetCooldown(CallbackInfoReturnable<Integer> cir) {
        if (NovaClient.INSTANCE == null) return;
        NoBreakDelay mod = NovaClient.INSTANCE.moduleManager.get(NoBreakDelay.class);
        if (mod != null && mod.isEnabled()) cir.setReturnValue(0);
    }
}
