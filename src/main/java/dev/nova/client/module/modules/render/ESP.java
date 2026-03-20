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
    private final BoolSetting   players  = register(new BoolSetting ("Players",   "Show players",    true));
    private final BoolSetting   crystals = register(new BoolSetting ("Crystals",  "Show crystals",   false));
    private final NumberSetting red      = register(new NumberSetting("Red",   "Red 0-255",   255, 0,255,1));
    private final NumberSetting green    = register(new NumberSetting("Green", "Green 0-255", 50,  0,255,1));
    private final NumberSetting blue     = register(new NumberSetting("Blue",  "Blue 0-255",  50,  0,255,1));
    private final NumberSetting alpha    = register(new NumberSetting("Alpha", "Opacity 0-255",200, 0,255,1));
    private final NumberSetting lineW    = register(new NumberSetting("Line Width","Box line width",1.5,0.5,4,0.5));

    public ESP() { super("ESP", "Entity boxes through walls", Category.RENDER, -1); }

    @EventHandler
    public void onRenderWorld(RenderWorldEvent e) {
        if (mc.world == null || mc.player == null) return;
        int color = RenderUtil.argb(alpha.intValue(), red.intValue(), green.intValue(), blue.intValue());
        MatrixStack ms = new MatrixStack();
        if (players.getValue())
            for (PlayerEntity p : mc.world.getPlayers())
                if (p != mc.player) RenderUtil.drawEntityBox(ms, p, e.delta, color);
        if (crystals.getValue())
            mc.world.getEntitiesByClass(net.minecraft.entity.decoration.EndCrystalEntity.class,
                    mc.player.getBoundingBox().expand(24), x -> true)
                    .forEach(c -> RenderUtil.drawEntityBox(ms, c, e.delta,
                            RenderUtil.argb(alpha.intValue(), 80, 160, 255)));
    }
}
