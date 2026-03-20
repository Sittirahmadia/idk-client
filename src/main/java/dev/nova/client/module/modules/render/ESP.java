package dev.nova.client.module.modules.render;

import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.RenderWorldEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import dev.nova.client.util.RenderUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;

public final class ESP extends Module {
    private final BoolSetting players = register(new BoolSetting("Players", "Show player boxes", true));
    private final NumberSetting r = register(new NumberSetting("Red",   "Red 0-255",   255, 0, 255, 1));
    private final NumberSetting g = register(new NumberSetting("Green", "Green 0-255", 50,  0, 255, 1));
    private final NumberSetting b = register(new NumberSetting("Blue",  "Blue 0-255",  50,  0, 255, 1));
    private final NumberSetting a = register(new NumberSetting("Alpha", "Alpha 0-255", 200, 0, 255, 1));

    public ESP() { super("ESP", "Draws entity boxes through walls", Category.RENDER, -1); }

    @EventHandler
    public void onRenderWorld(RenderWorldEvent e) {
        if (mc.world == null || mc.player == null) return;
        int color = RenderUtil.argb(a.intValue(), r.intValue(), g.intValue(), b.intValue());
        MatrixStack matrices = new MatrixStack();
        if (players.getValue())
            for (PlayerEntity p : mc.world.getPlayers())
                if (p != mc.player) RenderUtil.drawEntityBox(matrices, p, e.delta, color);
    }
}
