package dev.nova.client.module.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.nova.client.event.EventHandler;
import dev.nova.client.event.events.RenderWorldEvent;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class Tracers extends Module {
    private final NumberSetting thickness = register(new NumberSetting("Width", "Line width", 1.5, 0.5, 5, 0.5));
    private final NumberSetting r = register(new NumberSetting("Red",   "0-255", 255, 0, 255, 1));
    private final NumberSetting g = register(new NumberSetting("Green", "0-255", 50,  0, 255, 1));
    private final NumberSetting b = register(new NumberSetting("Blue",  "0-255", 50,  0, 255, 1));

    public Tracers() { super("Tracers", "Draws lines to players", Category.RENDER, -1); }

    @EventHandler
    public void onRenderWorld(RenderWorldEvent e) {
        if (mc.world == null || mc.player == null || mc.gameRenderer == null) return;

        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(thickness.floatValue());
        RenderSystem.disableDepthTest();

        MatrixStack matrices = new MatrixStack();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f mat = matrices.peek().getPositionMatrix();

        float rv = r.floatValue() / 255f, gv = g.floatValue() / 255f, bv = b.floatValue() / 255f;

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            Vec3d pos = p.getLerpedPos(e.delta).add(0, p.getHeight() / 2.0, 0);
            buf.vertex(mat, 0, 0, 0).color(rv, gv, bv, 0.9f);
            buf.vertex(mat, (float)(pos.x - cam.x), (float)(pos.y - cam.y), (float)(pos.z - cam.z)).color(rv, gv, bv, 0.9f);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
