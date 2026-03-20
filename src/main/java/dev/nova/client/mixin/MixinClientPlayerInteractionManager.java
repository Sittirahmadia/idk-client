package dev.nova.client.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Placeholder — getBlockBreakingCooldown does not exist in 1.21.1.
 * NoBreakDelay is handled differently if needed in future.
 */
@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {
    // intentionally empty
}
