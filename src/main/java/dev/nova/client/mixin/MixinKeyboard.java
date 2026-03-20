package dev.nova.client.mixin;

import dev.nova.client.NovaClient;
import dev.nova.client.event.events.KeyEvent;
import dev.nova.client.gui.NovaClickGUI;
import dev.nova.client.module.Module;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class MixinKeyboard {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (NovaClient.INSTANCE == null || client.player == null) return;
        if (action != GLFW.GLFW_PRESS) return;

        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            if (client.currentScreen instanceof NovaClickGUI) client.setScreen(null);
            else client.setScreen(new NovaClickGUI());
            return;
        }

        NovaClient.INSTANCE.eventBus.post(new KeyEvent(key, action));
        for (Module mod : NovaClient.INSTANCE.moduleManager.getModules())
            if (mod.getKey() == key) mod.toggle();
    }
}
